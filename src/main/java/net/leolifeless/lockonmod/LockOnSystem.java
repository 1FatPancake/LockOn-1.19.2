package net.leolifeless.lockonmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
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
    private static int currentTargetIndex = 0;
    private static int tickCounter = 0;
    private static boolean wasKeyHeld = false;

    // Interpolation values for smooth camera movement
    private static float prevYaw = 0F;
    private static float prevPitch = 0F;
    private static boolean wasLocked = false;

    // Runtime settings for togglable features
    private static boolean indicatorVisible = true;
    private static LockOnConfig.TargetingMode runtimeTargetingMode = null;

    /**
     * Enhanced key input handler with multiple modes and custom indicator support
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Check game mode restrictions
        if (shouldDisableForGameMode(player)) return;

        boolean lockKeyPressed = LockOnKeybinds.lockOnKey.isDown();
        boolean lockKeyClicked = LockOnKeybinds.lockOnKey.consumeClick();
        boolean cycleKeyClicked = LockOnKeybinds.cycleTargetKey.consumeClick();
        boolean cycleReverseKeyClicked = LockOnKeybinds.cycleTargetReverseKey.consumeClick();
        boolean clearKeyClicked = LockOnKeybinds.clearTargetKey.consumeClick();

        // Handle different keybinding modes
        if (LockOnConfig.holdToMaintainLock()) {
            handleHoldMode(player, lockKeyPressed);
        } else if (LockOnConfig.isToggleMode()) {
            if (lockKeyClicked) {
                handleToggleMode(player);
            }
        }

        // Handle target cycling
        if (cycleKeyClicked && targetEntity != null && LockOnConfig.canCycleThroughTargets()) {
            cycleTarget(player, false);
        }

        if (cycleReverseKeyClicked && targetEntity != null && LockOnConfig.canCycleThroughTargets()) {
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
            boolean newValue = !LockOnConfig.canTargetPlayers();
            // Note: These would need to be runtime toggles since config values are typically immutable
            showMessage(player, "Player Targeting: " + (newValue ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.toggleHostilesKey.consumeClick()) {
            boolean newValue = !LockOnConfig.canTargetHostileMobs();
            showMessage(player, "Hostile Mob Targeting: " + (newValue ? "Enabled" : "Disabled"));
            playSound(player, "target_switch");
        }

        if (LockOnKeybinds.togglePassivesKey.consumeClick()) {
            boolean newValue = !LockOnConfig.canTargetPassiveMobs();
            showMessage(player, "Passive Mob Targeting: " + (newValue ? "Enabled" : "Disabled"));
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

        // Cycle indicator type (including custom indicators)
        if (LockOnKeybinds.cycleIndicatorTypeKey.consumeClick()) {
            cycleIndicatorType(player);
        }
    }

    /**
     * Cycles through indicator types including custom indicators
     */
    private static void cycleIndicatorType(LocalPlayer player) {
        LockOnConfig.IndicatorType currentType = LockOnConfig.getIndicatorType();

        if (currentType == LockOnConfig.IndicatorType.CUSTOM && LockOnConfig.isCustomIndicatorCyclingEnabled()) {
            // Cycle through custom indicators
            cycleCustomIndicator(player);
        } else {
            // Cycle through main indicator types
            LockOnConfig.IndicatorType[] types = LockOnConfig.IndicatorType.values();
            int currentIndex = currentType.ordinal();
            int nextIndex = (currentIndex + 1) % types.length;
            LockOnConfig.IndicatorType nextType = types[nextIndex];

            // Note: This would require a way to set config values at runtime
            showMessage(player, "Indicator Type: " + nextType.getDisplayName());
            playSound(player, "target_switch");
        }
    }

    /**
     * Cycles through available custom indicators
     */
    private static void cycleCustomIndicator(LocalPlayer player) {
        String nextIndicator = CustomIndicatorManager.cycleToNextIndicator();

        // Show message to player
        String message = "Custom Indicator: " + CustomIndicatorManager.getIndicatorInfo(nextIndicator);
        showMessage(player, message);

        // Play sound
        playSound(player, "target_switch");
    }

    /**
     * Enhanced client tick with configurable update frequency
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null) return;

        // Update only at configured frequency
        tickCounter++;
        if (tickCounter % LockOnConfig.getUpdateFrequency() != 0) return;

        // Validate current target
        if (targetEntity != null) {
            if (!isValidTarget(targetEntity, player)) {
                // Auto-find new target if current becomes invalid
                if (LockOnConfig.isSmartTargeting()) {
                    findTarget(player);
                    if (targetEntity == null) {
                        onTargetLost();
                    }
                } else {
                    clearTarget();
                }
            }
        }
    }

    /**
     * Enhanced camera control with predictive targeting
     */
    @SubscribeEvent
    public static void onCameraSetup(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null || targetEntity == null) return;
        if (!targetEntity.isAlive() || !player.isAlive()) return;

        // Check if camera should auto-break on obstruction
        if (LockOnConfig.autoBreakOnObstruction() && !hasLineOfSight(player, targetEntity)) {
            clearTarget();
            return;
        }

        // Get positions
        Vec3 playerPos = player.getEyePosition();
        Vec3 targetPos = getTargetPosition(targetEntity);

        // Apply predictive targeting if enabled
        if (LockOnConfig.isPredictiveTargeting()) {
            Vec3 prediction = targetEntity.getDeltaMovement().scale(LockOnConfig.getPredictionStrength());
            targetPos = targetPos.add(prediction);
        }

        // Calculate direction vector
        Vec3 directionVec = targetPos.subtract(playerPos).normalize();

        // Convert to rotation
        double horizontalDistance = Math.sqrt(directionVec.x * directionVec.x + directionVec.z * directionVec.z);
        float targetYaw = (float) (Math.atan2(directionVec.z, directionVec.x) * 180.0 / Math.PI) - 90.0F;
        float targetPitch = (float) -(Math.atan2(directionVec.y, horizontalDistance) * 180.0 / Math.PI);

        // Get current rotations
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // Calculate yaw difference (shortest path)
        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;

        // Initialize smoothing values
        if (!wasLocked) {
            prevYaw = currentYaw;
            prevPitch = currentPitch;
            wasLocked = true;
        }

        // Calculate adaptive rotation speed
        float distance = player.distanceTo(targetEntity);
        float angleDifference = Math.abs(yawDiff) + Math.abs(targetPitch - currentPitch);

        float baseSpeed = LockOnConfig.getRotationSpeed();
        float adaptiveSpeed = calculateAdaptiveSpeed(baseSpeed, distance, angleDifference);

        // Apply smoothing if enabled
        float newYaw, newPitch;
        if (LockOnConfig.isSmoothCameraEnabled()) {
            float smoothingFactor = LockOnConfig.getSmoothingFactor();
            newYaw = interpolateRotation(prevYaw, currentYaw + yawDiff * adaptiveSpeed, smoothingFactor);
            newPitch = interpolateRotation(prevPitch, currentPitch + (targetPitch - currentPitch) * adaptiveSpeed, smoothingFactor);
        } else {
            newYaw = currentYaw + yawDiff * adaptiveSpeed;
            newPitch = currentPitch + (targetPitch - currentPitch) * adaptiveSpeed;
        }

        // Update previous values
        prevYaw = newYaw;
        prevPitch = newPitch;

        // Apply rotations
        player.setYRot(newYaw);
        player.setXRot(newPitch);
    }

    /**
     * Renders the enhanced lock-on indicator
     */
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        if (targetEntity != null && targetEntity.isAlive() && indicatorVisible) {
            LockOnRenderer.renderLockOnIndicator(event, targetEntity);
        }
    }

    /**
     * Handles hold-to-maintain lock mode
     */
    private static void handleHoldMode(LocalPlayer player, boolean keyHeld) {
        if (keyHeld && !wasKeyHeld) {
            // Key just pressed
            if (targetEntity == null) {
                findTarget(player);
            }
        } else if (!keyHeld && wasKeyHeld) {
            // Key just released
            clearTarget();
        }
    }

    /**
     * Handles toggle lock mode
     */
    private static void handleToggleMode(LocalPlayer player) {
        if (targetEntity == null) {
            findTarget(player);
        } else {
            clearTarget();
        }
    }

    /**
     * Enhanced target finding with multiple modes and filters
     */
    private static void findTarget(LocalPlayer player) {
        if (player.level == null) return;

        // Get potential targets within search radius
        AABB searchBox = player.getBoundingBox().inflate(LockOnConfig.getSearchRadius());
        List<Entity> nearbyEntities = player.level.getEntities(player, searchBox);

        // Filter entities based on configuration
        potentialTargets = nearbyEntities.stream()
                .filter(entity -> isValidTarget(entity, player))
                .filter(entity -> passesEntityFilters(entity))
                .filter(entity -> passesLineOfSightCheck(player, entity))
                .filter(entity -> passesAngleCheck(player, entity))
                .limit(LockOnConfig.getMaxTargetsToSearch())
                .collect(Collectors.toList());

        if (potentialTargets.isEmpty()) {
            showMessage(player, "No Valid Targets Found");
            return;
        }

        // Sort based on targeting mode
        sortTargetsByMode(player, potentialTargets);

        // Select the best target
        targetEntity = potentialTargets.get(0);
        currentTargetIndex = 0;
        wasLocked = true;

        // Show target acquired message
        showMessage(player, "Target Locked: " + targetEntity.getDisplayName().getString());

        // Play lock-on sound
        playSound(player, "lock_on");
    }

    /**
     * Cycles through available targets
     */
    private static void cycleTarget(LocalPlayer player, boolean reverse) {
        if (potentialTargets.isEmpty()) {
            findTarget(player);
            return;
        }

        // Update potential targets list
        potentialTargets = potentialTargets.stream()
                .filter(entity -> isValidTarget(entity, player))
                .collect(Collectors.toList());

        if (potentialTargets.size() <= 1) return;

        // Find current target index
        currentTargetIndex = potentialTargets.indexOf(targetEntity);
        if (currentTargetIndex == -1) {
            currentTargetIndex = 0;
        } else {
            // Move to next/previous target
            if (reverse || LockOnConfig.reverseScrollCycling()) {
                currentTargetIndex = (currentTargetIndex - 1 + potentialTargets.size()) % potentialTargets.size();
            } else {
                currentTargetIndex = (currentTargetIndex + 1) % potentialTargets.size();
            }
        }

        targetEntity = potentialTargets.get(currentTargetIndex);

        // Reset smoothing values
        prevYaw = player.getYRot();
        prevPitch = player.getXRot();

        // Show target switch message
        showMessage(player, "Target: " + targetEntity.getDisplayName().getString());

        // Play target switch sound
        playSound(player, "target_switch");
    }

    /**
     * Validates if an entity can be targeted
     */
    private static boolean isValidTarget(Entity entity, LocalPlayer player) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive()) return false;
        if (entity == player) return false;
        if (entity.distanceTo(player) > LockOnConfig.getMaxLockOnDistance()) return false;
        if (entity.level != player.level) return false;

        LivingEntity living = (LivingEntity) entity;

        // Health checks
        float minHealth = LockOnConfig.getMinTargetHealth();
        float maxHealth = LockOnConfig.getMaxTargetHealth();

        if (living.getHealth() < minHealth) return false;
        if (maxHealth > 0 && living.getHealth() > maxHealth) return false;

        return true;
    }

    /**
     * Checks entity type filters
     */
    private static boolean passesEntityFilters(Entity entity) {
        // Check whitelist/blacklist
        ResourceLocation entityType = EntityType.getKey(entity.getType());
        String entityTypeString = entityType.toString();

        if (LockOnConfig.useWhitelist()) {
            if (!LockOnConfig.getEntityWhitelist().contains(entityTypeString)) return false;
        } else {
            if (LockOnConfig.getEntityBlacklist().contains(entityTypeString)) return false;
        }

        // Type-based filters
        if (entity instanceof Player && !LockOnConfig.canTargetPlayers()) return false;
        if (entity instanceof Monster && !LockOnConfig.canTargetHostileMobs()) return false;
        if (entity instanceof Animal && !LockOnConfig.canTargetAnimals()) return false;
        if (entity instanceof AbstractVillager && !LockOnConfig.canTargetVillagers()) return false;

        // Boss check
        if ((entity instanceof WitherBoss || entity instanceof EnderDragon) && !LockOnConfig.canTargetBosses()) return false;

        // Mob category checks
        MobCategory category = entity.getType().getCategory();
        switch (category) {
            case MONSTER:
                return LockOnConfig.canTargetHostileMobs();
            case CREATURE:
                return LockOnConfig.canTargetPassiveMobs();
            case AMBIENT:
                return LockOnConfig.canTargetPassiveMobs();
            case WATER_CREATURE:
                return LockOnConfig.canTargetPassiveMobs();
            case WATER_AMBIENT:
                return LockOnConfig.canTargetPassiveMobs();
            case MISC:
                return LockOnConfig.canTargetNeutralMobs();
            default:
                return true;
        }
    }

    /**
     * Checks line of sight if required
     */
    private static boolean passesLineOfSightCheck(LocalPlayer player, Entity entity) {
        if (!LockOnConfig.requireLineOfSight()) return true;
        return hasLineOfSight(player, entity);
    }

    /**
     * Checks if entity is within targeting angle
     */
    private static boolean passesAngleCheck(LocalPlayer player, Entity entity) {
        Vec3 playerLook = player.getViewVector(1.0F).normalize();
        Vec3 directionToEntity = entity.position().subtract(player.getEyePosition()).normalize();

        double dotProduct = playerLook.dot(directionToEntity);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct))) * 180.0 / Math.PI;

        return angle <= LockOnConfig.getTargetingAngle();
    }

    /**
     * Performs line of sight check with glass penetration option
     */
    private static boolean hasLineOfSight(LocalPlayer player, Entity entity) {
        Vec3 start = player.getEyePosition();
        Vec3 end = getTargetPosition(entity);

        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult result = player.level.clip(context);

        if (result.getType() == HitResult.Type.MISS) return true;

        // Check glass penetration
        if (LockOnConfig.canPenetrateGlass()) {
            BlockState hitBlock = player.level.getBlockState(result.getBlockPos());
            return hitBlock.is(Blocks.GLASS) || hitBlock.is(Blocks.GLASS_PANE) ||
                    hitBlock.getBlock().toString().toLowerCase().contains("glass");
        }

        return false;
    }

    /**
     * Sorts targets based on the selected targeting mode (with runtime override)
     */
    private static void sortTargetsByMode(LocalPlayer player, List<Entity> targets) {
        LockOnConfig.TargetingMode mode = runtimeTargetingMode != null ? runtimeTargetingMode : LockOnConfig.getTargetingMode();

        switch (mode) {
            case CLOSEST:
                targets.sort(Comparator.comparingDouble(entity -> entity.distanceTo(player)));
                break;

            case MOST_DAMAGED:
                targets.sort((e1, e2) -> {
                    if (e1 instanceof LivingEntity && e2 instanceof LivingEntity) {
                        LivingEntity l1 = (LivingEntity) e1;
                        LivingEntity l2 = (LivingEntity) e2;
                        float health1 = l1.getHealth() / l1.getMaxHealth();
                        float health2 = l2.getHealth() / l2.getMaxHealth();
                        return Float.compare(health1, health2);
                    }
                    return 0;
                });
                break;

            case CROSSHAIR_CENTERED:
                Vec3 playerLook = player.getViewVector(1.0F).normalize();
                targets.sort(Comparator.comparingDouble(entity -> {
                    Vec3 directionToEntity = entity.position().subtract(player.getEyePosition()).normalize();
                    return -playerLook.dot(directionToEntity); // Negative for ascending order
                }));
                break;

            case THREAT_LEVEL:
                targets.sort((e1, e2) -> {
                    int threat1 = calculateThreatLevel(e1);
                    int threat2 = calculateThreatLevel(e2);
                    return Integer.compare(threat2, threat1); // Descending order
                });
                break;
        }

        // Apply smart targeting if enabled
        if (LockOnConfig.isSmartTargeting()) {
            applySmartTargeting(player, targets);
        }
    }

    /**
     * Applies smart targeting algorithm using multiple factors
     */
    private static void applySmartTargeting(LocalPlayer player, List<Entity> targets) {
        Vec3 playerLook = player.getViewVector(1.0F).normalize();

        targets.sort(Comparator.comparingDouble(entity -> {
            double distance = entity.distanceTo(player);
            double normalizedDistance = Math.min(1.0, distance / LockOnConfig.getMaxLockOnDistance());

            Vec3 directionToEntity = entity.position().subtract(player.getEyePosition()).normalize();
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, playerLook.dot(directionToEntity))));
            double normalizedAngle = angle / Math.PI;

            double healthFactor = 1.0;
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                healthFactor = 1.0 - (living.getHealth() / living.getMaxHealth());
            }

            // Weighted score (lower is better)
            return (normalizedDistance * LockOnConfig.getDistancePriorityWeight()) +
                    (normalizedAngle * LockOnConfig.getAnglePriorityWeight()) +
                    (healthFactor * LockOnConfig.getHealthPriorityWeight());
        }));
    }

    /**
     * Calculates threat level for threat-based targeting
     */
    private static int calculateThreatLevel(Entity entity) {
        if (entity instanceof WitherBoss || entity instanceof EnderDragon) return 100;
        if (entity instanceof Monster) return 50;
        if (entity instanceof Player) return 30;
        if (entity instanceof Animal) return 10;
        return 20;
    }

    /**
     * Gets the target position with configurable offset
     */
    private static Vec3 getTargetPosition(Entity entity) {
        float offset = LockOnConfig.getCameraOffset();
        return entity.position().add(0, entity.getBbHeight() * offset, 0);
    }

    /**
     * Calculates adaptive rotation speed
     */
    private static float calculateAdaptiveSpeed(float baseSpeed, float distance, float angleDifference) {
        float distanceWeight = LockOnConfig.getDistanceWeight();
        float distanceFactor = 1.0F + (distanceWeight * (distance / LockOnConfig.getMaxLockOnDistance()));
        float angleFactor = 1.0F + (angleDifference / 180.0F);

        float adaptiveSpeed = baseSpeed * distanceFactor * angleFactor;

        return Math.max(LockOnConfig.getMinRotationSpeed(),
                Math.min(LockOnConfig.getMaxRotationSpeed(), adaptiveSpeed));
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
     * Checks if lock-on should be disabled for current game mode
     */
    private static boolean shouldDisableForGameMode(LocalPlayer player) {
        if (player.isCreative() && LockOnConfig.disableInCreative()) return true;
        if (player.isSpectator() && LockOnConfig.disableInSpectator()) return true;
        return false;
    }

    /**
     * Sets the runtime targeting mode (overrides config)
     */
    private static void setRuntimeTargetingMode(LockOnConfig.TargetingMode mode) {
        runtimeTargetingMode = mode;
    }

    /**
     * Shows a message to the player
     */
    private static void showMessage(LocalPlayer player, String message) {
        if (player != null) {
            player.displayClientMessage(Component.literal(message), true);
        }
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
        wasLocked = false;
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
    }

    /**
     * Plays sounds based on configuration
     */
    private static void playSound(LocalPlayer player, String soundType) {
        if (!LockOnConfig.areSoundsEnabled()) return;

        boolean shouldPlay = false;
        switch (soundType) {
            case "lock_on":
                shouldPlay = LockOnConfig.playLockOnSound();
                break;
            case "target_switch":
                shouldPlay = LockOnConfig.playTargetSwitchSound();
                break;
            case "target_lost":
                shouldPlay = LockOnConfig.playTargetLostSound();
                break;
        }

        if (shouldPlay && player.level != null) {
            float volume = LockOnConfig.getSoundVolume();
            player.level.playSound(player, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK,
                    SoundSource.PLAYERS, volume, 1.0F);
        }
    }

    /**
     * Returns the currently targeted entity
     */
    public static Entity getTargetEntity() {
        return targetEntity;
    }

    /**
     * Returns the list of potential targets
     */
    public static List<Entity> getPotentialTargets() {
        return new ArrayList<>(potentialTargets);
    }

    /**
     * Returns whether a target is currently locked
     */
    public static boolean hasTarget() {
        return targetEntity != null && targetEntity.isAlive();
    }

    /**
     * Returns whether the indicator is currently visible
     */
    public static boolean isIndicatorVisible() {
        return indicatorVisible;
    }

    /**
     * Sets indicator visibility
     */
    public static void setIndicatorVisible(boolean visible) {
        indicatorVisible = visible;
    }

    /**
     * Gets the current runtime targeting mode
     */
    public static LockOnConfig.TargetingMode getRuntimeTargetingMode() {
        return runtimeTargetingMode != null ? runtimeTargetingMode : LockOnConfig.getTargetingMode();
    }
}