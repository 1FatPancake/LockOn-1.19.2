package net.leolifeless.lockonmod;

import com.mojang.logging.LogUtils;
import net.leolifeless.lockonmod.compat.ShoulderSurfingCompat;
import net.leolifeless.lockonmod.compat.ThirdPersonCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LockOnSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    // === CORE STATE ===
    private static Entity targetEntity = null;
    private static List<Entity> potentialTargets = new ArrayList<>();
    private static int currentTargetIndex = 0;
    private static boolean wasKeyHeld = false;
    private static boolean indicatorVisible = true;
    private static boolean wasLocked = false;
    private static LockOnConfig.TargetingMode runtimeTargetingMode = null;

    // === CACHES ===
    private static final Map<Entity, Long>    entityValidationCache = new HashMap<>();
    private static final Map<Entity, Boolean> lineOfSightCache      = new HashMap<>();
    private static final Map<Entity, Vec3>    entityPositionCache   = new HashMap<>();
    private static final long CACHE_VALIDATION_MS = 100;

    // === TIMING ===
    private static long lastTargetingUpdate = 0;
    private static long lastCacheCleanup    = 0;
    private static long lastThirdPersonCheck = 0;
    private static final int TARGETING_UPDATE_INTERVAL = 3;

    // === CAMERA STATE ===
    private static float prevYaw   = 0f;
    private static float prevPitch = 0f;

    // === THIRD PERSON STATE ===
    private static Vec3    lastCameraOffset    = Vec3.ZERO;
    private static boolean lastThirdPersonState = false;

    // === SYNC PROTECTION ===
    private static int  lagTimeout       = 0;
    private static final int MAX_LAG_TIMEOUT = 100;
    private static int  syncCheckCounter = 0;
    private static boolean wasNetworkLagging = false;

    // =========================================================
    //  PUBLIC ACCESSORS
    // =========================================================

    public static Entity  getTargetEntity()           { return targetEntity; }
    public static boolean hasTarget()                 { return targetEntity != null && targetEntity.isAlive(); }
    public static List<Entity> getPotentialTargets()  { return new ArrayList<>(potentialTargets); }
    public static boolean wasLocked()                 { return wasLocked; }
    public static boolean isActive()                  { return targetEntity != null && targetEntity.isAlive(); }
    public static boolean isIndicatorVisible()        { return indicatorVisible; }

    public static double getCurrentTargetDistance() {
        LocalPlayer player = Minecraft.getInstance().player;
        return (player != null && targetEntity != null) ? player.distanceTo(targetEntity) : 0.0;
    }

    public static float getCurrentTargetHealthPercent() {
        if (targetEntity instanceof LivingEntity living)
            return living.getHealth() / living.getMaxHealth();
        return 0f;
    }

    public static String getThirdPersonCompatibilityStatus() {
        return ThirdPersonCompatibility.getCompatibilityStatus();
    }

    // =========================================================
    //  MAIN TICK
    // =========================================================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        long currentTick = mc.level.getGameTime();
        long currentTime = System.currentTimeMillis();

        // Sync check every 3 seconds
        syncCheckCounter++;
        if (syncCheckCounter >= 60) {
            syncCheckCounter = 0;
            if (targetEntity != null && !isTargetStillValid(targetEntity, player))
                clearTargetSilently();
        }

        // Third person state change detection
        if (currentTime - lastThirdPersonCheck > 500) {
            boolean current = ThirdPersonCompatibility.isThirdPersonActive();
            if (current != lastThirdPersonState) {
                lastThirdPersonState = current;
                if (targetEntity != null && !isValidTarget(targetEntity, player))
                    clearTargetSilently();
            }
            if (current) lastCameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            lastThirdPersonCheck = currentTime;
        }

        if (shouldDisableForGameMode(player)) { clearTarget(); return; }

        handleInput(player);

        if (currentTick - lastTargetingUpdate >= TARGETING_UPDATE_INTERVAL) {
            updateTargetingWithSync(player, currentTick);
            lastTargetingUpdate = currentTick;
        } else {
            updateCameraRotation(player);
        }

        if (currentTime - lastCacheCleanup > 5000) {
            cleanCaches();
            lastCacheCleanup = currentTime;
        }
    }

    // =========================================================
    //  RENDER
    // =========================================================

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (targetEntity == null || !indicatorVisible) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        float indicatorSize = LockOnConfig.getIndicatorSize();
        if (ThirdPersonCompatibility.isThirdPersonActive())
            indicatorSize = ThirdPersonCompatibility.getAdjustedIndicatorSize(indicatorSize);

        Vec3 targetPos = targetEntity.getEyePosition();
        if (ThirdPersonCompatibility.isThirdPersonActive())
            targetPos = ThirdPersonCompatibility.getAdjustedTargetPosition(targetEntity, targetPos);

        LockOnRenderer.renderLockOnIndicator(
                event.getPoseStack(),
                targetEntity,
                targetPos,
                indicatorSize,
                LockOnConfig.getIndicatorType(),
                ThirdPersonCompatibility.isThirdPersonActive()
        );
    }

    // =========================================================
    //  INPUT
    // =========================================================

    private static void handleInput(LocalPlayer player) {
        boolean lockKeyPressed         = LockOnKeybinds.lockOnKey.isDown();
        boolean lockKeyClicked         = LockOnKeybinds.lockOnKey.consumeClick();
        boolean cycleKeyClicked        = LockOnKeybinds.cycleTargetKey.consumeClick();
        boolean reverseCycleKeyClicked = LockOnKeybinds.cycleTargetReverseKey.consumeClick();
        boolean clearKeyClicked        = LockOnKeybinds.clearTargetKey.consumeClick();

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

    private static void findAndLockTarget(LocalPlayer player) {
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

    private static List<Entity> findValidTargets(LocalPlayer player) {
        double baseRange = LockOnConfig.getMaxLockOnDistance();
        double range = ThirdPersonCompatibility.isThirdPersonActive()
                ? ThirdPersonCompatibility.getAdjustedTargetingRange(baseRange) : baseRange;
        double searchRadius = Math.min(range * 1.2, LockOnConfig.getSearchRadius());

        AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        return player.level.getEntities(player, searchBox).stream()
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> e != player)
                .filter(e -> e.distanceTo(player) <= range)
                .filter(e -> isValidTargetCached(e, player))
                .filter(e -> hasLineOfSightCached(player, e))
                .filter(e -> isWithinTargetingAngle(player, e))
                .limit(LockOnConfig.getMaxTargetsToSearch())
                .collect(Collectors.toList());
    }

    private static boolean isValidTargetCached(Entity entity, LocalPlayer player) {
        long now = System.currentTimeMillis();
        Long last = entityValidationCache.get(entity);
        if (last == null || now - last > CACHE_VALIDATION_MS) {
            boolean valid = isValidTarget(entity, player);
            entityValidationCache.put(entity, now);
            return valid;
        }
        return true;
    }

    private static boolean isValidTarget(Entity entity, LocalPlayer player) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (entity == player || !entity.isAlive()) return false;

        float health = living.getHealth();
        if (health < LockOnConfig.getMinTargetHealth()) return false;
        if (LockOnConfig.getMaxTargetHealth() > 0 && health > LockOnConfig.getMaxTargetHealth()) return false;

        if (entity instanceof Player    && !LockOnConfig.canTargetPlayers())     return false;
        if (entity instanceof Monster   && !LockOnConfig.canTargetHostileMobs()) return false;
        if (entity instanceof Animal    && !LockOnConfig.canTargetPassiveMobs()) return false;
        if ((entity instanceof WitherBoss || entity instanceof EnderDragon)
                && !LockOnConfig.canTargetBosses())      return false;

        String id = EntityType.getKey(entity.getType()).toString();
        if (LockOnConfig.useWhitelist())
            return LockOnConfig.getEntityWhitelist().contains(id);
        else
            return !LockOnConfig.getEntityBlacklist().contains(id);
    }

    private static boolean hasLineOfSightCached(LocalPlayer player, Entity target) {
        if (!LockOnConfig.requireLineOfSight()) return true;

        Boolean cached = lineOfSightCache.get(target);
        Vec3 lastPos = entityPositionCache.get(target);
        Vec3 currentPos = target.position();

        if (cached != null && lastPos != null && lastPos.distanceTo(currentPos) < 0.5)
            return cached;

        boolean hasLOS = hasLineOfSight(player, target);
        lineOfSightCache.put(target, hasLOS);
        entityPositionCache.put(target, currentPos);
        return hasLOS;
    }

    private static boolean hasLineOfSight(LocalPlayer player, Entity target) {
        if (!LockOnConfig.requireLineOfSight()) return true;

        Vec3 start = player.getEyePosition();
        Vec3 end = target.getEyePosition();

        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vec3 offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            start = start.add(offset.scale(0.2));
        }

        BlockHitResult result = player.level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockState state = player.level.getBlockState(result.getBlockPos());
            if (LockOnConfig.penetrateGlass() &&
                    (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                            || state.getBlock().toString().contains("glass")))
                return true;
            return false;
        }
        return true;
    }

    private static boolean isWithinTargetingAngle(LocalPlayer player, Entity target) {
        double maxAngle = LockOnConfig.getTargetingAngle();
        if (ThirdPersonCompatibility.isThirdPersonActive())
            maxAngle = ThirdPersonCompatibility.getAdjustedTargetingAngle(maxAngle);

        Vec3 playerPos = player.getEyePosition();
        Vec3 targetPos = target.getEyePosition();
        Vec3 look = player.getLookAngle();

        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vec3 offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            playerPos = playerPos.add(offset.scale(0.5));
        }

        Vec3 toTarget = targetPos.subtract(playerPos).normalize();
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, look.dot(toTarget)))) * 180.0 / Math.PI;
        return angle <= maxAngle;
    }

    private static Entity selectBestTarget(List<Entity> targets, LocalPlayer player) {
        if (targets.isEmpty()) return null;
        LockOnConfig.TargetingMode mode = runtimeTargetingMode != null
                ? runtimeTargetingMode : LockOnConfig.getTargetingMode();

        switch (mode) {
            case CLOSEST: return targets.stream()
                    .min(Comparator.comparing(e -> e.distanceTo(player))).orElse(null);
            case MOST_DAMAGED: return targets.stream()
                    .filter(e -> e instanceof LivingEntity)
                    .min(Comparator.comparing(e -> ((LivingEntity) e).getHealth())).orElse(null);
            case CROSSHAIR_CENTERED: return targets.stream()
                    .min(Comparator.comparing(e -> crosshairDistance(player, e))).orElse(null);
            case THREAT_LEVEL: return targets.stream()
                    .max(Comparator.comparing(e -> threatLevel(e))).orElse(null);
            case SMART: return targets.stream()
                    .min(Comparator.comparing(e -> smartScore(player, e))).orElse(null);
            default: return targets.get(0);
        }
    }

    private static List<Entity> adjustTargetsForThirdPerson(List<Entity> targets, LocalPlayer player) {
        Vec3 offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
        return targets.stream()
                .filter(e -> {
                    Vec3 adjustedCam = player.position().add(offset);
                    Vec3 toEntity = e.position().subtract(adjustedCam);
                    return toEntity.dot(player.getLookAngle()) > -0.5;
                })
                .collect(Collectors.toList());
    }

    private static void cycleTarget(LocalPlayer player, boolean reverse) {
        if (potentialTargets.isEmpty()) { findAndLockTarget(player); return; }

        potentialTargets = potentialTargets.stream()
                .filter(e -> e.isAlive() && isValidTargetCached(e, player))
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

    private static void updateCameraRotation(LocalPlayer player) {
        if (targetEntity == null || !targetEntity.isAlive()) {
            if (targetEntity != null) { onTargetLost(); clearTarget(); }
            return;
        }

        if (!LockOnConfig.isSmoothCameraEnabled()) return;

        Vec3 playerEyePos = player.getEyePosition();
        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vec3 offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            playerEyePos = playerEyePos.add(offset.scale(0.3));
        }

        Vec3 targetPos = targetEntity.getEyePosition();
        if (ThirdPersonCompatibility.isThirdPersonActive())
            targetPos = ThirdPersonCompatibility.getAdjustedTargetPosition(targetEntity, targetPos);

        if (LockOnConfig.isPredictiveTargetingEnabled())
            targetPos = targetPos.add(targetEntity.getDeltaMovement().scale(3.0));

        Vec3 direction = targetPos.subtract(playerEyePos).normalize();
        float targetYaw   = (float)(Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float targetPitch = (float)(Math.asin(-direction.y) * 180.0 / Math.PI);

        float speed = calculateAdaptiveRotationSpeed(player, targetEntity);
        if (ThirdPersonCompatibility.isThirdPersonActive())
            speed = ThirdPersonCompatibility.getAdjustedRotationSpeed(speed);

        float smooth = LockOnConfig.getCameraSmoothness() * speed;
        if (ThirdPersonCompatibility.shouldUseEnhancedSmoothing())
            smooth = ThirdPersonCompatibility.getThirdPersonSmoothingFactor(smooth);

        float newYaw   = interpolateAngle(player.getYRot(), targetYaw,   smooth);
        float newPitch = interpolateAngle(player.getXRot(), targetPitch, smooth);
        newPitch = Math.max(-90f, Math.min(90f, newPitch));

        // Also set SS camera directly when active
        if (ShoulderSurfingCompat.isActive()) {
            ShoulderSurfingCompat.setCameraRotation(newYaw, newPitch);
        }

        player.setYRot(newYaw);
        player.setXRot(newPitch);
        prevYaw   = newYaw;
        prevPitch = newPitch;
    }

    private static float calculateAdaptiveRotationSpeed(LocalPlayer player, Entity target) {
        float base = LockOnConfig.getRotationSpeed();
        if (!LockOnConfig.isAdaptiveRotationEnabled()) return base;

        double distance = player.distanceTo(target);
        Vec3 dir = target.getEyePosition().subtract(player.getEyePosition()).normalize();
        double angleDiff = Math.acos(Math.max(-1.0, Math.min(1.0, player.getLookAngle().dot(dir)))) * 180.0 / Math.PI;

        if (ThirdPersonCompatibility.isThirdPersonActive())
            distance += ThirdPersonCompatibility.getThirdPersonCameraOffset().length();

        float distFactor  = (float)(1.0 + ((float) LockOnConfig.getDistancePriorityWeight() * (distance / LockOnConfig.getMaxLockOnDistance())));
        float angleFactor = (float)(1.0 + ((float) LockOnConfig.getAnglePriorityWeight()    * (angleDiff / 180.0)));
        return Math.max(LockOnConfig.getMinRotationSpeed(),
                Math.min(LockOnConfig.getMaxRotationSpeed(), base * distFactor * angleFactor));
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
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();
    }

    public static void clearTarget() {
        boolean hadTarget = targetEntity != null;
        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = 0;
        wasLocked = false;
        runtimeTargetingMode = null;
        lagTimeout = 0;
        wasNetworkLagging = false;
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();
        if (hadTarget) playSound(Minecraft.getInstance().player, "target_lost");
    }

    private static void clearTargetSilently() {
        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = 0;
        wasLocked = false;
        runtimeTargetingMode = null;
        lagTimeout = 0;
        wasNetworkLagging = false;
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();
    }

    private static void onTargetLost() {
        playSound(Minecraft.getInstance().player, "target_lost");
        wasLocked = false;
    }

    private static void updateTargetingWithSync(LocalPlayer player, long currentTick) {
        if (targetEntity == null) return;

        if (!isTargetStillValid(targetEntity, player)) {
            clearTargetSilently();
            return;
        }

        if (isNetworkLagging()) {
            lagTimeout++;
            if (lagTimeout > MAX_LAG_TIMEOUT) clearTargetSilently();
            return;
        }

        if (currentTick % 5 == 0 && LockOnConfig.isAutoBreakOnObstructionEnabled()) {
            if (!hasLineOfSightCached(player, targetEntity)) { clearTargetSilently(); return; }
        }

        if (currentTick % 10 == 0) {
            double maxRange = LockOnConfig.getMaxLockOnDistance();
            if (ThirdPersonCompatibility.isThirdPersonActive())
                maxRange = ThirdPersonCompatibility.getAdjustedTargetingRange(maxRange);
            if (player.distanceTo(targetEntity) > maxRange) { clearTargetSilently(); return; }
        }

        lagTimeout = 0;
        updateCameraRotation(player);
    }

    // =========================================================
    //  VALIDATION
    // =========================================================

    private static boolean isTargetStillValid(Entity target, LocalPlayer player) {
        if (target == null || !target.isAlive() || target.isRemoved()) return false;
        if (!Minecraft.getInstance().hasSingleplayerServer()) {
            if (target.tickCount == 0 && target.getId() != -1) return false;
        }
        return isValidTargetCached(target, player);
    }

    private static boolean isNetworkLagging() {
        if (Minecraft.getInstance().hasSingleplayerServer()) return false;
        if (targetEntity != null &&
                System.currentTimeMillis() - targetEntity.tickCount > 1000) return true;
        return false;
    }

    // =========================================================
    //  MISC INPUT HANDLERS
    // =========================================================

    private static void handleTargetingModeShortcuts(LocalPlayer player) {
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

    private static void handleVisualControls(LocalPlayer player) {
        if (LockOnKeybinds.toggleIndicatorKey.consumeClick()) {
            indicatorVisible = !indicatorVisible;
            showMessage(player, "Indicator: " + (indicatorVisible ? "On" : "Off"));
            playSound(player, "target_switch");
        }
        if (LockOnKeybinds.cycleIndicatorTypeKey.consumeClick()) {
            showMessage(player, "Indicator: " + LockOnConfig.getIndicatorType().name());
            playSound(player, "target_switch");
        }
    }

    // =========================================================
    //  SOUNDS & MESSAGES
    // =========================================================

    private static void showMessage(LocalPlayer player, String message) {
        if (player != null)
            player.displayClientMessage(Component.literal(message), true);
    }

    private static void playSound(LocalPlayer player, String type) {
        if (!LockOnConfig.areSoundsEnabled() || player == null) return;
        float vol = LockOnConfig.getSoundVolume();
        switch (type) {
            case "lock_on" -> {
                if (LockOnConfig.playLockOnSound())
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, vol, 1.2f, false);
            }
            case "target_switch" -> {
                if (LockOnConfig.playTargetSwitchSound())
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, vol, 1.5f, false);
            }
            case "target_lost" -> {
                if (LockOnConfig.playTargetLostSound())
                    player.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, vol, 0.8f, false);
            }
        }
    }

    // =========================================================
    //  UTILITIES
    // =========================================================

    private static boolean shouldDisableForGameMode(LocalPlayer player) {
        if (player.isCreative()  && LockOnConfig.disableInCreative())  return true;
        if (player.isSpectator() && LockOnConfig.disableInSpectator()) return true;
        return false;
    }

    private static double crosshairDistance(LocalPlayer player, Entity entity) {
        Vec3 look = player.getLookAngle();
        Vec3 toEntity = entity.getEyePosition().subtract(player.getEyePosition()).normalize();
        return Math.acos(Math.max(-1.0, Math.min(1.0, look.dot(toEntity))));
    }

    private static double threatLevel(Entity entity) {
        if (entity instanceof Monster) return 3.0;
        if (entity instanceof Player)  return 2.0;
        if (entity instanceof Animal)  return 1.0;
        return 0.0;
    }

    private static double smartScore(LocalPlayer player, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return Double.MAX_VALUE;
        double distance  = player.distanceTo(entity);
        double angle     = crosshairDistance(player, entity);
        double healthPct = living.getHealth() / living.getMaxHealth();
        double threat    = threatLevel(entity);
        return distance * LockOnConfig.getDistancePriorityWeight()
                + angle   * LockOnConfig.getAnglePriorityWeight()
                + (1.0 - healthPct) * LockOnConfig.getHealthPriorityWeight()
                + (4.0 - threat)    * 0.1;
    }

    private static void cleanCaches() {
        long now = System.currentTimeMillis();
        entityValidationCache.entrySet().removeIf(e -> now - e.getValue() > CACHE_VALIDATION_MS * 10);
        lineOfSightCache.entrySet().removeIf(e -> e.getKey() == null || !e.getKey().isAlive());
        entityPositionCache.entrySet().removeIf(e -> e.getKey() == null || !e.getKey().isAlive());
    }

    // =========================================================
    //  PUBLIC UTILITY
    // =========================================================

    public static boolean setTargetEntity(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && entity != null && isValidTargetCached(entity, player)) {
            setTarget(entity);
            showMessage(player, "Locked: " + entity.getDisplayName().getString());
            playSound(player, "lock_on");
            return true;
        }
        return false;
    }

    public static void toggleIndicatorVisibility() {
        indicatorVisible = !indicatorVisible;
        showMessage(Minecraft.getInstance().player,
                "Indicator: " + (indicatorVisible ? "On" : "Off"));
    }

    public static void refreshTargets() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) potentialTargets = findValidTargets(player);
    }

    public static void emergencyReset() {
        clearTargetSilently();
        wasKeyHeld = false;
        indicatorVisible = true;
        lastThirdPersonState = false;
        lastCameraOffset = Vec3.ZERO;
        LOGGER.debug("Lock-On emergency reset complete");
    }

    public static String getDebugInfo() {
        return "Target: " + (targetEntity != null ? targetEntity.getDisplayName().getString() : "None")
                + " | Targets: " + potentialTargets.size()
                + " | ThirdPerson: " + ThirdPersonCompatibility.isThirdPersonActive()
                + " | Mode: " + (runtimeTargetingMode != null ? runtimeTargetingMode : "Config");
    }
}