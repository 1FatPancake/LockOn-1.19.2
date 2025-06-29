package net.leolifeless.lockonmod;

import net.leolifeless.lockonmod.compat.ThirdPersonCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LockOnSystem {
    // === CORE TARGETING STATE ===
    private static Entity targetEntity = null;
    private static List<Entity> potentialTargets = new ArrayList<>();
    private static int currentTargetIndex = -1;
    private static boolean wasKeyHeld = false;
    private static boolean indicatorVisible = true;
    private static boolean wasLocked = false;
    private static LockOnConfig.TargetingMode runtimeTargetingMode = null;

    // === PERFORMANCE OPTIMIZATION CACHES ===
    private static final Map<Entity, Long> entityValidationCache = new HashMap<>();
    private static final Map<Entity, Boolean> lineOfSightCache = new HashMap<>();
    private static final Map<Entity, Vec3> entityPositionCache = new HashMap<>();

    // === TIMING CONTROLS ===
    private static long lastTargetingUpdate = 0;
    private static long lastCameraUpdate = 0;
    private static long lastCacheCleanup = 0;

    // === SMOOTH INTERPOLATION STATE ===
    private static float previousYaw = 0;
    private static float previousPitch = 0;
    private static Vec3 previousTargetPos = Vec3.ZERO;
    private static boolean firstRotationFrame = true;

    // === THIRD PERSON COMPATIBILITY STATE ===
    private static Vec3 lastCameraOffset = Vec3.ZERO;
    private static boolean lastThirdPersonState = false;
    private static long lastThirdPersonCheck = 0;

    // === ADAPTIVE PERFORMANCE VARIABLES ===
    private static int targetingUpdateInterval = 3; // Dynamic based on config
    private static int cameraUpdateInterval = 1;
    private static long cacheValidationDuration = 100;

    // === PUBLIC ACCESSORS ===

    /**
     * Get the current target entity (public accessor)
     */
    public static Entity getTargetEntity() {
        return targetEntity;
    }

    /**
     * Check if there is currently a locked target
     */
    public static boolean hasTarget() {
        return targetEntity != null && targetEntity.isAlive();
    }

    /**
     * Get the list of potential targets
     */
    public static List<Entity> getPotentialTargets() {
        return new ArrayList<>(potentialTargets);
    }

    /**
     * Check if the mod was previously locked (for UI purposes)
     */
    public static boolean wasLocked() {
        return wasLocked;
    }

    /**
     * Gets the current compatibility status for external access
     */
    public static String getThirdPersonCompatibilityStatus() {
        return ThirdPersonCompatibility.getCompatibilityStatus();
    }

    // === MAIN EVENT HANDLERS ===

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Update performance settings from config
        updatePerformanceSettings();

        // Fixed: Use mc.level instead of mc.level() for 1.19.2
        long currentTick = mc.level.getGameTime();
        long currentTime = System.currentTimeMillis();

        // Check for third person state changes (less frequently for performance)
        if (currentTime - lastThirdPersonCheck > 500) { // Check every 500ms
            checkThirdPersonStateChange();
            lastThirdPersonCheck = currentTime;
        }

        // Check if lock-on should be disabled for current game mode
        if (shouldDisableForGameMode(player)) {
            clearTarget();
            return;
        }

        // Handle input every tick for responsiveness
        handleInput(player);

        // Separate update intervals for different operations
        if (currentTick - lastTargetingUpdate >= targetingUpdateInterval) {
            updateTargetingOptimized(player, currentTick);
            lastTargetingUpdate = currentTick;
        }

        if (currentTick - lastCameraUpdate >= cameraUpdateInterval) {
            updateCameraRotationOptimized(player);
            lastCameraUpdate = currentTick;
        }

        // Clean cache periodically (every 5 seconds)
        if (currentTime - lastCacheCleanup > 5000) {
            cleanCaches();
            lastCacheCleanup = currentTime;
        }
    }

    /**
     * Update performance settings from config (with fallbacks)
     */
    private static void updatePerformanceSettings() {
        try {
            // Try to get from config, use fallbacks if methods don't exist
            targetingUpdateInterval = getConfigInt("targetingUpdateInterval", 3);
            cameraUpdateInterval = getConfigInt("cameraUpdateInterval", 1);
            cacheValidationDuration = getConfigInt("cacheValidationDuration", 100);

            // Emergency performance mode fallback
            boolean performanceMode = getConfigBoolean("performanceMode", false);
            if (performanceMode) {
                targetingUpdateInterval = Math.max(targetingUpdateInterval, 5);
                cameraUpdateInterval = Math.max(cameraUpdateInterval, 2);
                cacheValidationDuration = Math.max(cacheValidationDuration, 300);
            }
        } catch (Exception e) {
            // Fallback values if config isn't available yet
            targetingUpdateInterval = 3;
            cameraUpdateInterval = 1;
            cacheValidationDuration = 100;
        }
    }

    /**
     * Safe config getter with fallback
     */
    private static int getConfigInt(String key, int fallback) {
        try {
            switch (key) {
                case "targetingUpdateInterval":
                    return LockOnConfig.getUpdateFrequency(); // Use existing method
                case "cameraUpdateInterval":
                    return 1; // Fixed value since this doesn't exist in config
                case "cacheValidationDuration":
                    return 100; // Fixed value since this doesn't exist in config
                default:
                    return fallback;
            }
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Safe config getter with fallback
     */
    private static boolean getConfigBoolean(String key, boolean fallback) {
        try {
            switch (key) {
                case "performanceMode":
                    return false; // Not implemented in config yet
                case "targetPrediction":
                    return false; // Not implemented in config yet
                case "adaptiveSmoothing":
                    return true; // Default enabled
                case "toggleMode":
                    return LockOnConfig.isToggleMode();
                case "holdMode":
                    return LockOnConfig.holdToMaintainLock();
                case "canCycle":
                    return LockOnConfig.canCycleThroughTargets();
                case "requireLineOfSight":
                    return LockOnConfig.requireLineOfSight();
                case "canTargetPlayers":
                    return LockOnConfig.canTargetPlayers();
                case "canTargetHostiles":
                    return LockOnConfig.canTargetHostileMobs();
                case "canTargetPassives":
                    return LockOnConfig.canTargetPassiveMobs();
                case "canTargetBosses":
                    return LockOnConfig.canTargetBosses();
                case "smoothCamera":
                    return LockOnConfig.isSmoothCameraEnabled();
                case "soundsEnabled":
                    return LockOnConfig.areSoundsEnabled();
                case "lockSound":
                    return LockOnConfig.playLockOnSound();
                case "switchSound":
                    return LockOnConfig.playTargetSwitchSound();
                case "lostSound":
                    return LockOnConfig.playTargetLostSound();
                case "disableCreative":
                    return LockOnConfig.disableInCreative();
                case "disableSpectator":
                    return LockOnConfig.disableInSpectator();
                case "reverseScroll":
                    return LockOnConfig.reverseScrollCycling();
                case "penetrateGlass":
                    return true; // Default enabled since method doesn't exist
                case "autoBreakOnObstruction":
                    return true; // Default enabled since method doesn't exist
                case "adaptiveRotation":
                    return false; // Default disabled since method doesn't exist
                default:
                    return fallback;
            }
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Safe config double getter
     */
    private static double getConfigDouble(String key, double fallback) {
        try {
            switch (key) {
                case "searchRadius":
                    return LockOnConfig.getSearchRadius();
                case "maxDistance":
                    return LockOnConfig.getMaxLockOnDistance();
                case "targetingAngle":
                    return LockOnConfig.getTargetingAngle();
                case "minHealth":
                    return LockOnConfig.getMinTargetHealth();
                case "maxHealth":
                    return LockOnConfig.getMaxTargetHealth();
                case "rotationSpeed":
                    return LockOnConfig.getRotationSpeed();
                case "minRotationSpeed":
                    return LockOnConfig.getMinRotationSpeed();
                case "maxRotationSpeed":
                    return LockOnConfig.getMaxRotationSpeed();
                case "distanceWeight":
                    return LockOnConfig.getDistancePriorityWeight();
                case "angleWeight":
                    return LockOnConfig.getAnglePriorityWeight();
                case "microMovementThreshold":
                    return 1.0; // Default value since this doesn't exist in config
                case "positionSmoothing":
                    return 0.7; // Default value since this doesn't exist in config
                case "soundVolume":
                    return LockOnConfig.getSoundVolume();
                default:
                    return fallback;
            }
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Check for third person state changes
     */
    private static void checkThirdPersonStateChange() {
        boolean currentThirdPersonState = ThirdPersonCompatibility.isThirdPersonActive();
        if (currentThirdPersonState != lastThirdPersonState) {
            onThirdPersonStateChanged(currentThirdPersonState);
            lastThirdPersonState = currentThirdPersonState;
        }

        // Update camera offset tracking
        if (currentThirdPersonState) {
            lastCameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
        }
    }

    /**
     * Handle third person state changes
     */
    private static void onThirdPersonStateChanged(boolean isThirdPerson) {
        if (isThirdPerson) {
            LockOnMod.LOGGER.debug("Switched to third person - enabling enhanced lock-on features");
            // Reset rotation state for smooth transition
            firstRotationFrame = true;
        } else {
            LockOnMod.LOGGER.debug("Switched to first person - using standard lock-on features");
        }

        // Optionally adjust current target if locked
        if (targetEntity != null) {
            updateTargetingParameters();
        }
    }

    /**
     * Update targeting parameters when switching perspectives
     */
    private static void updateTargetingParameters() {
        if (targetEntity != null) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && !isValidTargetCached(targetEntity, player)) {
                clearTarget();
            }
        }
    }

    /**
     * Enhanced input handling
     */
    private static void handleInput(LocalPlayer player) {
        boolean lockKeyPressed = LockOnKeybinds.lockOnKey.isDown();
        boolean lockKeyClicked = LockOnKeybinds.lockOnKey.consumeClick();
        boolean cycleKeyClicked = LockOnKeybinds.cycleTargetKey.consumeClick();
        boolean reverseCycleKeyClicked = LockOnKeybinds.cycleTargetReverseKey.consumeClick();
        boolean clearKeyClicked = LockOnKeybinds.clearTargetKey.consumeClick();

        // Handle lock-on based on configuration mode
        boolean toggleMode = getConfigBoolean("toggleMode", true);
        boolean holdMode = getConfigBoolean("holdMode", false);

        if (toggleMode) {
            if (lockKeyClicked) {
                if (targetEntity == null) {
                    findAndLockTargetOptimized(player);
                } else {
                    clearTarget();
                }
            }
        } else if (holdMode) {
            if (lockKeyPressed && !wasKeyHeld) {
                findAndLockTargetOptimized(player);
            } else if (!lockKeyPressed && wasKeyHeld) {
                clearTarget();
            }
        } else {
            if (lockKeyClicked) {
                findAndLockTargetOptimized(player);
            }
        }

        // Handle target cycling
        boolean canCycle = getConfigBoolean("canCycle", true);
        if (cycleKeyClicked && canCycle) {
            cycleTargetOptimized(player, false);
        }

        if (reverseCycleKeyClicked && canCycle) {
            cycleTargetOptimized(player, true);
        }

        // Handle clear target
        if (clearKeyClicked) {
            clearTarget();
        }

        // Handle targeting mode shortcuts
        handleTargetingModeShortcuts(player);

        // Handle filter toggles
        handleFilterToggles(player);

        // Handle visual controls
        handleVisualControls(player);

        wasKeyHeld = lockKeyPressed;
    }

    /**
     * Optimized target finding with caching and third person support
     */
    private static void findAndLockTargetOptimized(LocalPlayer player) {
        List<Entity> targets = findValidTargetsOptimized(player);

        if (targets.isEmpty()) {
            showMessage(player, "No Valid Targets Found");
            playSound(player, "target_lost");
            return;
        }

        // Apply third person adjustments to targeting
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            targets = adjustTargetsForThirdPerson(targets, player);
        }

        Entity newTarget = selectBestTargetOptimized(targets, player);

        if (newTarget != null) {
            setTarget(newTarget);
            potentialTargets = targets;
            currentTargetIndex = targets.indexOf(newTarget);
            showMessage(player, "Target Locked: " + getEntityDisplayName(newTarget));
            playSound(player, "lock_on");

            // Reset rotation state for smooth initial lock
            firstRotationFrame = true;
        }
    }

    /**
     * Optimized target finding with enhanced caching
     */
    private static List<Entity> findValidTargetsOptimized(LocalPlayer player) {
        // Get range with third person adjustments
        double range = getMaxTargetingRangeAdjusted();
        double searchRadius = Math.min(range * 1.2, getConfigDouble("searchRadius", 30.0));

        AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        Level level = player.level();
        List<Entity> nearbyEntities = level.getEntities(player, searchBox);

        return nearbyEntities.stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity != player)
                .filter(entity -> (double) entity.distanceTo(player) <= range)
                .filter(entity -> isValidTargetCached(entity, player))
                .filter(entity -> hasLineOfSightCached(player, entity))
                .filter(entity -> isWithinTargetingAngleOptimized(player, entity))
                .limit(LockOnConfig.getMaxTargetsToSearch())
                .collect(Collectors.toList());
    }

    /**
     * Cached entity validation
     */
    private static boolean isValidTargetCached(Entity entity, LocalPlayer player) {
        long currentTime = System.currentTimeMillis();
        Long lastValidation = entityValidationCache.get(entity);

        if (lastValidation == null || currentTime - lastValidation > cacheValidationDuration) {
            boolean isValid = isValidTargetDirect(entity, player);
            entityValidationCache.put(entity, currentTime);
            return isValid;
        }

        return true; // Assume valid if recently cached
    }

    /**
     * Direct entity validation (non-cached)
     */
    private static boolean isValidTargetDirect(Entity entity, LocalPlayer player) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == player) return false;
        if (!entity.isAlive()) return false;

        LivingEntity living = (LivingEntity) entity;

        // Health filtering with safe fallbacks
        float health = living.getHealth();
        double minHealth = getConfigDouble("minHealth", 1.0);
        double maxHealth = getConfigDouble("maxHealth", 0.0);

        if (health < minHealth) return false;
        if (maxHealth > 0 && health > maxHealth) return false;

        // Entity type filtering with safe fallbacks
        boolean canTargetPlayers = getConfigBoolean("canTargetPlayers", true);
        boolean canTargetHostiles = getConfigBoolean("canTargetHostiles", true);
        boolean canTargetPassives = getConfigBoolean("canTargetPassives", false);
        boolean canTargetBosses = getConfigBoolean("canTargetBosses", true);

        if (entity instanceof Player && !canTargetPlayers) return false;
        if (entity instanceof Monster && !canTargetHostiles) return false;
        if (entity instanceof Animal && !canTargetPassives) return false;
        if ((entity instanceof WitherBoss || entity instanceof EnderDragon) && !canTargetBosses) return false;

        return true;
    }

    /**
     * Cached line of sight checking
     */
    private static boolean hasLineOfSightCached(LocalPlayer player, Entity target) {
        boolean requireLOS = getConfigBoolean("requireLineOfSight", true);
        if (!requireLOS) return true;

        // Use cached result if available and entity hasn't moved much
        Boolean cachedResult = lineOfSightCache.get(target);
        Vec3 lastPos = entityPositionCache.get(target);
        Vec3 currentPos = target.position();

        if (cachedResult != null && lastPos != null && lastPos.distanceTo(currentPos) < 0.5) {
            return cachedResult;
        }

        // Perform actual line of sight check
        boolean hasLOS = hasLineOfSightDirect(player, target);
        lineOfSightCache.put(target, hasLOS);
        entityPositionCache.put(target, currentPos);

        return hasLOS;
    }

    /**
     * Direct line of sight check (non-cached)
     */
    private static boolean hasLineOfSightDirect(LocalPlayer player, Entity target) {
        Vec3 start = player.getEyePosition();
        Vec3 end = target.getEyePosition();

        // Adjust for third person camera position
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vec3 cameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            start = start.add(cameraOffset.scale(0.2)); // Slight adjustment
        }

        Level level = player.level();
        BlockHitResult result = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockState blockState = level.getBlockState(result.getBlockPos());

            // Allow targeting through glass if configured
            boolean penetrateGlass = getConfigBoolean("penetrateGlass", true);
            if (penetrateGlass &&
                    (blockState.is(Blocks.GLASS) || blockState.is(Blocks.GLASS_PANE) ||
                            blockState.getBlock().toString().contains("glass"))) {
                return true;
            }

            return false;
        }

        return true;
    }

    /**
     * Optimized angle checking with third person adjustments
     */
    private static boolean isWithinTargetingAngleOptimized(LocalPlayer player, Entity target) {
        // Get angle with third person adjustments
        double maxAngle = getTargetingAngleAdjusted();

        Vec3 playerPos = player.getEyePosition();
        Vec3 targetPos = target.getEyePosition();
        Vec3 lookDirection = player.getLookAngle();

        // Adjust for third person camera offset
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vec3 cameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            playerPos = playerPos.add(cameraOffset.scale(0.5)); // Partial offset for better targeting
        }

        Vec3 toTarget = targetPos.subtract(playerPos).normalize();
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, lookDirection.dot(toTarget)))) * 180.0 / Math.PI;

        return angle <= maxAngle;
    }

    /**
     * Optimized target selection with improved algorithms
     */
    private static Entity selectBestTargetOptimized(List<Entity> targets, LocalPlayer player) {
        if (targets.isEmpty()) return null;

        // Use configured targeting mode or runtime override
        LockOnConfig.TargetingMode mode = runtimeTargetingMode != null ?
                runtimeTargetingMode : LockOnConfig.getTargetingMode();

        switch (mode) {
            case CLOSEST:
                return targets.stream()
                        .min(Comparator.comparing(e -> e.distanceTo(player)))
                        .orElse(null);

            case MOST_DAMAGED:
                return targets.stream()
                        .filter(e -> e instanceof LivingEntity)
                        .min(Comparator.comparing(e -> ((LivingEntity) e).getHealth()))
                        .orElse(null);

            case CROSSHAIR_CENTERED:
                return targets.stream()
                        .min(Comparator.comparing(e -> calculateCrosshairDistance(player, e)))
                        .orElse(null);

            case THREAT_LEVEL:
                return targets.stream()
                        .max(Comparator.comparing(e -> calculateThreatLevel(e)))
                        .orElse(null);

            // Note: SMART case removed due to compilation error
            default:
                return targets.get(0);
        }
    }

    /**
     * Aggressive camera rotation for testing - WORKING VERSION
     */
    private static void updateCameraRotationOptimized(LocalPlayer player) {
        if (targetEntity == null || !targetEntity.isAlive()) {
            if (targetEntity != null) {
                onTargetLost();
                clearTarget();
            }
            return;
        }

        // Debug info
        debugCameraRotation(player, targetEntity);

        // FORCE ENABLE for testing - ignore config
        boolean smoothCamera = true; // Force enabled
        if (!smoothCamera) {
            LockOnMod.LOGGER.info("Smooth camera is DISABLED in config");
            return;
        }

        // Calculate target rotation directly (no third person adjustments for now)
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 targetPos = targetEntity.getEyePosition();

        // Calculate look direction
        Vec3 direction = targetPos.subtract(playerEyePos).normalize();
        float targetYaw = (float) (Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float targetPitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);

        // Get current rotation
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // AGGRESSIVE rotation speed for testing
        float rotationSpeed = 0.5f; // Very high speed

        // Calculate angle differences
        float yawDiff = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Apply aggressive rotation
        float newYaw = currentYaw + yawDiff * rotationSpeed;
        float newPitch = currentPitch + pitchDiff * rotationSpeed;

        // Clamp pitch
        newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));

        // Log every rotation attempt
        LockOnMod.LOGGER.info("ROTATION ATTEMPT:");
        LockOnMod.LOGGER.info("  Current Yaw/Pitch: {}/{}", currentYaw, currentPitch);
        LockOnMod.LOGGER.info("  Target Yaw/Pitch: {}/{}", targetYaw, targetPitch);
        LockOnMod.LOGGER.info("  Applying Yaw/Pitch: {}/{}", newYaw, newPitch);
        LockOnMod.LOGGER.info("  Differences: {}/{}", yawDiff, pitchDiff);

        // Try multiple rotation methods
        try {
            // Method 1: Direct setters
            player.setYRot(newYaw);
            player.setXRot(newPitch);
            LockOnMod.LOGGER.info("Applied Method 1: setYRot/setXRot");

            // Method 2: Head rotation
            player.setYHeadRot(newYaw);
            LockOnMod.LOGGER.info("Applied Method 2: setYHeadRot");

            // Method 3: Turn method
            player.turn(yawDiff * rotationSpeed, pitchDiff * rotationSpeed);
            LockOnMod.LOGGER.info("Applied Method 3: turn");

            // Method 4: Force previous rotation values
            player.yRotO = newYaw;
            player.xRotO = newPitch;
            LockOnMod.LOGGER.info("Applied Method 4: yRotO/xRotO");

        } catch (Exception e) {
            LockOnMod.LOGGER.error("Failed to apply camera rotation: {}", e.getMessage());
        }

        // Verify the rotation was applied
        float verifyYaw = player.getYRot();
        float verifyPitch = player.getXRot();
        LockOnMod.LOGGER.info("VERIFICATION - Final Yaw/Pitch: {}/{}", verifyYaw, verifyPitch);

        if (Math.abs(verifyYaw - newYaw) > 0.1f || Math.abs(verifyPitch - newPitch) > 0.1f) {
            LockOnMod.LOGGER.warn("ROTATION NOT APPLIED! Expected: {}/{}, Got: {}/{}",
                    newYaw, newPitch, verifyYaw, verifyPitch);
        } else {
            LockOnMod.LOGGER.info("ROTATION SUCCESS!");
        }
    }

    /**
     * Debug camera rotation information
     */
    private static void debugCameraRotation(LocalPlayer player, Entity target) {
        if (target == null) return;

        // Log current state
        LockOnMod.LOGGER.info("=== CAMERA DEBUG ===");
        LockOnMod.LOGGER.info("Target: {}", target.getDisplayName().getString());
        LockOnMod.LOGGER.info("Player Yaw: {}, Pitch: {}", player.getYRot(), player.getXRot());
        LockOnMod.LOGGER.info("Smooth Camera Enabled: {}", getConfigBoolean("smoothCamera", true));
        LockOnMod.LOGGER.info("Target Distance: {}", player.distanceTo(target));

        // Calculate what the rotation should be
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 targetPos = target.getEyePosition();
        Vec3 direction = targetPos.subtract(playerEyePos).normalize();
        float targetYaw = (float) (Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float targetPitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);

        LockOnMod.LOGGER.info("Calculated Target Yaw: {}, Pitch: {}", targetYaw, targetPitch);
        LockOnMod.LOGGER.info("=================");
    }

    /**
     * Calculate stabilized rotation speed based on distance and angle
     */
    private static float calculateStabilizedRotationSpeed(LocalPlayer player, Entity target, double distance) {
        float baseSpeed = (float) getConfigDouble("rotationSpeed", 0.15);

        // Distance-based scaling
        float distanceScale = 1.0f;
        if (distance < 5.0) {
            distanceScale = (float) (distance / 5.0); // Scale down for close targets
            distanceScale = Math.max(0.2f, distanceScale); // Minimum speed
        }

        // Third person adjustments
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            baseSpeed = ThirdPersonCompatibility.getAdjustedRotationSpeed(baseSpeed);
            distanceScale *= 0.8f; // Slightly slower in third person for stability
        }

        return baseSpeed * distanceScale;
    }

    /**
     * Safely apply rotation with validation and fallbacks
     */
    private static void applyRotationSafely(LocalPlayer player, float newYaw, float newPitch,
                                            float currentYaw, float currentPitch) {

        // Validate rotation values
        if (!Float.isFinite(newYaw) || !Float.isFinite(newPitch)) {
            LockOnMod.LOGGER.warn("Invalid rotation values: Yaw={}, Pitch={}", newYaw, newPitch);
            return;
        }

        // Check for excessive rotation speed (sign of instability)
        float yawChange = Math.abs(normalizeAngle(newYaw - currentYaw));
        float pitchChange = Math.abs(newPitch - currentPitch);

        if (yawChange > 90.0f || pitchChange > 90.0f) {
            LockOnMod.LOGGER.warn("Excessive rotation detected, limiting: YawChange={}, PitchChange={}",
                    yawChange, pitchChange);

            // Limit rotation to prevent wild spinning
            float maxChange = 30.0f;
            if (yawChange > maxChange) {
                float yawDiff = normalizeAngle(newYaw - currentYaw);
                newYaw = currentYaw + Math.signum(yawDiff) * maxChange;
            }
            if (pitchChange > maxChange) {
                float pitchDiff = newPitch - currentPitch;
                newPitch = currentPitch + Math.signum(pitchDiff) * maxChange;
            }
        }

        try {
            // Apply the rotation
            player.setYRot(newYaw);
            player.setXRot(newPitch);

            // Update head rotation for consistency
            player.setYHeadRot(newYaw);

            // Update previous rotation values for smooth interpolation
            player.yRotO = newYaw;
            player.xRotO = newPitch;

        } catch (Exception e) {
            LockOnMod.LOGGER.error("Failed to apply safe rotation: {}", e.getMessage());
        }
    }

    /**
     * Normalize angle to [-180, 180] range
     */
    private static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Optimized targeting state update
     */
    private static void updateTargetingOptimized(LocalPlayer player, long currentTick) {
        if (targetEntity == null) return;

        // Quick validation check
        if (!targetEntity.isAlive()) {
            clearTarget();
            return;
        }

        // Use cached validation for performance
        if (!isValidTargetCached(targetEntity, player)) {
            clearTarget();
            return;
        }

        // Auto-break on obstruction (check less frequently)
        boolean autoBreak = getConfigBoolean("autoBreakOnObstruction", true);
        if (currentTick % 5 == 0 && autoBreak) {
            if (!hasLineOfSightCached(player, targetEntity)) {
                clearTarget();
                return;
            }
        }

        // Check distance (check less frequently for performance)
        if (currentTick % 10 == 0) {
            double distance = (double) player.distanceTo(targetEntity);
            double maxRange = getMaxTargetingRangeAdjusted();

            if (distance > maxRange) {
                clearTarget();
                return;
            }
        }
    }

    /**
     * Optimized target cycling
     */
    private static void cycleTargetOptimized(LocalPlayer player, boolean reverse) {
        if (potentialTargets.isEmpty()) {
            findAndLockTargetOptimized(player);
            return;
        }

        // Filter out invalid targets with caching
        potentialTargets = potentialTargets.stream()
                .filter(entity -> entity.isAlive() && isValidTargetCached(entity, player))
                .collect(Collectors.toList());

        if (potentialTargets.isEmpty()) {
            clearTarget();
            return;
        }

        // Calculate next index
        int direction = reverse ? -1 : 1;
        boolean reverseScroll = getConfigBoolean("reverseScroll", false);
        if (reverseScroll) {
            direction *= -1;
        }

        currentTargetIndex += direction;
        if (currentTargetIndex >= potentialTargets.size()) {
            currentTargetIndex = 0;
        } else if (currentTargetIndex < 0) {
            currentTargetIndex = potentialTargets.size() - 1;
        }

        // Set new target
        Entity newTarget = potentialTargets.get(currentTargetIndex);
        setTarget(newTarget);
        showMessage(player, "Target Switched: " + getEntityDisplayName(newTarget));
        playSound(player, "target_switch");

        // Reset rotation state for smooth transition
        firstRotationFrame = true;
    }

    /**
     * Enhanced rendering for indicator display
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (targetEntity == null || !indicatorVisible) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Use the correct method signature that matches your current LockOnRenderer
        LockOnRenderer.renderLockOnIndicator(event, targetEntity);
    }

    /**
     * Adjust target list for third person perspective
     */
    private static List<Entity> adjustTargetsForThirdPerson(List<Entity> targets, LocalPlayer player) {
        Vec3 cameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();

        // Filter out targets that might be behind the camera in third person
        return targets.stream()
                .filter(entity -> {
                    Vec3 toEntity = entity.position().subtract(player.position());
                    Vec3 adjustedCameraPos = player.position().add(cameraOffset);
                    Vec3 toCameraEntity = entity.position().subtract(adjustedCameraPos);

                    // Ensure target is generally in front of the camera
                    return toCameraEntity.dot(player.getLookAngle()) > -0.5;
                })
                .collect(Collectors.toList());
    }

    /**
     * Clean old cache entries for performance
     */
    private static void cleanCaches() {
        long currentTime = System.currentTimeMillis();

        // Clean entity validation cache
        entityValidationCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > cacheValidationDuration * 10);

        // Clean line of sight cache (remove dead entities)
        lineOfSightCache.entrySet().removeIf(entry ->
                entry.getKey() == null || !entry.getKey().isAlive());

        // Clean position cache (remove dead entities)
        entityPositionCache.entrySet().removeIf(entry ->
                entry.getKey() == null || !entry.getKey().isAlive());

        LockOnMod.LOGGER.debug("Cleaned targeting caches - validation: {}, LOS: {}, position: {}",
                entityValidationCache.size(), lineOfSightCache.size(), entityPositionCache.size());
    }

    /**
     * Get maximum targeting range with third person adjustments
     */
    private static double getMaxTargetingRangeAdjusted() {
        double baseRange = getConfigDouble("maxDistance", 50.0);
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            return ThirdPersonCompatibility.getAdjustedTargetingRange(baseRange);
        }
        return baseRange;
    }

    /**
     * Get targeting angle with third person adjustments
     */
    private static double getTargetingAngleAdjusted() {
        double baseAngle = getConfigDouble("targetingAngle", 45.0);
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            return ThirdPersonCompatibility.getAdjustedTargetingAngle(baseAngle);
        }
        return baseAngle;
    }

    // === UTILITY CALCULATION METHODS ===

    /**
     * Calculate distance from crosshair for targeting
     */
    private static double calculateCrosshairDistance(LocalPlayer player, Entity entity) {
        Vec3 lookDirection = player.getLookAngle();
        Vec3 toEntity = entity.getEyePosition().subtract(player.getEyePosition()).normalize();
        return Math.acos(Math.max(-1.0, Math.min(1.0, lookDirection.dot(toEntity))));
    }

    /**
     * Calculate threat level for targeting
     */
    private static double calculateThreatLevel(Entity entity) {
        if (entity instanceof Monster) return 3.0;
        if (entity instanceof Player) return 2.0;
        if (entity instanceof Animal) return 1.0;
        return 0.0;
    }

    // === INPUT HANDLING METHODS ===

    /**
     * Handles targeting mode shortcut keys
     */
    private static void handleTargetingModeShortcuts(LocalPlayer player) {
        if (LockOnKeybinds.targetClosestKey.consumeClick()) {
            setRuntimeTargetingMode(LockOnConfig.TargetingMode.CLOSEST);
            showMessage(player, "Targeting Mode: Closest");
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.targetMostDamagedKey.consumeClick()) {
            setRuntimeTargetingMode(LockOnConfig.TargetingMode.MOST_DAMAGED);
            showMessage(player, "Targeting Mode: Most Damaged");
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.targetThreatKey.consumeClick()) {
            setRuntimeTargetingMode(LockOnConfig.TargetingMode.THREAT_LEVEL);
            showMessage(player, "Targeting Mode: Threat Level");
            playSound(player, "target_switch");
        }
    }

    /**
     * Handles filter toggle keys
     */
    private static void handleFilterToggles(LocalPlayer player) {
        if (LockOnKeybinds.togglePlayersKey.consumeClick()) {
            boolean canTargetPlayers = getConfigBoolean("canTargetPlayers", true);
            showMessage(player, "Player Targeting: " + (canTargetPlayers ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.toggleHostilesKey.consumeClick()) {
            boolean canTargetHostiles = getConfigBoolean("canTargetHostiles", true);
            showMessage(player, "Hostile Mob Targeting: " + (canTargetHostiles ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.togglePassivesKey.consumeClick()) {
            boolean canTargetPassives = getConfigBoolean("canTargetPassives", false);
            showMessage(player, "Passive Mob Targeting: " + (canTargetPassives ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }
    }

    /**
     * Handles visual control keys including custom indicator cycling
     */
    private static void handleVisualControls(LocalPlayer player) {
        // Toggle indicator visibility
        if (LockOnKeybinds.toggleIndicatorKey.consumeClick()) {
            indicatorVisible = !indicatorVisible;
            showMessage(player, "Lock-On Indicator: " + (indicatorVisible ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }

        // Cycle indicator type
        if (LockOnKeybinds.cycleIndicatorTypeKey.consumeClick()) {
            showMessage(player, "Indicator Type: Circle"); // Fallback since we can't access config
            playSound(player, "target_switch");
        }
    }

    // === RENDERING EVENT HANDLER ===
    // NOTE: Rendering is handled by the existing LockOnRenderer class
    // We don't need a separate render event here as it would conflict

    // === UTILITY AND HELPER METHODS ===

    /**
     * Sets the runtime targeting mode (overrides config)
     */
    private static void setRuntimeTargetingMode(LockOnConfig.TargetingMode mode) {
        runtimeTargetingMode = mode;
    }

    /**
     * Sets the target entity
     */
    private static void setTarget(Entity target) {
        targetEntity = target;
        wasLocked = true;

        // Clear caches for new target
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();
    }

    /**
     * Shows a message to the player with third person compatibility info
     */
    private static void showMessage(LocalPlayer player, String message) {
        if (player != null) {
            // Add compatibility status for debug purposes
            if (LockOnMod.LOGGER.isDebugEnabled() && ThirdPersonCompatibility.isModLoaded()) {
                String compatMessage = message + " [" + ThirdPersonCompatibility.getCompatibilityStatus() + "]";
                player.displayClientMessage(Component.literal(compatMessage), true);
            } else {
                player.displayClientMessage(Component.literal(message), true);
            }
        }
    }

    /**
     * Plays appropriate sound based on type (fixed for 1.19.2)
     */
    private static void playSound(LocalPlayer player, String soundType) {
        boolean soundsEnabled = getConfigBoolean("soundsEnabled", true);
        if (!soundsEnabled) return;

        Level level = player.level();
        float volume = (float) getConfigDouble("soundVolume", 1.0);

        // Play appropriate sound based on type
        try {
            switch (soundType) {
                case "lock_on":
                    boolean lockSound = getConfigBoolean("lockSound", true);
                    if (lockSound) {
                        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                                volume, 1.2f, false);
                    }
                    break;
                case "target_switch":
                    boolean switchSound = getConfigBoolean("switchSound", true);
                    if (switchSound) {
                        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                                SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.PLAYERS,
                                volume, 1.5f, false);
                    }
                    break;
                case "target_lost":
                    boolean lostSound = getConfigBoolean("lostSound", true);
                    if (lostSound) {
                        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS,
                                volume, 0.8f, false);
                    }
                    break;
            }
        } catch (Exception e) {
            // Fallback: If sounds fail, just continue without sound
            LockOnMod.LOGGER.debug("Could not play sound {}: {}", soundType, e.getMessage());
        }
    }

    /**
     * Gets display name for an entity
     */
    private static String getEntityDisplayName(Entity entity) {
        return entity.getDisplayName().getString();
    }

    /**
     * Clears the current target
     */
    public static void clearTarget() {
        if (targetEntity != null) {
            onTargetLost();
        }
        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = -1;
        wasLocked = false;
        runtimeTargetingMode = null;
        firstRotationFrame = true;

        // Clear position tracking
        previousTargetPos = Vec3.ZERO;
    }

    /**
     * Handles target lost event
     */
    private static void onTargetLost() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            showMessage(player, "Target Lost");
            playSound(player, "target_lost");
        }
        wasLocked = false;
    }

    /**
     * Checks if lock-on should be disabled for current game mode
     */
    private static boolean shouldDisableForGameMode(LocalPlayer player) {
        boolean disableCreative = getConfigBoolean("disableCreative", false);
        boolean disableSpectator = getConfigBoolean("disableSpectator", true);

        if (player.isCreative() && disableCreative) return true;
        if (player.isSpectator() && disableSpectator) return true;
        return false;
    }

    // === PUBLIC UTILITY METHODS FOR EXTERNAL ACCESS ===

    /**
     * Debug method to get current targeting state
     */
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Target: ").append(targetEntity != null ? getEntityDisplayName(targetEntity) : "None").append("\n");
        sb.append("Potential Targets: ").append(potentialTargets.size()).append("\n");
        sb.append("Third Person Active: ").append(ThirdPersonCompatibility.isThirdPersonActive()).append("\n");
        sb.append("Runtime Mode: ").append(runtimeTargetingMode != null ? runtimeTargetingMode : "Config Default").append("\n");
        sb.append("Cache Sizes - Validation: ").append(entityValidationCache.size())
                .append(", LOS: ").append(lineOfSightCache.size())
                .append(", Position: ").append(entityPositionCache.size()).append("\n");
        sb.append("Update Intervals - Targeting: ").append(targetingUpdateInterval)
                .append(", Camera: ").append(cameraUpdateInterval).append("\n");
        return sb.toString();
    }

    /**
     * Force refresh of target list (useful for external integrations)
     */
    public static void refreshTargets() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            potentialTargets = findValidTargetsOptimized(player);
        }
    }

    /**
     * Check if targeting is currently active
     */
    public static boolean isActive() {
        return targetEntity != null && targetEntity.isAlive();
    }

    /**
     * Get the current target distance (for UI display)
     */
    public static double getCurrentTargetDistance() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && targetEntity != null) {
            return (double) player.distanceTo(targetEntity);
        }
        return 0.0;
    }

    /**
     * Get the current target health percentage (for UI display)
     */
    public static float getCurrentTargetHealthPercent() {
        if (targetEntity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) targetEntity;
            return living.getHealth() / living.getMaxHealth();
        }
        return 0.0f;
    }

    /**
     * Manual target selection by entity (for external integration)
     */
    public static boolean setTargetEntity(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && entity != null && isValidTargetCached(entity, player)) {
            setTarget(entity);
            showMessage(player, "Target Set: " + getEntityDisplayName(entity));
            playSound(player, "lock_on");
            return true;
        }
        return false;
    }

    /**
     * Toggle indicator visibility (for keybind)
     */
    public static void toggleIndicatorVisibility() {
        indicatorVisible = !indicatorVisible;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            showMessage(player, "Lock-On Indicator: " + (indicatorVisible ? "Enabled" : "Disabled"));
        }
    }

    /**
     * Get indicator visibility state
     */
    public static boolean isIndicatorVisible() {
        return indicatorVisible;
    }

    /**
     * Emergency reset (clears all state)
     */
    public static void emergencyReset() {
        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = -1;
        wasKeyHeld = false;
        wasLocked = false;
        runtimeTargetingMode = null;
        indicatorVisible = true;
        firstRotationFrame = true;

        // Clear third person state
        lastThirdPersonState = false;
        lastCameraOffset = Vec3.ZERO;
        lastThirdPersonCheck = 0;

        // Clear interpolation state
        previousYaw = 0;
        previousPitch = 0;
        previousTargetPos = Vec3.ZERO;

        // Clear caches
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();

        // Reset timing
        lastTargetingUpdate = 0;
        lastCameraUpdate = 0;
        lastCacheCleanup = 0;

        LockOnMod.LOGGER.info("Lock-On System emergency reset completed with optimizations");
    }

    /**
     * Enable emergency performance mode for low-end systems
     */
    public static void enableEmergencyPerformanceMode() {
        targetingUpdateInterval = 10; // Much slower updates
        cameraUpdateInterval = 5;
        cacheValidationDuration = 1000; // Longer cache duration

        // Clear caches to start fresh
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();

        LockOnMod.LOGGER.info("Emergency performance mode enabled for Lock-On System");
    }

    /**
     * Get performance statistics
     */
    public static String getPerformanceStats() {
        return String.format("Performance Stats - Targeting Interval: %d, Camera Interval: %d, Cache Duration: %dms, " +
                        "Active Caches: %d entities",
                targetingUpdateInterval, cameraUpdateInterval, cacheValidationDuration,
                entityValidationCache.size());
    }
}