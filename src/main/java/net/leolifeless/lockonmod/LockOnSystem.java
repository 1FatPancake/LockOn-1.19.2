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

    // === SYNC PROTECTION STATE (NEW) ===
    private static int lagTimeout = 0;
    private static final int MAX_LAG_TIMEOUT = 100; // 5 seconds at 20 TPS
    private static int syncCheckCounter = 0;
    private static long lastServerResponseTime = 0;
    private static boolean wasNetworkLagging = false;

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

        long currentTick = mc.level.getGameTime();
        long currentTime = System.currentTimeMillis();

        // NEW: Automatic sync checking (configurable)
        syncCheckCounter++;
        if (syncCheckCounter >= 60) { // Check every 3 seconds
            syncCheckCounter = 0;
            performAutomaticSyncCheck(player);
        }

        // Check for third person state changes (less frequently for performance)
        if (currentTime - lastThirdPersonCheck > 500) {
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
            updateTargetingOptimizedWithSync(player, currentTick); // NEW: Enhanced method
            lastTargetingUpdate = currentTick;
        }

        // Only update camera if not experiencing lag (NEW)
        boolean skipCameraOnLag = true; // You can make this configurable
        boolean isLagging = isNetworkLagging(player);

        if (currentTick - lastCameraUpdate >= cameraUpdateInterval) {
            if (!skipCameraOnLag || !isLagging) {
                updateCameraRotationOptimized(player);
            }
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
     * Enhanced target validation with server sync checking
     */
    private static boolean isTargetStillValid(Entity target, LocalPlayer player) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Check if entity still exists in the world (server sync)
        if (target.isRemoved()) {
            return false;
        }

        // Check if we can still see the entity (network sync)
        // In single player, skip network checks
        if (!Minecraft.getInstance().hasSingleplayerServer()) {
            if (target.tickCount == 0 && target.getId() != -1) {
                // Entity exists but hasn't been updated recently - possible desync
                return false;
            }
        }

        return isValidTargetCached(target, player);
    }

    /**
     * Detect network lag that might cause desync
     */
    private static boolean isNetworkLagging(LocalPlayer player) {
        // Check if we're in single player (no network lag possible)
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return false;
        }

        // Simple lag detection based on entity update frequency
        if (targetEntity != null) {
            long currentTime = System.currentTimeMillis();

            // If entity hasn't been updated in a while, might be lag
            if (currentTime - targetEntity.tickCount > 1000) { // 1 second
                return true;
            }
        }

        return false;
    }

    /**
     * Handle targeting during network lag
     */
    private static void handleLaggyTargeting(LocalPlayer player) {
        lagTimeout++;

        if (lagTimeout > MAX_LAG_TIMEOUT) {
            // Been lagging too long, clear the target
            clearTargetWithReason("Network timeout - clearing stale target");
            return;
        }

        // Show a warning message to the player (but not too frequently)
        if (lagTimeout == 20) { // After 1 second
            showMessage(player, "§eNetwork lag detected - lock-on may be unstable");
        }
    }

    /**
     * Clear target with a reason for debugging
     */
    private static void clearTargetWithReason(String reason) {
        if (targetEntity != null) {
            LockOnMod.LOGGER.debug("Clearing target: {}", reason);
            onTargetLost();
        }
        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = -1;
        wasLocked = false;
        runtimeTargetingMode = null;
        lagTimeout = 0; // Reset lag timeout
        firstRotationFrame = true;
        previousTargetPos = Vec3.ZERO;
    }

    /**
     * Perform automatic sync check
     */
    private static void performAutomaticSyncCheck(LocalPlayer player) {
        if (targetEntity == null) return;

        // Check if target is still valid
        if (!isTargetStillValid(targetEntity, player)) {
            showMessage(player, "§6Auto-sync: Cleared invalid target");
            clearTargetWithReason("Automatic sync check failed");
        }
    }

    /**
     * Force sync check for debugging
     */
    public static void forceSyncCheck() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (targetEntity != null && !isTargetStillValid(targetEntity, player)) {
            clearTargetWithReason("Manual sync check failed");
            showMessage(player, "§cDesync detected - cleared stale target");
        } else if (targetEntity != null) {
            showMessage(player, "§aTarget sync OK");
        } else {
            showMessage(player, "§7No target to check");
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
     * Enhanced input handling with Shoulder Surfing compatibility
     */
    private static void handleInput(LocalPlayer player) {
        // Check if Shoulder Surfing is interfering
        boolean isShoulderSurfingActive = ThirdPersonCompatibility.getActiveMod() == ThirdPersonCompatibility.ActiveThirdPersonMod.SHOULDER_SURFING;

        boolean lockKeyPressed = LockOnKeybinds.lockOnKey.isDown();
        boolean lockKeyClicked = LockOnKeybinds.lockOnKey.consumeClick();
        boolean cycleKeyClicked = LockOnKeybinds.cycleTargetKey.consumeClick();
        boolean reverseCycleKeyClicked = LockOnKeybinds.cycleTargetReverseKey.consumeClick();
        boolean clearKeyClicked = LockOnKeybinds.clearTargetKey.consumeClick();

        // Debug input detection when Shoulder Surfing is active
        if (isShoulderSurfingActive && (lockKeyPressed || lockKeyClicked)) {
            LockOnMod.LOGGER.info("INPUT DEBUG - Shoulder Surfing Active:");
            LockOnMod.LOGGER.info("  Lock Key Pressed: {}", lockKeyPressed);
            LockOnMod.LOGGER.info("  Lock Key Clicked: {}", lockKeyClicked);
            LockOnMod.LOGGER.info("  Current Target: {}", targetEntity != null ? targetEntity.getDisplayName().getString() : "None");
        }

        // Handle lock-on based on configuration mode
        boolean toggleMode = getConfigBoolean("toggleMode", true);
        boolean holdMode = getConfigBoolean("holdMode", false);

        if (toggleMode) {
            if (lockKeyClicked) {
                if (targetEntity == null) {
                    LockOnMod.LOGGER.info("Toggle mode: Attempting to lock target");
                    findAndLockTargetOptimized(player);
                } else {
                    LockOnMod.LOGGER.info("Toggle mode: Clearing existing target");
                    clearTarget();
                }
            }
        } else if (holdMode) {
            if (lockKeyPressed && !wasKeyHeld) {
                LockOnMod.LOGGER.info("Hold mode: Key pressed, attempting lock");
                findAndLockTargetOptimized(player);
            } else if (!lockKeyPressed && wasKeyHeld) {
                LockOnMod.LOGGER.info("Hold mode: Key released, clearing target");
                clearTarget();
            }
        } else {
            if (lockKeyClicked) {
                LockOnMod.LOGGER.info("Click mode: Attempting to lock target");
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
        if (LockOnKeybinds.toggleIndicatorKey.isDown() &&
                LockOnKeybinds.cycleIndicatorTypeKey.isDown() &&
                LockOnKeybinds.clearTargetKey.isDown()) {
            // Triple key combo for manual sync check
            forceSyncCheck();
        }
        if (LockOnKeybinds.forceSyncCheckKey.consumeClick()) {
            forceSyncCheck();
        }
    }

    /**
     * Optimized target finding with caching and third person support
     */
    private static void findAndLockTargetOptimized(LocalPlayer player) {
        // Debug current state
        LockOnMod.LOGGER.info("=== LOCK-ON ATTEMPT ===");
        LockOnMod.LOGGER.info("Third Person Mod: {}", ThirdPersonCompatibility.getActiveMod().name());
        LockOnMod.LOGGER.info("Third Person Active: {}", ThirdPersonCompatibility.isThirdPersonActive());

        List<Entity> targets = findValidTargetsOptimized(player);

        if (targets.isEmpty()) {
            showMessage(player, "No Valid Targets Found");
            playSound(player, "target_lost");
            LockOnMod.LOGGER.info("No valid targets found");
            return;
        }

        LockOnMod.LOGGER.info("Found {} potential targets", targets.size());

        // Apply third person adjustments to targeting
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            targets = adjustTargetsForThirdPerson(targets, player);
            LockOnMod.LOGGER.info("After third person adjustment: {} targets", targets.size());
        }

        Entity newTarget = selectBestTargetOptimized(targets, player);

        if (newTarget != null) {
            LockOnMod.LOGGER.info("Selected target: {}", newTarget.getDisplayName().getString());

            setTarget(newTarget);
            potentialTargets = targets;
            currentTargetIndex = targets.indexOf(newTarget);
            showMessage(player, "Target Locked: " + getEntityDisplayName(newTarget));
            playSound(player, "lock_on");

            // Reset rotation state for smooth initial lock
            firstRotationFrame = true;

            LockOnMod.LOGGER.info("Lock-on successful!");
        } else {
            LockOnMod.LOGGER.warn("Failed to select target from {} candidates", targets.size());
        }
        LockOnMod.LOGGER.info("======================");
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

        // === ADD THIS MISSING BLACKLIST/WHITELIST VALIDATION ===
        ResourceLocation entityId = EntityType.getKey(entity.getType());
        String entityIdString = entityId.toString();

        // Debug logging
        LockOnMod.LOGGER.info("=== TARGETING DEBUG ===");
        LockOnMod.LOGGER.info("Entity: {} (ID: {})", entity.getDisplayName().getString(), entityIdString);
        LockOnMod.LOGGER.info("Use Whitelist: {}", LockOnConfig.useWhitelist());

        if (LockOnConfig.useWhitelist()) {
            List<String> whitelist = LockOnConfig.getEntityWhitelist();
            LockOnMod.LOGGER.info("Whitelist: {}", whitelist);

            boolean inWhitelist = whitelist.contains(entityIdString);
            LockOnMod.LOGGER.info("Entity {} {} in whitelist", entityIdString, inWhitelist ? "IS" : "IS NOT");

            if (!inWhitelist) {
                LockOnMod.LOGGER.info("REJECTED: Entity {} not in whitelist", entityIdString);
                return false;
            } else {
                LockOnMod.LOGGER.info("ACCEPTED: Entity {} found in whitelist", entityIdString);
            }
        } else {
            List<String> blacklist = LockOnConfig.getEntityBlacklist();
            LockOnMod.LOGGER.info("Blacklist: {}", blacklist);

            boolean inBlacklist = blacklist.contains(entityIdString);
            LockOnMod.LOGGER.info("Entity {} {} in blacklist", entityIdString, inBlacklist ? "IS" : "IS NOT");

            if (inBlacklist) {
                LockOnMod.LOGGER.info("REJECTED: Entity {} found in blacklist", entityIdString);
                return false;
            } else {
                LockOnMod.LOGGER.info("ACCEPTED: Entity {} not in blacklist", entityIdString);
            }
        }

        LockOnMod.LOGGER.info("=== END DEBUG ===");
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

    // Replace your updateCameraRotationOptimized method with this faster but still smooth version:

    /**
     * Balanced camera rotation - faster but smooth
     */
    private static void updateCameraRotationOptimized(LocalPlayer player) {
        if (targetEntity == null || !targetEntity.isAlive()) {
            if (targetEntity != null) {
                onTargetLost();
                clearTarget();
            }
            return;
        }

        // Debug info (keep your original debug)
        debugCameraRotation(player, targetEntity);

        // FORCE ENABLE for testing - ignore config (keep your original logic)
        boolean smoothCamera = true; // Force enabled
        if (!smoothCamera) {
            LockOnMod.LOGGER.info("Smooth camera is DISABLED in config");
            return;
        }

        // Calculate target rotation directly (keep your original third person adjustments)
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 targetPos = targetEntity.getEyePosition();

        // Calculate look direction
        Vec3 direction = targetPos.subtract(playerEyePos).normalize();
        float targetYaw = (float) (Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float targetPitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);

        // Get current rotation
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // ADAPTIVE rotation speed based on distance and angle difference
        float distance = player.distanceTo(targetEntity);
        float yawDiff = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Calculate adaptive speed
        float baseSpeed = 0.65f; // Increased from 0.15f

        // Distance scaling - closer targets need faster rotation
        float distanceScale = 1.0f;
        if (distance < 2.0) {
            distanceScale = 2.0f; // Much faster for very close targets
        } else if (distance < 5.0) {
            distanceScale = 1.5f; // Faster for close targets
        } else if (distance > 15.0) {
            distanceScale = 0.7f; // Slightly slower for far targets
        }

        // Angle scaling - bigger differences need faster rotation
        float angleDiff = Math.abs(yawDiff) + Math.abs(pitchDiff);
        float angleScale = 1.0f;
        if (angleDiff > 30.0f) {
            angleScale = 2.0f; // Much faster for large angle differences
        } else if (angleDiff > 10.0f) {
            angleScale = 1.5f; // Faster for medium angle differences
        }

        float rotationSpeed = baseSpeed * distanceScale * angleScale;

        // Cap the maximum speed to prevent wild spinning
        rotationSpeed = Math.min(rotationSpeed, 0.8f);

        // Reduce micro-movement threshold for more responsive tracking
        if (Math.abs(yawDiff) < 0.2f && Math.abs(pitchDiff) < 0.2f) {
            return; // Skip very small movements
        }

        // Apply ADAPTIVE rotation
        float newYaw = currentYaw + yawDiff * rotationSpeed;
        float newPitch = currentPitch + pitchDiff * rotationSpeed;

        // Clamp pitch
        newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));

        // Log rotation info with new adaptive values
        LockOnMod.LOGGER.info("ADAPTIVE ROTATION:");
        LockOnMod.LOGGER.info("  Distance: {}, Angle Diff: {}", distance, angleDiff);
        LockOnMod.LOGGER.info("  Distance Scale: {}, Angle Scale: {}", distanceScale, angleScale);
        LockOnMod.LOGGER.info("  Final Speed: {}", rotationSpeed);
        LockOnMod.LOGGER.info("  Current Yaw/Pitch: {}/{}", currentYaw, currentPitch);
        LockOnMod.LOGGER.info("  Target Yaw/Pitch: {}/{}", targetYaw, targetPitch);
        LockOnMod.LOGGER.info("  Applying Yaw/Pitch: {}/{}", newYaw, newPitch);
        LockOnMod.LOGGER.info("  Differences: {}/{}", yawDiff, pitchDiff);

        // Keep your original rotation methods but with adaptive speed
        try {
            // Method 1: Direct setters
            player.setYRot(newYaw);
            player.setXRot(newPitch);
            LockOnMod.LOGGER.info("Applied Method 1: setYRot/setXRot");

            // Method 2: Head rotation
            player.setYHeadRot(newYaw);
            LockOnMod.LOGGER.info("Applied Method 2: setYHeadRot");

            // Method 3: Turn method (with adaptive speed)
            player.turn(yawDiff * rotationSpeed * 0.3f, pitchDiff * rotationSpeed * 0.3f);
            LockOnMod.LOGGER.info("Applied Method 3: turn (adaptive)");

            // Method 4: Force previous rotation values
            player.yRotO = newYaw;
            player.xRotO = newPitch;
            LockOnMod.LOGGER.info("Applied Method 4: yRotO/xRotO");

        } catch (Exception e) {
            LockOnMod.LOGGER.error("Failed to apply camera rotation: {}", e.getMessage());
        }

        // Verify the rotation was applied (keep your original verification)
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
     * Enhanced targeting state update with sync protection
     */
    private static void updateTargetingOptimizedWithSync(LocalPlayer player, long currentTick) {
        if (targetEntity == null) return;

        // NEW: Enhanced validation with server sync checking
        if (!isTargetStillValid(targetEntity, player)) {
            clearTargetWithReason("Target no longer valid or desynced");
            return;
        }

        // NEW: Check for network lag indicators
        if (isNetworkLagging(player)) {
            handleLaggyTargeting(player);
            return;
        }

        // Auto-break on obstruction (check less frequently)
        boolean autoBreak = getConfigBoolean("autoBreakOnObstruction", true);
        if (currentTick % 5 == 0 && autoBreak) {
            if (!hasLineOfSightCached(player, targetEntity)) {
                clearTargetWithReason("Line of sight lost");
                return;
            }
        }

        // Check distance (check less frequently for performance)
        if (currentTick % 10 == 0) {
            double distance = (double) player.distanceTo(targetEntity);
            double maxRange = getMaxTargetingRangeAdjusted();

            if (distance > maxRange) {
                clearTargetWithReason("Target out of range");
                return;
            }
        }

        // Reset lag timeout if everything is fine
        lagTimeout = 0;
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

        // NEW: Debug keybind (you can assign this to any key you want)
        // For now, let's trigger it when both indicator keys are pressed together
        if (LockOnKeybinds.toggleIndicatorKey.isDown() && LockOnKeybinds.cycleIndicatorTypeKey.isDown()) {
            debugShoulderSurfingInput(player);
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

        // NEW: Handle third person compatibility and input conflicts
        ThirdPersonCompatibility.ensurePlayerRotationFollowsCamera();

        // NEW: Specifically handle Shoulder Surfing input conflicts
        if (ThirdPersonCompatibility.getActiveMod() == ThirdPersonCompatibility.ActiveThirdPersonMod.SHOULDER_SURFING) {
            ThirdPersonCompatibility.disableShoulderSurfingInput();
            LockOnMod.LOGGER.debug("Disabled Shoulder Surfing input for lock-on compatibility");
        }
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

            // NEW: Restore third person settings and input handling
            ThirdPersonCompatibility.restorePlayerRotationSettings();

            // NEW: Specifically re-enable Shoulder Surfing input
            if (ThirdPersonCompatibility.getActiveMod() == ThirdPersonCompatibility.ActiveThirdPersonMod.SHOULDER_SURFING) {
                ThirdPersonCompatibility.enableShoulderSurfingInput();
                LockOnMod.LOGGER.debug("Re-enabled Shoulder Surfing input after lock-on");
            }
        }

        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = -1;
        wasLocked = false;
        runtimeTargetingMode = null;
        firstRotationFrame = true;
        previousTargetPos = Vec3.ZERO;

        // ADD THESE MISSING LINES FOR SYNC PROTECTION:
        lagTimeout = 0; // Reset lag detection
        wasNetworkLagging = false; // Reset network state

        // Clear caches to prevent stale entity references
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();
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
        // Clear target state first
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

        // NEW: Restore third person compatibility settings
        try {
            ThirdPersonCompatibility.restorePlayerRotationSettings();
            if (ThirdPersonCompatibility.getActiveMod() == ThirdPersonCompatibility.ActiveThirdPersonMod.SHOULDER_SURFING) {
                ThirdPersonCompatibility.enableShoulderSurfingInput();
            }
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Failed to restore third person settings during emergency reset: {}", e.getMessage());
        }

        LockOnMod.LOGGER.info("Lock-On System emergency reset completed with optimizations and third person compatibility");
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

    /**
     * Debug method specifically for Shoulder Surfing input issues
     */
    public static void debugShoulderSurfingInput(LocalPlayer player) {
        if (player == null) return;

        LockOnMod.LOGGER.info("=== SHOULDER SURFING DEBUG ===");
        LockOnMod.LOGGER.info("Active Mod: {}", ThirdPersonCompatibility.getActiveMod().name());
        LockOnMod.LOGGER.info("Third Person Active: {}", ThirdPersonCompatibility.isThirdPersonActive());
        LockOnMod.LOGGER.info("Detailed Status: {}", ThirdPersonCompatibility.getDetailedStatus());

        // Test key states
        LockOnMod.LOGGER.info("Key States:");
        LockOnMod.LOGGER.info("  Lock Key Down: {}", LockOnKeybinds.lockOnKey.isDown());
        LockOnMod.LOGGER.info("  Cycle Key Down: {}", LockOnKeybinds.cycleTargetKey.isDown());

        // Test target state
        LockOnMod.LOGGER.info("Target State:");
        LockOnMod.LOGGER.info("  Has Target: {}", hasTarget());
        LockOnMod.LOGGER.info("  Target Entity: {}", targetEntity != null ? targetEntity.getDisplayName().getString() : "None");

        // Camera state
        LockOnMod.LOGGER.info("Camera State:");
        LockOnMod.LOGGER.info("  Player Yaw: {}", player.getYRot());
        LockOnMod.LOGGER.info("  Player Pitch: {}", player.getXRot());

        LockOnMod.LOGGER.info("==============================");
    }
}