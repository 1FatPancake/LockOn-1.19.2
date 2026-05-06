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

    // === CORE STATE ===
    private static Entity targetEntity = null;
    private static List<Entity> potentialTargets = new ArrayList<>();
    private static int currentTargetIndex = -1;
    private static boolean wasKeyHeld = false;
    private static boolean indicatorVisible = true;
    private static boolean wasLocked = false;
    private static LockOnConfig.TargetingMode runtimeTargetingMode = null;

    // === THIRD PERSON STATE ===
    private static Vector3d lastCameraOffset = Vector3d.ZERO;
    private static boolean lastThirdPersonState = false;
    private static long lastUpdateTime = 0;

    // =========================================================
    //  PUBLIC ACCESSORS
    // =========================================================

    public static Entity getTargetEntity()           { return targetEntity; }
    public static boolean hasTarget()                { return targetEntity != null && targetEntity.isAlive(); }
    public static List<Entity> getPotentialTargets() { return new ArrayList<>(potentialTargets); }
    public static boolean wasLocked()                { return wasLocked; }
    public static boolean isActive()                 { return targetEntity != null && targetEntity.isAlive(); }
    public static boolean isIndicatorVisible()       { return indicatorVisible; }

    public static String getThirdPersonCompatibilityStatus() {
        return ThirdPersonCompatibility.getCompatibilityStatus();
    }

    public static double getCurrentTargetDistance() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        return (player != null && targetEntity != null) ? player.distanceTo(targetEntity) : 0.0;
    }

    public static float getCurrentTargetHealthPercent() {
        if (targetEntity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) targetEntity;
            return living.getHealth() / living.getMaxHealth();
        }
        return 0f;
    }

    // =========================================================
    //  MAIN TICK
    // =========================================================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < (LockOnConfig.getUpdateFrequency() * 50)) return;
        lastUpdateTime = currentTime;

        // Third person state change detection
        boolean currentThirdPersonState = ThirdPersonCompatibility.isThirdPersonActive();
        if (currentThirdPersonState != lastThirdPersonState) {
            lastThirdPersonState = currentThirdPersonState;
            if (targetEntity != null && !isValidTarget(targetEntity, player))
                clearTarget();
        }
        if (currentThirdPersonState)
            lastCameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();

        if (shouldDisableForGameMode(player)) {
            clearTarget();
            return;
        }

        handleInput(player);
        updateTargeting(player);
        updateCameraRotation(player);
    }

    // =========================================================
    //  INPUT
    // =========================================================

    private static void handleInput(ClientPlayerEntity player) {
        boolean lockKeyPressed        = LockOnKeybinds.lockOnKey.isDown();
        boolean lockKeyClicked        = LockOnKeybinds.lockOnKey.consumeClick();
        boolean cycleKeyClicked       = LockOnKeybinds.cycleTargetKey.consumeClick();
        boolean reverseCycleKeyClicked= LockOnKeybinds.cycleTargetReverseKey.consumeClick();
        boolean clearKeyClicked       = LockOnKeybinds.clearTargetKey.consumeClick();

        if (LockOnConfig.isToggleMode()) {
            if (lockKeyClicked) {
                if (targetEntity == null) findAndLockTarget(player);
                else clearTarget();
            }
        } else if (LockOnConfig.holdToMaintainLock()) {
            if (lockKeyPressed && !wasKeyHeld) findAndLockTarget(player);
            else if (!lockKeyPressed && wasKeyHeld) clearTarget();
        } else {
            if (lockKeyClicked) findAndLockTarget(player);
        }

        if (cycleKeyClicked && LockOnConfig.canCycleThroughTargets())
            cycleTarget(player, false);
        if (reverseCycleKeyClicked && LockOnConfig.canCycleThroughTargets())
            cycleTarget(player, true);
        if (clearKeyClicked)
            clearTarget();

        handleTargetingModeShortcuts(player);
        handleVisualControls(player);

        wasKeyHeld = lockKeyPressed;
    }

    // =========================================================
    //  TARGETING
    // =========================================================

    private static void findAndLockTarget(ClientPlayerEntity player) {
        List<Entity> targets = findValidTargets(player);

        if (targets.isEmpty()) {
            showMessage(player, "No targets found");
            playSound(player, "target_lost");
            return;
        }

        if (ThirdPersonCompatibility.isThirdPersonActive())
            targets = adjustTargetsForThirdPerson(targets, player);

        Entity newTarget = selectBestTarget(targets, player);
        if (newTarget != null) {
            setTarget(newTarget);
            potentialTargets = targets;
            currentTargetIndex = targets.indexOf(newTarget);
            showMessage(player, "Locked: " + newTarget.getDisplayName().getString());
            playSound(player, "lock_on");
        }
    }

    private static List<Entity> findValidTargets(ClientPlayerEntity player) {
        double baseRange = LockOnConfig.getMaxLockOnDistance();
        double range = ThirdPersonCompatibility.isThirdPersonActive()
                ? ThirdPersonCompatibility.getAdjustedTargetingRange(baseRange) : baseRange;
        double searchRadius = Math.min(range * 1.2, LockOnConfig.getSearchRadius());

        AxisAlignedBB searchBox = player.getBoundingBox().inflate(searchRadius);
        return player.level.getEntities(player, searchBox).stream()
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> e != player)
                .filter(e -> player.distanceToSqr(e) <= range * range)
                .filter(e -> isValidTarget(e, player))
                .filter(e -> hasLineOfSight(player, e))
                .filter(e -> isWithinTargetingAngle(player, e))
                .limit(LockOnConfig.getMaxTargetsToSearch())
                .collect(Collectors.toList());
    }

    private static boolean isValidTarget(Entity entity, ClientPlayerEntity player) {
        if (!(entity instanceof LivingEntity)) return false;
        LivingEntity living = (LivingEntity) entity;
        if (entity == player || !entity.isAlive()) return false;

        float health = living.getHealth();
        if (health < LockOnConfig.getMinTargetHealth()) return false;
        if (LockOnConfig.getMaxTargetHealth() > 0 && health > LockOnConfig.getMaxTargetHealth()) return false;

        if (entity instanceof PlayerEntity    && !LockOnConfig.canTargetPlayers())     return false;
        if (entity instanceof MonsterEntity   && !LockOnConfig.canTargetHostileMobs()) return false;
        if (entity instanceof AnimalEntity    && !LockOnConfig.canTargetPassiveMobs()) return false;
        if ((entity instanceof WitherEntity || entity instanceof EnderDragonEntity)
                && !LockOnConfig.canTargetBosses())      return false;

        ResourceLocation entityId = entity.getType().getRegistryName();
        if (entityId == null) return false;
        String id = entityId.toString();

        if (LockOnConfig.useWhitelist())
            return LockOnConfig.getEntityWhitelist().contains(id);
        else
            return !LockOnConfig.getEntityBlacklist().contains(id);
    }

    private static boolean hasLineOfSight(ClientPlayerEntity player, Entity target) {
        if (!LockOnConfig.requireLineOfSight()) return true;

        Vector3d start = player.getEyePosition(1f);
        Vector3d end = target.getEyePosition(1f);

        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vector3d offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            start = start.add(offset.scale(0.2));
        }

        BlockRayTraceResult result = player.level.clip(new RayTraceContext(
                start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));

        if (result.getType() == RayTraceResult.Type.BLOCK) {
            BlockState state = player.level.getBlockState(result.getBlockPos());
            if (LockOnConfig.penetrateGlass() &&
                    (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                            || state.getBlock().toString().contains("glass")))
                return true;
            return false;
        }
        return true;
    }

    private static boolean isWithinTargetingAngle(ClientPlayerEntity player, Entity target) {
        double maxAngle = LockOnConfig.getTargetingAngle();
        if (ThirdPersonCompatibility.isThirdPersonActive())
            maxAngle = ThirdPersonCompatibility.getAdjustedTargetingAngle(maxAngle);

        Vector3d playerPos = player.getEyePosition(1f);
        Vector3d targetPos = target.getEyePosition(1f);
        Vector3d look = player.getLookAngle();

        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vector3d offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            playerPos = playerPos.add(offset.scale(0.5));
        }

        Vector3d toTarget = targetPos.subtract(playerPos).normalize();
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, look.dot(toTarget)))) * 180.0 / Math.PI;
        return angle <= maxAngle;
    }

    private static Entity selectBestTarget(List<Entity> targets, ClientPlayerEntity player) {
        if (targets.isEmpty()) return null;
        LockOnConfig.TargetingMode mode = runtimeTargetingMode != null
                ? runtimeTargetingMode : LockOnConfig.getTargetingMode();

        switch (mode) {
            case CLOSEST:
                return targets.stream()
                        .min(Comparator.comparing(e -> player.distanceToSqr(e))).orElse(null);
            case MOST_DAMAGED:
                return targets.stream()
                        .filter(e -> e instanceof LivingEntity)
                        .min(Comparator.comparing(e -> ((LivingEntity) e).getHealth())).orElse(null);
            case CROSSHAIR_CENTERED:
                return targets.stream()
                        .min(Comparator.comparing(e -> crosshairDistance(player, e))).orElse(null);
            case THREAT_LEVEL:
                return targets.stream()
                        .max(Comparator.comparing(e -> threatLevel(e))).orElse(null);
            case SMART:
                return targets.stream()
                        .min(Comparator.comparing(e -> smartScore(player, e))).orElse(null);
            default:
                return targets.get(0);
        }
    }

    private static List<Entity> adjustTargetsForThirdPerson(List<Entity> targets, ClientPlayerEntity player) {
        Vector3d offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
        return targets.stream()
                .filter(e -> {
                    Vector3d adjustedCam = player.position().add(offset);
                    Vector3d toEntity = e.position().subtract(adjustedCam);
                    return toEntity.dot(player.getLookAngle()) > -0.5;
                })
                .collect(Collectors.toList());
    }

    private static void cycleTarget(ClientPlayerEntity player, boolean reverse) {
        if (potentialTargets.isEmpty()) { findAndLockTarget(player); return; }

        potentialTargets = potentialTargets.stream()
                .filter(e -> e.isAlive() && isValidTarget(e, player))
                .collect(Collectors.toList());

        if (potentialTargets.isEmpty()) { clearTarget(); return; }

        int dir = reverse ? -1 : 1;
        if (LockOnConfig.reverseScrollCycling()) dir *= -1;

        currentTargetIndex = (currentTargetIndex + dir + potentialTargets.size()) % potentialTargets.size();
        Entity newTarget = potentialTargets.get(currentTargetIndex);
        setTarget(newTarget);
        showMessage(player, "Target: " + newTarget.getDisplayName().getString());
        playSound(player, "target_switch");
    }

    // =========================================================
    //  CAMERA ROTATION
    // =========================================================

    private static void updateCameraRotation(ClientPlayerEntity player) {
        if (targetEntity == null || !targetEntity.isAlive()) {
            if (targetEntity != null) { onTargetLost(); clearTarget(); }
            return;
        }

        if (!LockOnConfig.isSmoothCameraEnabled()) {
            snapCameraToTarget(player);
            return;
        }

        Vector3d playerEyePos = player.getEyePosition(1f);
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vector3d offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            playerEyePos = playerEyePos.add(offset.scale(0.3));
        }

        Vector3d targetPos = targetEntity.getEyePosition(1f);
        if (ThirdPersonCompatibility.isThirdPersonActive())
            targetPos = ThirdPersonCompatibility.getAdjustedTargetPosition(targetEntity, targetPos);

        Vector3d direction = targetPos.subtract(playerEyePos).normalize();
        float targetYaw   = (float)(Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float targetPitch = (float)(Math.asin(-direction.y) * 180.0 / Math.PI);

        float speed = LockOnConfig.getRotationSpeed();
        if (ThirdPersonCompatibility.isThirdPersonActive())
            speed = ThirdPersonCompatibility.getAdjustedRotationSpeed(speed);

        float smoothing = Math.min(speed * 8f, 0.8f);
        if (ThirdPersonCompatibility.shouldUseEnhancedSmoothing())
            smoothing = ThirdPersonCompatibility.getThirdPersonSmoothingFactor(smoothing);

        player.yRot = interpolateAngle(player.yRot, targetYaw, smoothing);
        player.xRot = Math.max(-90f, Math.min(90f, interpolateAngle(player.xRot, targetPitch, smoothing)));
    }

    private static void snapCameraToTarget(ClientPlayerEntity player) {
        Vector3d playerEyePos = player.getEyePosition(1f);
        Vector3d targetPos = targetEntity.getEyePosition(1f);

        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vector3d offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            playerEyePos = playerEyePos.add(offset.scale(0.3));
            targetPos = ThirdPersonCompatibility.getAdjustedTargetPosition(targetEntity, targetPos);
        }

        Vector3d dir = targetPos.subtract(playerEyePos).normalize();
        player.yRot = (float)(Math.atan2(-dir.x, dir.z) * 180.0 / Math.PI);
        player.xRot = Math.max(-90f, Math.min(90f, (float)(Math.asin(-dir.y) * 180.0 / Math.PI)));
    }

    private static float interpolateAngle(float from, float to, float factor) {
        float diff = to - from;
        while (diff > 180)  diff -= 360;
        while (diff < -180) diff += 360;
        return from + diff * factor;
    }

    // =========================================================
    //  TARGET MANAGEMENT
    // =========================================================

    private static void setTarget(Entity target) {
        targetEntity = target;
        wasLocked = true;
    }

    public static void clearTarget() {
        boolean hadTarget = targetEntity != null;
        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = -1;
        wasLocked = false;
        runtimeTargetingMode = null;
        if (hadTarget) {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            playSound(player, "target_lost");
        }
    }

    private static void onTargetLost() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        playSound(player, "target_lost");
        wasLocked = false;
    }

    private static void updateTargeting(ClientPlayerEntity player) {
        if (targetEntity == null) return;

        if (!targetEntity.isAlive() || !isValidTarget(targetEntity, player)) {
            clearTarget();
            return;
        }

        if (LockOnConfig.isAutoBreakOnObstructionEnabled() && !hasLineOfSight(player, targetEntity)) {
            clearTarget();
            return;
        }

        double distance = player.distanceTo(targetEntity);
        double maxRange = LockOnConfig.getMaxLockOnDistance();
        if (ThirdPersonCompatibility.isThirdPersonActive())
            maxRange = ThirdPersonCompatibility.getAdjustedTargetingRange(maxRange);

        if (distance > maxRange) clearTarget();
    }

    // =========================================================
    //  MISC INPUT HANDLERS
    // =========================================================

    private static void handleTargetingModeShortcuts(ClientPlayerEntity player) {
        if (LockOnKeybinds.targetClosestKey.consumeClick()) {
            runtimeTargetingMode = LockOnConfig.TargetingMode.CLOSEST;
            showMessage(player, "Mode: Closest");
            playSound(player, "target_switch");
        }
        if (LockOnKeybinds.targetMostDamagedKey.consumeClick()) {
            runtimeTargetingMode = LockOnConfig.TargetingMode.MOST_DAMAGED;
            showMessage(player, "Mode: Most Damaged");
            playSound(player, "target_switch");
        }
        if (LockOnKeybinds.targetThreatKey.consumeClick()) {
            runtimeTargetingMode = LockOnConfig.TargetingMode.THREAT_LEVEL;
            showMessage(player, "Mode: Threat Level");
            playSound(player, "target_switch");
        }
    }

    private static void handleVisualControls(ClientPlayerEntity player) {
        if (LockOnKeybinds.toggleIndicatorKey.consumeClick()) {
            indicatorVisible = !indicatorVisible;
            showMessage(player, "Indicator: " + (indicatorVisible ? "On" : "Off"));
            playSound(player, "target_switch");
        }
        if (LockOnKeybinds.cycleIndicatorTypeKey.consumeClick()) {
            LockOnHudRenderer.cycleCrosshair();
            showMessage(player, "Crosshair: " + LockOnHudRenderer.getCurrentCrosshairInfo());
            playSound(player, "target_switch");
        }
    }

    // =========================================================
    //  RENDER
    // =========================================================

    @SubscribeEvent
    public static void onRenderLevel(RenderWorldLastEvent event) {
        if (targetEntity == null || !indicatorVisible) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        float indicatorSize = LockOnConfig.getIndicatorSize();
        if (ThirdPersonCompatibility.isThirdPersonActive())
            indicatorSize = ThirdPersonCompatibility.getAdjustedIndicatorSize(indicatorSize);

        Vector3d targetPos = targetEntity.getEyePosition(1f);
        if (ThirdPersonCompatibility.isThirdPersonActive())
            targetPos = ThirdPersonCompatibility.getAdjustedTargetPosition(targetEntity, targetPos);

        LockOnRenderer.renderLockOnIndicator(
                event.getMatrixStack(),
                targetEntity,
                targetPos,
                indicatorSize,
                LockOnConfig.getIndicatorType(),
                ThirdPersonCompatibility.isThirdPersonActive()
        );
    }

    // =========================================================
    //  SOUNDS & MESSAGES
    // =========================================================

    private static void showMessage(ClientPlayerEntity player, String message) {
        if (player != null)
            player.displayClientMessage(new StringTextComponent(message), true);
    }

    private static void playSound(ClientPlayerEntity player, String type) {
        if (!LockOnConfig.areSoundsEnabled() || player == null) return;
        float vol = LockOnConfig.getSoundVolume();
        switch (type) {
            case "lock_on":
                if (LockOnConfig.playLockOnSound())
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, vol, 1.2f, false);
                break;
            case "target_switch":
                if (LockOnConfig.playTargetSwitchSound())
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, vol, 1.5f, false);
                break;
            case "target_lost":
                if (LockOnConfig.playTargetLostSound())
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundCategory.PLAYERS, vol, 0.8f, false);
                break;
        }
    }

    // =========================================================
    //  UTILITIES
    // =========================================================

    private static boolean shouldDisableForGameMode(ClientPlayerEntity player) {
        if (player.isCreative()  && LockOnConfig.disableInCreative())  return true;
        if (player.isSpectator() && LockOnConfig.disableInSpectator()) return true;
        return false;
    }

    private static double crosshairDistance(ClientPlayerEntity player, Entity entity) {
        Vector3d look = player.getLookAngle();
        Vector3d toEntity = entity.getEyePosition(1f).subtract(player.getEyePosition(1f)).normalize();
        return Math.acos(Math.max(-1.0, Math.min(1.0, look.dot(toEntity))));
    }

    private static double threatLevel(Entity entity) {
        if (entity instanceof MonsterEntity) return 3.0;
        if (entity instanceof PlayerEntity)  return 2.0;
        if (entity instanceof AnimalEntity)  return 1.0;
        return 0.0;
    }

    private static double smartScore(ClientPlayerEntity player, Entity entity) {
        if (!(entity instanceof LivingEntity)) return Double.MAX_VALUE;
        LivingEntity living = (LivingEntity) entity;
        double distance  = player.distanceToSqr(entity);
        double angle     = crosshairDistance(player, entity);
        double healthPct = living.getHealth() / living.getMaxHealth();
        double threat    = threatLevel(entity);
        return distance * LockOnConfig.getDistancePriorityWeight()
                + angle   * LockOnConfig.getAnglePriorityWeight()
                + (1.0 - healthPct) * LockOnConfig.getHealthPriorityWeight()
                + (4.0 - threat)    * 0.1;
    }

    // =========================================================
    //  PUBLIC UTILITY
    // =========================================================

    public static boolean setTargetEntity(Entity entity) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && entity != null && isValidTarget(entity, player)) {
            setTarget(entity);
            showMessage(player, "Locked: " + entity.getDisplayName().getString());
            playSound(player, "lock_on");
            return true;
        }
        return false;
    }

    public static void toggleIndicatorVisibility() {
        indicatorVisible = !indicatorVisible;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        showMessage(player, "Indicator: " + (indicatorVisible ? "On" : "Off"));
    }

    public static void refreshTargets() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null) potentialTargets = findValidTargets(player);
    }

    public static String getDebugInfo() {
        return "Target: " + (targetEntity != null ? targetEntity.getDisplayName().getString() : "None")
                + " | Targets: " + potentialTargets.size()
                + " | ThirdPerson: " + ThirdPersonCompatibility.isThirdPersonActive()
                + " | Mode: " + (runtimeTargetingMode != null ? runtimeTargetingMode : "Config");
    }

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
        LockOnMod.LOGGER.debug("Lock-On emergency reset complete");
    }
}