package net.leolifeless.lockonmod;

import net.leolifeless.lockonmod.compat.ThirdPersonCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LockOnSystem {
    private static Entity targetEntity = null;
    private static List<Entity> potentialTargets = new ArrayList<>();
    private static int currentTargetIndex = -1;
    private static boolean wasKeyHeld = false;
    private static boolean indicatorVisible = true;
    private static boolean wasLocked = false;
    private static LockOnConfig.TargetingMode runtimeTargetingMode = null;

    // Enhanced for third person compatibility
    private static Vector3d lastCameraOffset = Vector3d.ZERO;
    private static boolean lastThirdPersonState = false;
    private static long lastUpdateTime = 0;

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
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Performance optimization - only update every few ticks based on config
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < (LockOnConfig.getUpdateFrequency() * 50)) {
            return;
        }
        lastUpdateTime = currentTime;

        // Check for third person state changes
        boolean currentThirdPersonState = ThirdPersonCompatibility.isThirdPersonActive();
        if (currentThirdPersonState != lastThirdPersonState) {
            onThirdPersonStateChanged(currentThirdPersonState);
            lastThirdPersonState = currentThirdPersonState;
        }

        // Update camera offset tracking
        if (currentThirdPersonState) {
            lastCameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
        }

        // Check if lock-on should be disabled for current game mode
        if (shouldDisableForGameMode(player)) {
            clearTarget();
            return;
        }

        handleInput(player);
        updateTargeting(player);
        updateCameraRotation(player);
    }

    /**
     * Handle third person state changes
     */
    private static void onThirdPersonStateChanged(boolean isThirdPerson) {
        if (isThirdPerson) {
            LockOnMod.LOGGER.debug("Switched to third person - enabling enhanced lock-on features");
            // Optionally adjust current target if locked
            if (targetEntity != null) {
                updateTargetingParameters();
            }
        } else {
            LockOnMod.LOGGER.debug("Switched to first person - using standard lock-on features");
        }
    }

    /**
     * Update targeting parameters when switching perspectives
     */
    private static void updateTargetingParameters() {
        // Recalculate targeting validity in new perspective
        if (targetEntity != null) {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null && !isValidTarget(targetEntity, player)) {
                clearTarget();
            }
        }
    }

    /**
     * Enhanced input handling with third person considerations
     */
    private static void handleInput(ClientPlayerEntity player) {
        boolean lockKeyPressed = LockOnKeybinds.lockOnKey.isDown();
        boolean lockKeyClicked = LockOnKeybinds.lockOnKey.consumeClick();
        boolean cycleKeyClicked = LockOnKeybinds.cycleTargetKey.consumeClick();
        boolean reverseCycleKeyClicked = LockOnKeybinds.cycleTargetReverseKey.consumeClick();
        boolean clearKeyClicked = LockOnKeybinds.clearTargetKey.consumeClick();

        // Handle lock-on based on configuration mode
        if (LockOnConfig.isToggleMode()) {
            if (lockKeyClicked) {
                if (targetEntity == null) {
                    findAndLockTarget(player);
                } else {
                    clearTarget();
                }
            }
        } else if (LockOnConfig.holdToMaintainLock()) {
            if (lockKeyPressed && !wasKeyHeld) {
                findAndLockTarget(player);
            } else if (!lockKeyPressed && wasKeyHeld) {
                clearTarget();
            }
        } else {
            if (lockKeyClicked) {
                findAndLockTarget(player);
            }
        }

        if (LockOnKeybinds.cycleIndicatorTypeKey.consumeClick()) {
            LockOnHudRenderer.cycleCrosshair();
            showMessage(player, "Crosshair: " + LockOnHudRenderer.getCurrentCrosshairInfo());
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.toggleIndicatorKey.consumeClick()) {
            LockOnHudRenderer.cycleCrosshair();
            showMessage(player, "Crosshair: " + LockOnHudRenderer.getCurrentCrosshairInfo());
            playSound(player, "target_switch");
        }


        // Handle target cycling
        if (cycleKeyClicked && LockOnConfig.canCycleThroughTargets()) {
            cycleTarget(player, false);
        }

        if (reverseCycleKeyClicked && LockOnConfig.canCycleThroughTargets()) {
            cycleTarget(player, true);
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
     * Enhanced targeting with third person compatibility
     */
    private static void findAndLockTarget(ClientPlayerEntity player) {
        List<Entity> targets = findValidTargets(player);

        if (targets.isEmpty()) {
            showMessage(player, "No Valid Targets Found");
            playSound(player, "target_lost");
            return;
        }

        // Apply third person adjustments to targeting
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            targets = adjustTargetsForThirdPerson(targets, player);
        }

        Entity newTarget = selectBestTarget(targets, player);

        if (newTarget != null) {
            setTarget(newTarget);
            potentialTargets = targets;
            currentTargetIndex = targets.indexOf(newTarget);
            showMessage(player, "Target Locked: " + getEntityDisplayName(newTarget));
            playSound(player, "lock_on");
        }
    }

    /**
     * Adjust target list for third person perspective
     */
    private static List<Entity> adjustTargetsForThirdPerson(List<Entity> targets, ClientPlayerEntity player) {
        Vector3d cameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();

        // Filter out targets that might be behind the camera in third person
        return targets.stream()
                .filter(entity -> {
                    Vector3d toEntity = entity.position().subtract(player.position());
                    Vector3d adjustedCameraPos = player.position().add(cameraOffset);
                    Vector3d toCameraEntity = entity.position().subtract(adjustedCameraPos);

                    // Ensure target is generally in front of the camera
                    return toCameraEntity.dot(player.getLookAngle()) > -0.5;
                })
                .collect(Collectors.toList());
    }

    /**
     * Enhanced target finding with third person range adjustments
     */
    private static List<Entity> findValidTargets(ClientPlayerEntity player) {
        // Get base range and adjust for third person
        double baseRange = LockOnConfig.getMaxLockOnDistance();
        final double range;
        final double searchRadius;

        try {
            range = ThirdPersonCompatibility.isThirdPersonActive() ?
                    ThirdPersonCompatibility.getAdjustedTargetingRange(baseRange) : baseRange;

            // Get search radius with third person adjustments
            double baseSearchRadius = LockOnConfig.getSearchRadius();
            searchRadius = ThirdPersonCompatibility.isThirdPersonActive() ?
                    ThirdPersonCompatibility.getAdjustedTargetingRange(baseSearchRadius) : baseSearchRadius;
        } catch (Exception e) {
            // Fallback if third person compatibility fails
            LockOnMod.LOGGER.debug("Third person compatibility error, using defaults: " + e.getMessage());
            final double fallbackRange = baseRange;
            final double fallbackSearchRadius = LockOnConfig.getSearchRadius();

            AxisAlignedBB searchBox = player.getBoundingBox().inflate(fallbackSearchRadius);
            List<Entity> nearbyEntities = player.level.getEntities(player, searchBox);

            return nearbyEntities.stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .filter(entity -> entity != player)
                    .filter(entity -> player.distanceToSqr(entity) <= fallbackRange * fallbackRange)
                    .filter(entity -> isValidTarget(entity, player))
                    .filter(entity -> hasLineOfSight(player, entity))
                    .filter(entity -> isWithinTargetingAngle(player, entity))
                    .limit(LockOnConfig.getMaxTargetsToSearch())
                    .collect(Collectors.toList());
        }

        AxisAlignedBB searchBox = player.getBoundingBox().inflate(searchRadius);
        List<Entity> nearbyEntities = player.level.getEntities(player, searchBox);

        return nearbyEntities.stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity != player)
                .filter(entity -> player.distanceToSqr(entity) <= range * range)
                .filter(entity -> isValidTarget(entity, player))
                .filter(entity -> hasLineOfSight(player, entity))
                .filter(entity -> isWithinTargetingAngle(player, entity))
                .limit(LockOnConfig.getMaxTargetsToSearch())
                .collect(Collectors.toList());
    }

    /**
     * Enhanced angle checking with third person adjustments
     */
    private static boolean isWithinTargetingAngle(ClientPlayerEntity player, Entity target) {
        // Get base angle and adjust for third person
        double maxAngle = LockOnConfig.getTargetingAngle();
        try {
            if (ThirdPersonCompatibility.isThirdPersonActive()) {
                maxAngle = ThirdPersonCompatibility.getAdjustedTargetingAngle(maxAngle);
            }
        } catch (Exception e) {
            // Fallback if third person compatibility fails
            LockOnMod.LOGGER.debug("Third person angle adjustment failed, using default: " + e.getMessage());
        }

        Vector3d playerPos = player.getEyePosition(1.0f);
        Vector3d targetPos = target.getEyePosition(1.0f);
        Vector3d lookDirection = player.getLookAngle();

        // Adjust for third person camera offset
        try {
            if (ThirdPersonCompatibility.isThirdPersonActive()) {
                Vector3d cameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
                playerPos = playerPos.add(cameraOffset.scale(0.5)); // Partial offset for better targeting
            }
        } catch (Exception e) {
            // Fallback if third person compatibility fails
            LockOnMod.LOGGER.debug("Third person camera offset failed, using default position: " + e.getMessage());
        }

        Vector3d toTarget = targetPos.subtract(playerPos).normalize();
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, lookDirection.dot(toTarget)))) * 180.0 / Math.PI;

        return angle <= maxAngle;
    }

    /**
     * Comprehensive target validation
     */
    private static boolean isValidTarget(Entity entity, ClientPlayerEntity player) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == player) return false;
        if (!entity.isAlive()) return false;

        LivingEntity living = (LivingEntity) entity;

        // Health filtering
        float health = living.getHealth();
        if (health < LockOnConfig.getMinTargetHealth()) return false;
        if (LockOnConfig.getMaxTargetHealth() > 0 && health > LockOnConfig.getMaxTargetHealth()) return false;

        // Entity type filtering
        if (entity instanceof PlayerEntity && !LockOnConfig.canTargetPlayers()) return false;
        if (entity instanceof MonsterEntity && !LockOnConfig.canTargetHostileMobs()) return false;
        if (entity instanceof AnimalEntity && !LockOnConfig.canTargetPassiveMobs()) return false;
        if ((entity instanceof WitherEntity || entity instanceof EnderDragonEntity) && !LockOnConfig.canTargetBosses()) return false;

        // Blacklist/Whitelist filtering
        ResourceLocation entityId = entity.getType().getRegistryName();
        if (entityId == null) return false;
        String entityIdString = entityId.toString();

        if (LockOnConfig.useWhitelist()) {
            return LockOnConfig.getEntityWhitelist().contains(entityIdString);
        } else {
            return !LockOnConfig.getEntityBlacklist().contains(entityIdString);
        }
    }

    /**
     * Enhanced line of sight checking
     */
    private static boolean hasLineOfSight(ClientPlayerEntity player, Entity target) {
        if (!LockOnConfig.requireLineOfSight()) return true;

        Vector3d start = player.getEyePosition(1.0f);
        Vector3d end = target.getEyePosition(1.0f);

        // Adjust for third person camera position
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vector3d cameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            start = start.add(cameraOffset.scale(0.2)); // Slight adjustment
        }

        BlockRayTraceResult result = player.level.clip(new RayTraceContext(
                start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));

        if (result.getType() == RayTraceResult.Type.BLOCK) {
            BlockState blockState = player.level.getBlockState(result.getBlockPos());

            // Allow targeting through glass if configured
            if (LockOnConfig.penetrateGlass() &&
                    (blockState.is(Blocks.GLASS) || blockState.is(Blocks.GLASS_PANE) ||
                            blockState.getBlock().toString().contains("glass"))) {
                return true;
            }

            return false;
        }

        return true;
    }

    /**
     * Select the best target based on configured mode
     */
    private static Entity selectBestTarget(List<Entity> targets, ClientPlayerEntity player) {
        if (targets.isEmpty()) return null;

        // Use configured targeting mode or runtime override
        LockOnConfig.TargetingMode mode = runtimeTargetingMode != null ?
                runtimeTargetingMode : LockOnConfig.getTargetingMode();

        switch (mode) {
            case CLOSEST:
                return targets.stream()
                        .min(Comparator.comparing(e -> player.distanceToSqr(e)))
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

            case SMART:
                return targets.stream()
                        .min(Comparator.comparing(e -> calculateSmartScore(player, e)))
                        .orElse(null);

            default:
                return targets.get(0);
        }
    }

    /**
     * Calculate distance from crosshair for targeting
     */
    private static double calculateCrosshairDistance(ClientPlayerEntity player, Entity entity) {
        Vector3d lookDirection = player.getLookAngle();
        Vector3d toEntity = entity.getEyePosition(1.0f).subtract(player.getEyePosition(1.0f)).normalize();
        return Math.acos(Math.max(-1.0, Math.min(1.0, lookDirection.dot(toEntity))));
    }

    /**
     * Calculate threat level for targeting
     */
    private static double calculateThreatLevel(Entity entity) {
        if (entity instanceof MonsterEntity) return 3.0;
        if (entity instanceof PlayerEntity) return 2.0;
        if (entity instanceof AnimalEntity) return 1.0;
        return 0.0;
    }

    /**
     * Calculate smart targeting score (lower is better)
     */
    private static double calculateSmartScore(ClientPlayerEntity player, Entity entity) {
        if (!(entity instanceof LivingEntity)) return Double.MAX_VALUE;

        LivingEntity living = (LivingEntity) entity;
        double distance = player.distanceToSqr(entity); // Use squared distance for performance
        double angle = calculateCrosshairDistance(player, entity);
        double healthPercent = living.getHealth() / living.getMaxHealth();
        double threat = calculateThreatLevel(entity);

        // Weighted scoring (distance is already squared, so use it directly)
        double distanceScore = distance * LockOnConfig.getDistancePriorityWeight();
        double angleScore = angle * LockOnConfig.getAnglePriorityWeight();
        double healthScore = (1.0 - healthPercent) * LockOnConfig.getHealthPriorityWeight();
        double threatScore = (4.0 - threat) * 0.1; // Slight threat preference

        return distanceScore + angleScore + healthScore + threatScore;
    }

    /**
     * Enhanced camera rotation with third person smoothing - FIXED VERSION
     */
    private static void updateCameraRotation(ClientPlayerEntity player) {
        if (targetEntity == null || !targetEntity.isAlive()) {
            if (targetEntity != null) {
                onTargetLost();
                clearTarget();
            }
            return;
        }

        if (!LockOnConfig.isSmoothCameraEnabled()) {
            // Direct snap to target if smoothing disabled
            snapCameraToTarget(player);
            return;
        }

        // Calculate rotation with third person adjustments
        Vector3d playerEyePos = player.getEyePosition(1.0f);

        // Adjust eye position for third person camera
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vector3d cameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            playerEyePos = playerEyePos.add(cameraOffset.scale(0.3)); // Partial offset
        }

        // Get adjusted target position for third person
        Vector3d targetPos = targetEntity.getEyePosition(1.0f);
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            targetPos = ThirdPersonCompatibility.getAdjustedTargetPosition(targetEntity, targetPos);
        }

        // Predictive targeting
        if (LockOnConfig.isPredictiveTargetingEnabled()) {
            Vector3d velocity = targetEntity.getDeltaMovement();
            targetPos = targetPos.add(velocity.scale(3.0)); // Predict 3 ticks ahead
        }

        // Calculate look direction
        Vector3d direction = targetPos.subtract(playerEyePos).normalize();
        float targetYaw = (float) (Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float targetPitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);

        // Get base rotation speed
        float baseRotationSpeed = LockOnConfig.getRotationSpeed();

        // Apply third person adjustment if needed
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            baseRotationSpeed = ThirdPersonCompatibility.getAdjustedRotationSpeed(baseRotationSpeed);
        }

        // Calculate adaptive speed multiplier
        float speedMultiplier = 1.0f;
        if (LockOnConfig.isAdaptiveRotationEnabled()) {
            speedMultiplier = calculateAdaptiveSpeedMultiplier(player, targetEntity);
        }

        // Final rotation speed - much more aggressive for responsive feel
        float finalRotationSpeed = Math.min(baseRotationSpeed * speedMultiplier * 8.0f, 0.8f); // Increased multiplier and cap

        // Apply smoothing - use simpler calculation
        float smoothingFactor = finalRotationSpeed;
        if (ThirdPersonCompatibility.shouldUseEnhancedSmoothing()) {
            smoothingFactor = ThirdPersonCompatibility.getThirdPersonSmoothingFactor(smoothingFactor);
        }

        // Apply smooth rotation
        float currentYaw = player.yRot;
        float currentPitch = player.xRot;

        float newYaw = interpolateRotation(currentYaw, targetYaw, smoothingFactor);
        float newPitch = interpolateRotation(currentPitch, targetPitch, smoothingFactor);

        // Clamp pitch to reasonable values
        newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));

        player.yRot = newYaw;
        player.xRot = newPitch;
    }

    /**
     * Snap camera directly to target (no smoothing)
     */
    private static void snapCameraToTarget(ClientPlayerEntity player) {
        Vector3d playerEyePos = player.getEyePosition(1.0f);
        Vector3d targetPos = targetEntity.getEyePosition(1.0f);

        // Adjust for third person if needed
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vector3d cameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            playerEyePos = playerEyePos.add(cameraOffset.scale(0.3));
            targetPos = ThirdPersonCompatibility.getAdjustedTargetPosition(targetEntity, targetPos);
        }

        // Calculate direction and set rotation directly
        Vector3d direction = targetPos.subtract(playerEyePos).normalize();
        float targetYaw = (float) (Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float targetPitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);

        // Clamp pitch
        targetPitch = Math.max(-90.0f, Math.min(90.0f, targetPitch));

        player.yRot = targetYaw;
        player.xRot = targetPitch;
    }

    /**
     * Calculate adaptive speed multiplier (simplified version)
     */
    private static float calculateAdaptiveSpeedMultiplier(ClientPlayerEntity player, Entity target) {
        // Calculate factors for adaptive speed - use actual distance, not squared
        double distance = player.distanceTo(target);
        Vector3d direction = target.getEyePosition(1.0f).subtract(player.getEyePosition(1.0f)).normalize();
        Vector3d lookDirection = player.getLookAngle();
        double angleDifference = Math.acos(Math.max(-1.0, Math.min(1.0, lookDirection.dot(direction)))) * 180.0 / Math.PI;

        // Adjust for third person
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            distance += ThirdPersonCompatibility.getThirdPersonCameraOffset().length();
        }

        // Simple adaptive calculation - closer targets and larger angles = faster rotation
        float distanceFactor = (float) Math.max(0.5, 2.0 - (distance / LockOnConfig.getMaxLockOnDistance()));
        float angleFactor = (float) Math.max(0.5, 1.0 + (angleDifference / 90.0)); // More aggressive angle response

        return distanceFactor * angleFactor;
    }

    /**
     * Calculate adaptive rotation speed with proper type casting - DEPRECATED
     * This method is kept for compatibility but should not be used
     */
    private static float calculateAdaptiveRotationSpeed(ClientPlayerEntity player, Entity target) {
        // This method was causing the slow rotation - keeping for backward compatibility
        return LockOnConfig.getRotationSpeed() * calculateAdaptiveSpeedMultiplier(player, target);
    }

    /**
     * Smoothly interpolates between angles
     */
    private static float interpolateRotation(float prev, float current, float factor) {
        float diff = current - prev;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return prev + diff * factor;
    }

    /**
     * Update targeting state and auto-break if configured
     */
    private static void updateTargeting(ClientPlayerEntity player) {
        if (targetEntity == null) return;

        // Check if target is still valid
        if (!targetEntity.isAlive() || !isValidTarget(targetEntity, player)) {
            clearTarget();
            return;
        }

        // Auto-break on obstruction
        if (LockOnConfig.isAutoBreakOnObstructionEnabled() && !hasLineOfSight(player, targetEntity)) {
            clearTarget();
            return;
        }

        // Check if target is still in range - use actual distance
        double distance = player.distanceTo(targetEntity);
        double maxRange = LockOnConfig.getMaxLockOnDistance();
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            maxRange = ThirdPersonCompatibility.getAdjustedTargetingRange(maxRange);
        }

        if (distance > maxRange) {
            clearTarget();
            return;
        }
    }

    /**
     * Cycle through available targets
     */
    private static void cycleTarget(ClientPlayerEntity player, boolean reverse) {
        if (potentialTargets.isEmpty()) {
            findAndLockTarget(player);
            return;
        }

        // Filter out invalid targets
        potentialTargets = potentialTargets.stream()
                .filter(entity -> entity.isAlive() && isValidTarget(entity, player))
                .collect(Collectors.toList());

        if (potentialTargets.isEmpty()) {
            clearTarget();
            return;
        }

        // Calculate next index
        int direction = reverse ? -1 : 1;
        if (LockOnConfig.reverseScrollCycling()) {
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
    }

    /**
     * Handles targeting mode shortcut keys
     */
    private static void handleTargetingModeShortcuts(ClientPlayerEntity player) {
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
    private static void handleFilterToggles(ClientPlayerEntity player) {
        if (LockOnKeybinds.togglePlayersKey.consumeClick()) {
            showMessage(player, "Player Targeting: " + (LockOnConfig.canTargetPlayers() ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.toggleHostilesKey.consumeClick()) {
            showMessage(player, "Hostile Mob Targeting: " + (LockOnConfig.canTargetHostileMobs() ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.togglePassivesKey.consumeClick()) {
            showMessage(player, "Passive Mob Targeting: " + (LockOnConfig.canTargetPassiveMobs() ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }
    }

    /**
     * Handles visual control keys including custom indicator cycling
     */
    private static void handleVisualControls(ClientPlayerEntity player) {
        // Toggle indicator visibility
        if (LockOnKeybinds.toggleIndicatorKey.consumeClick()) {
            indicatorVisible = !indicatorVisible;
            showMessage(player, "Lock-On Indicator: " + (indicatorVisible ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }

        // Cycle indicator type
        if (LockOnKeybinds.cycleIndicatorTypeKey.consumeClick()) {
            showMessage(player, "Indicator Type: " + LockOnConfig.getIndicatorType().name());
            playSound(player, "target_switch");
        }
    }

    /**
     * Enhanced rendering for third person compatibility
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderWorldLastEvent event) {
        if (targetEntity == null || !indicatorVisible) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Get adjusted indicator size for third person
        float indicatorSize = LockOnConfig.getIndicatorSize();
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            indicatorSize = ThirdPersonCompatibility.getAdjustedIndicatorSize(indicatorSize);
        }

        // Render with enhanced positioning for third person
        Vector3d targetPos = targetEntity.getEyePosition(1.0f);
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            targetPos = ThirdPersonCompatibility.getAdjustedTargetPosition(targetEntity, targetPos);
        }

        LockOnRenderer.renderLockOnIndicator(
                event.getMatrixStack(),
                targetEntity,
                targetPos,
                indicatorSize,
                LockOnConfig.getIndicatorType(),
                ThirdPersonCompatibility.isThirdPersonActive()
        );
    }

    // === UTILITY METHODS ===

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
    }

    /**
     * Shows a message to the player with third person compatibility info
     */
    private static void showMessage(ClientPlayerEntity player, String message) {
        if (player != null) {
            // Add compatibility status for debug purposes
            if (LockOnMod.LOGGER.isDebugEnabled() && ThirdPersonCompatibility.isModLoaded()) {
                String compatMessage = message + " [" + ThirdPersonCompatibility.getCompatibilityStatus() + "]";
                player.displayClientMessage(new StringTextComponent(compatMessage), true);
            } else {
                player.displayClientMessage(new StringTextComponent(message), true);
            }
        }
    }

    /**
     * Plays appropriate sound based on type
     */
    private static void playSound(ClientPlayerEntity player, String soundType) {
        if (!LockOnConfig.areSoundsEnabled()) return;

        // Play appropriate sound based on type
        switch (soundType) {
            case "lock_on":
                if (LockOnConfig.playLockOnSound()) {
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS,
                            LockOnConfig.getSoundVolume(), 1.2f, false);
                }
                break;
            case "target_switch":
                if (LockOnConfig.playTargetSwitchSound()) {
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS,
                            LockOnConfig.getSoundVolume(), 1.5f, false);
                }
                break;
            case "target_lost":
                if (LockOnConfig.playTargetLostSound()) {
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundCategory.PLAYERS,
                            LockOnConfig.getSoundVolume(), 0.8f, false);
                }
                break;
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
    }

    /**
     * Handles target lost event
     */
    private static void onTargetLost() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            showMessage(player, "Target Lost");
            playSound(player, "target_lost");
        }
        wasLocked = false;
    }

    /**
     * Checks if lock-on should be disabled for current game mode
     */
    private static boolean shouldDisableForGameMode(ClientPlayerEntity player) {
        if (player.isCreative() && LockOnConfig.disableInCreative()) return true;
        if (player.isSpectator() && LockOnConfig.disableInSpectator()) return true;
        return false;
    }

    /**
     * Debug method to get current targeting state
     */
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Target: ").append(targetEntity != null ? getEntityDisplayName(targetEntity) : "None").append("\n");
        sb.append("Potential Targets: ").append(potentialTargets.size()).append("\n");
        sb.append("Third Person Active: ").append(ThirdPersonCompatibility.isThirdPersonActive()).append("\n");
        sb.append("Runtime Mode: ").append(runtimeTargetingMode != null ? runtimeTargetingMode : "Config Default").append("\n");
        return sb.toString();
    }

    /**
     * Force refresh of target list (useful for external integrations)
     */
    public static void refreshTargets() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            potentialTargets = findValidTargets(player);
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
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && targetEntity != null) {
            return player.distanceTo(targetEntity);
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
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && entity != null && isValidTarget(entity, player)) {
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
        ClientPlayerEntity player = Minecraft.getInstance().player;
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
        lastThirdPersonState = false;
        lastCameraOffset = Vector3d.ZERO;
        LockOnMod.LOGGER.info("Lock-On System emergency reset completed");
    }
}