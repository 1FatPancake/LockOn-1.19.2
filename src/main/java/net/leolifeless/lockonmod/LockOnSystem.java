package net.leolifeless.lockonmod;

import net.leolifeless.lockonmod.compat.LeawindCompat;
import net.leolifeless.lockonmod.compat.ShoulderSurfingCompat;
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

    // === PERFORMANCE CACHES ===
    private static final Map<Entity, Long> entityValidationCache = new HashMap<>();
    private static final Map<Entity, Boolean> lineOfSightCache = new HashMap<>();
    private static final Map<Entity, Vec3> entityPositionCache = new HashMap<>();

    // === TIMING ===
    private static long lastCameraUpdate = 0;
    private static long lastCacheCleanup = 0;
    private static long lastThirdPersonCheck = 0;

    // === SMOOTH CAMERA STATE ===
    private static boolean firstRotationFrame = true;
    private static Vec3 previousTargetPos = Vec3.ZERO;

    // === THIRD PERSON STATE ===
    private static Vec3 lastCameraOffset = Vec3.ZERO;
    private static boolean lastThirdPersonState = false;

    // === SYNC PROTECTION ===
    private static int lagTimeout = 0;
    private static final int MAX_LAG_TIMEOUT = 100;
    private static int syncCheckCounter = 0;
    private static boolean wasNetworkLagging = false;

    // === PERFORMANCE SETTINGS (fixed values) ===
    private static final int CAMERA_UPDATE_INTERVAL = 1;
    private static final long CACHE_VALIDATION_MS = 100;

    // =========================================================
    //  PUBLIC ACCESSORS
    // =========================================================

    public static Entity getTargetEntity()           { return targetEntity; }
    public static boolean hasTarget()                { return targetEntity != null && targetEntity.isAlive(); }
    public static List<Entity> getPotentialTargets() { return new ArrayList<>(potentialTargets); }
    public static boolean wasLocked()                { return wasLocked; }
    public static boolean isActive()                 { return targetEntity != null && targetEntity.isAlive(); }
    public static boolean isIndicatorVisible()       { return indicatorVisible; }

    public static double getCurrentTargetDistance() {
        LocalPlayer player = Minecraft.getInstance().player;
        return (player != null && targetEntity != null) ? player.distanceTo(targetEntity) : 0.0;
    }

    public static float getCurrentTargetHealthPercent() {
        if (targetEntity instanceof LivingEntity living)
            return living.getHealth() / living.getMaxHealth();
        return 0f;
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

        long currentTime = System.currentTimeMillis();

        // Periodic sync check (every 3 seconds)
        syncCheckCounter++;
        if (syncCheckCounter >= 60) {
            syncCheckCounter = 0;
            if (targetEntity != null && !isTargetStillValid(targetEntity, player)) {
                clearTargetSilently();
            }
        }

        // Third person state change detection
        if (currentTime - lastThirdPersonCheck > 500) {
            checkThirdPersonStateChange();
            lastThirdPersonCheck = currentTime;
        }

        if (shouldDisableForGameMode(player)) {
            clearTarget();
            return;
        }

        handleInput(player);

        // Cache cleanup every 5 seconds
        if (currentTime - lastCacheCleanup > 5000) {
            cleanCaches();
            lastCacheCleanup = currentTime;
        }
    }

    // =========================================================
    //  RENDER — per-frame camera update
    // =========================================================

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (targetEntity != null && indicatorVisible)
            LockOnRenderer.renderLockOnIndicator(event, targetEntity);

        if (targetEntity != null)
            updateCameraRotation(player);
    }

    // =========================================================
    //  INPUT
    // =========================================================

    private static void handleInput(LocalPlayer player) {
        boolean lockKeyPressed = LockOnKeybinds.lockOnKey.isDown();
        boolean lockKeyClicked = LockOnKeybinds.lockOnKey.consumeClick();
        boolean cycleKeyClicked = LockOnKeybinds.cycleTargetKey.consumeClick();
        boolean reverseCycleKeyClicked = LockOnKeybinds.cycleTargetReverseKey.consumeClick();
        boolean clearKeyClicked = LockOnKeybinds.clearTargetKey.consumeClick();

        boolean toggleMode = LockOnConfig.isToggleMode();
        boolean holdMode   = LockOnConfig.holdToMaintainLock();

        if (toggleMode) {
            if (lockKeyClicked) {
                if (targetEntity == null) findAndLockTarget(player);
                else clearTarget();
            }
        } else if (holdMode) {
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

        // Manual sync check keybind
        if (LockOnKeybinds.forceSyncCheckKey.consumeClick())
            forceSyncCheck(player);
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
        double range = getMaxRange();
        double searchRadius = Math.min(range * 1.2, LockOnConfig.getSearchRadius());

        AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        return player.level().getEntities(player, searchBox).stream()
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
        float minHealth = LockOnConfig.getMinTargetHealth();
        float maxHealth = LockOnConfig.getMaxTargetHealth();
        if (health < minHealth) return false;
        if (maxHealth > 0 && health > maxHealth) return false;

        if (entity instanceof Player    && !LockOnConfig.canTargetPlayers())     return false;
        if (entity instanceof Monster   && !LockOnConfig.canTargetHostileMobs()) return false;
        if (entity instanceof Animal    && !LockOnConfig.canTargetPassiveMobs()) return false;
        if ((entity instanceof WitherBoss || entity instanceof EnderDragon)
                && !LockOnConfig.canTargetBosses())      return false;

        String entityId = EntityType.getKey(entity.getType()).toString();

        if (LockOnConfig.useWhitelist())
            return LockOnConfig.getEntityWhitelist().contains(entityId);
        else
            return !LockOnConfig.getEntityBlacklist().contains(entityId);
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
        Vec3 start = player.getEyePosition();
        Vec3 end = target.getEyePosition();

        if (ThirdPersonCompatibility.isThirdPersonActive()) {
            Vec3 offset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
            start = start.add(offset.scale(0.2));
        }

        BlockHitResult result = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockState state = player.level().getBlockState(result.getBlockPos());
            if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                    || state.getBlock().toString().contains("glass"))
                return true;
            return false;
        }
        return true;
    }

    private static boolean isWithinTargetingAngle(LocalPlayer player, Entity target) {
        // Always maintain lock on current target when decoupled
        if (targetEntity != null && target == targetEntity
                && ThirdPersonCompatibility.isShoulderSurfingDecoupled())
            return true;

        double maxAngle = getTargetingAngle();
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

        return switch (mode) {
            case CLOSEST -> targets.stream()
                    .min(Comparator.comparing(e -> e.distanceTo(player))).orElse(null);
            case MOST_DAMAGED -> targets.stream()
                    .filter(e -> e instanceof LivingEntity)
                    .min(Comparator.comparing(e -> ((LivingEntity) e).getHealth())).orElse(null);
            case CROSSHAIR_CENTERED -> targets.stream()
                    .min(Comparator.comparing(e -> crosshairDistance(player, e))).orElse(null);
            case THREAT_LEVEL -> targets.stream()
                    .max(Comparator.comparing(e -> threatLevel(e))).orElse(null);
            default -> targets.get(0);
        };
    }

    private static void cycleTarget(LocalPlayer player, boolean reverse) {
        if (potentialTargets.isEmpty()) {
            findAndLockTarget(player);
            return;
        }

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
        firstRotationFrame = true;
    }

    // =========================================================
    //  CAMERA ROTATION
    // =========================================================

    private static void updateCameraRotation(LocalPlayer player) {
        if (targetEntity == null || !targetEntity.isAlive()) {
            if (targetEntity != null) { onTargetLost(); clearTarget(); }
            return;
        }

        Vec3 playerEyePos = player.getEyePosition();
        Vec3 targetPos = targetEntity.getEyePosition();
        Vec3 direction = targetPos.subtract(playerEyePos).normalize();

        float targetYaw   = (float)(Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI);
        float targetPitch = (float)(Math.asin(-direction.y) * 180.0 / Math.PI);

        float currentYaw, currentPitch;
        if (ShoulderSurfingCompat.isActive()) {
            float[] r = ShoulderSurfingCompat.getCameraRotation();
            currentYaw = r[0]; currentPitch = r[1];
        } else if (LeawindCompat.isLoaded() && LeawindCompat.isInitialized()) {
            float[] r = LeawindCompat.getCameraRotation();
            currentYaw = r[0]; currentPitch = r[1];
        } else {
            currentYaw   = player.getYRot();
            currentPitch = player.getXRot();
        }

        float yawDiff   = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Skip micro-movements
        if (Math.abs(yawDiff) < 0.2f && Math.abs(pitchDiff) < 0.2f) return;

        // Adaptive speed
        float distance    = player.distanceTo(targetEntity);
        float angleDiff   = Math.abs(yawDiff) + Math.abs(pitchDiff);
        float baseSpeed   = 0.15f;
        float distScale   = distance < 2f ? 2f : distance < 5f ? 1.5f : distance > 15f ? 0.7f : 1f;
        float angleScale  = angleDiff > 30f ? 2f : angleDiff > 10f ? 1.5f : 1f;
        float speed       = Math.min(baseSpeed * distScale * angleScale, 0.8f);

        float newYaw   = currentYaw + yawDiff * speed;
        float newPitch = Math.max(-90f, Math.min(90f, currentPitch + pitchDiff * speed));

        if (ShoulderSurfingCompat.isActive()) {
            ShoulderSurfingCompat.setCameraRotation(newYaw, newPitch);
            player.setYRot(newYaw);
            player.setXRot(newPitch);
            player.setYHeadRot(newYaw);
            player.yRotO = newYaw;
            player.xRotO = newPitch;
        } else if (LeawindCompat.isLoaded() && LeawindCompat.isInitialized()) {
            LeawindCompat.forceCameraFollowEntity();
            LeawindCompat.setCameraRotation(newYaw, newPitch);
            player.setYRot(newYaw);
            player.setXRot(newPitch);
            player.setYHeadRot(newYaw);
            player.yRotO = newYaw;
            player.xRotO = newPitch;
        } else {
            player.setYRot(newYaw);
            player.setXRot(newPitch);
            player.setYHeadRot(newYaw);
            player.yRotO = newYaw;
            player.xRotO = newPitch;
            player.turn(yawDiff * speed * 0.3f, pitchDiff * speed * 0.3f);
        }
    }

    // =========================================================
    //  TARGET MANAGEMENT
    // =========================================================

    private static void setTarget(Entity target) {
        ThirdPersonCompatibility.ensurePlayerRotationSettings();
        targetEntity = target;
        firstRotationFrame = true;
        wasLocked = true;
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();

        if (ThirdPersonCompatibility.getActiveMod()
                == ThirdPersonCompatibility.ActiveThirdPersonMod.SHOULDER_SURFING) {
            ThirdPersonCompatibility.disableShoulderSurfingInput();
        }
        if (LeawindCompat.isLoaded() && LeawindCompat.isInitialized()) {
            LeawindCompat.forceCameraFollowEntity();
        }
    }

    public static void clearTarget() {
        ThirdPersonCompatibility.restorePlayerRotationSettings();
        boolean hadTarget = targetEntity != null;

        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = -1;
        wasLocked = false;
        runtimeTargetingMode = null;
        firstRotationFrame = true;
        previousTargetPos = Vec3.ZERO;
        lagTimeout = 0;
        wasNetworkLagging = false;
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();

        if (hadTarget) {
            LocalPlayer player = Minecraft.getInstance().player;
            playSound(player, "target_lost");
        }
    }

    /** Clear target silently (no sound/message) — for internal sync checks */
    private static void clearTargetSilently() {
        ThirdPersonCompatibility.restorePlayerRotationSettings();
        targetEntity = null;
        potentialTargets.clear();
        currentTargetIndex = -1;
        wasLocked = false;
        runtimeTargetingMode = null;
        firstRotationFrame = true;
        previousTargetPos = Vec3.ZERO;
        lagTimeout = 0;
        wasNetworkLagging = false;
        entityValidationCache.clear();
        lineOfSightCache.clear();
        entityPositionCache.clear();
    }

    private static void onTargetLost() {
        LocalPlayer player = Minecraft.getInstance().player;
        playSound(player, "target_lost");
        wasLocked = false;
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

    private static boolean isNetworkLagging(LocalPlayer player) {
        if (Minecraft.getInstance().hasSingleplayerServer()) return false;
        if (targetEntity != null) {
            if (System.currentTimeMillis() - targetEntity.tickCount > 1000) return true;
        }
        return false;
    }

    private static void forceSyncCheck(LocalPlayer player) {
        if (targetEntity != null && !isTargetStillValid(targetEntity, player)) {
            clearTargetSilently();
            showMessage(player, "§cDesync detected — target cleared");
        } else if (targetEntity != null) {
            showMessage(player, "§aSync OK");
        }
    }

    // =========================================================
    //  THIRD PERSON
    // =========================================================

    private static void checkThirdPersonStateChange() {
        boolean current = ThirdPersonCompatibility.isThirdPersonActive();
        if (current != lastThirdPersonState) {
            lastThirdPersonState = current;
            firstRotationFrame = true;
            if (targetEntity != null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && !isValidTargetCached(targetEntity, player))
                    clearTargetSilently();
            }
        }
        if (current)
            lastCameraOffset = ThirdPersonCompatibility.getThirdPersonCameraOffset();
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
        Level level = player.level();
        float vol = LockOnConfig.getSoundVolume();
        try {
            switch (type) {
                case "lock_on" -> {
                    if (LockOnConfig.playLockOnSound())
                        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, vol, 1.2f, false);
                }
                case "target_switch" -> {
                    if (LockOnConfig.playTargetSwitchSound())
                        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                                SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.PLAYERS, vol, 1.5f, false);
                }
                case "target_lost" -> {
                    if (LockOnConfig.playTargetLostSound())
                        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, vol, 0.8f, false);
                }
            }
        } catch (Exception e) {
            LockOnMod.LOGGER.debug("Sound playback failed: {}", e.getMessage());
        }
    }

    // =========================================================
    //  CACHE
    // =========================================================

    private static void cleanCaches() {
        long now = System.currentTimeMillis();
        entityValidationCache.entrySet().removeIf(e -> now - e.getValue() > CACHE_VALIDATION_MS * 10);
        lineOfSightCache.entrySet().removeIf(e -> e.getKey() == null || !e.getKey().isAlive());
        entityPositionCache.entrySet().removeIf(e -> e.getKey() == null || !e.getKey().isAlive());
    }

    // =========================================================
    //  UTILITIES
    // =========================================================

    private static float normalizeAngle(float angle) {
        while (angle > 180)  angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
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

    private static double getMaxRange() {
        double base = LockOnConfig.getMaxLockOnDistance();
        return ThirdPersonCompatibility.isThirdPersonActive()
                ? ThirdPersonCompatibility.getAdjustedTargetingRange(base) : base;
    }

    private static double getTargetingAngle() {
        double base = LockOnConfig.getTargetingAngle();
        return ThirdPersonCompatibility.isThirdPersonActive()
                ? ThirdPersonCompatibility.getAdjustedTargetingAngle(base) : base;
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

    private static boolean shouldDisableForGameMode(LocalPlayer player) {
        if (player.isCreative()  && LockOnConfig.disableInCreative())  return true;
        if (player.isSpectator() && LockOnConfig.disableInSpectator()) return true;
        return false;
    }

    public static void toggleIndicatorVisibility() {
        indicatorVisible = !indicatorVisible;
        LocalPlayer player = Minecraft.getInstance().player;
        showMessage(player, "Indicator: " + (indicatorVisible ? "On" : "Off"));
    }

    public static void refreshTargets() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) potentialTargets = findValidTargets(player);
    }

    public static String getThirdPersonCompatibilityStatus() {
        return ThirdPersonCompatibility.getCompatibilityStatus();
    }

    public static void emergencyReset() {
        clearTargetSilently();
        wasKeyHeld = false;
        indicatorVisible = true;
        lastThirdPersonState = false;
        lastCameraOffset = Vec3.ZERO;
        lastThirdPersonCheck = 0;
        try {
            ThirdPersonCompatibility.restorePlayerRotationSettings();
            if (ThirdPersonCompatibility.getActiveMod()
                    == ThirdPersonCompatibility.ActiveThirdPersonMod.SHOULDER_SURFING)
                ThirdPersonCompatibility.enableShoulderSurfingInput();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Emergency reset failed to restore third person settings: {}", e.getMessage());
        }
        LockOnMod.LOGGER.debug("Lock-On emergency reset complete");
    }
}