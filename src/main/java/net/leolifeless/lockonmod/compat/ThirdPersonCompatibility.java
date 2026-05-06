package net.leolifeless.lockonmod.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Compatibility handler for third person mods (1.19.2)
 * - Shoulder Surfing Reloaded (full API support)
 * - Leawind's Third Person (basic support)
 * - Vanilla Third Person (supported)
 *
 * Note: Better Third Person support has been dropped.
 */
public class ThirdPersonCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String LEAWIND_MOD_ID        = "leawind_third_person";
    private static final String SHOULDER_SURFING_MOD_ID = "shouldersurfing";

    private static boolean isLeawindLoaded           = false;
    private static boolean isShoulderSurfingLoaded   = false;
    private static boolean isLeawindInitialized      = false;
    private static boolean isShoulderSurfingInitialized = false;

    private static ActiveThirdPersonMod activeMod = ActiveThirdPersonMod.NONE;

    // Leawind reflection (basic — no full API on 1.19.2)
    private static Class<?> leawindConfigClass;
    private static java.lang.reflect.Method leawindIsEnabledMethod;
    private static java.lang.reflect.Field  leawindEnabledField;

    // Camera offset cache
    private static Vec3  cachedCameraOffset = Vec3.ZERO;
    private static long  lastCacheTime      = 0;
    private static final long CACHE_DURATION_MS = 16;

    public enum ActiveThirdPersonMod {
        NONE,
        LEAWIND,
        SHOULDER_SURFING,
        MINECRAFT_VANILLA
    }

    static {
        initialize();
    }

    public static void initialize() {
        try {
            isLeawindLoaded          = ModList.get().isLoaded(LEAWIND_MOD_ID);
            isShoulderSurfingLoaded  = ModList.get().isLoaded(SHOULDER_SURFING_MOD_ID);

            LOGGER.info("Third person mod detection (1.19.2): Leawind={}, ShoulderSurfing={}",
                    isLeawindLoaded ? "Found" : "Not found",
                    isShoulderSurfingLoaded ? "Found" : "Not found");

            if (isLeawindLoaded)         initializeLeawind();
            if (isShoulderSurfingLoaded) isShoulderSurfingInitialized = true; // API handled by ShoulderSurfingCompat
        } catch (Exception e) {
            LOGGER.warn("Third person mod detection failed: {}", e.getMessage());
        }
    }

    // =========================================================
    //  LEAWIND INIT (reflection only — no public API on 1.19.2)
    // =========================================================

    private static void initializeLeawind() {
        try {
            String[] classes = {
                    "com.github.leawind.thirdperson.config.Config",
                    "com.github.leawind.thirdperson.Config",
                    "leawind.thirdperson.config.Config",
                    "leawind.thirdperson.Config"
            };
            for (String c : classes) {
                try { leawindConfigClass = Class.forName(c); break; }
                catch (ClassNotFoundException ignored) {}
            }

            if (leawindConfigClass != null) {
                for (String m : new String[]{"isModEnabled", "isEnabled", "isAvailable"}) {
                    try { leawindIsEnabledMethod = leawindConfigClass.getMethod(m); break; }
                    catch (NoSuchMethodException ignored) {}
                }
                if (leawindIsEnabledMethod == null) {
                    for (String f : new String[]{"enabled", "isEnabled", "modEnabled"}) {
                        try {
                            leawindEnabledField = leawindConfigClass.getField(f);
                            leawindEnabledField.setAccessible(true);
                            break;
                        } catch (NoSuchFieldException ignored) {}
                    }
                }
                isLeawindInitialized = leawindIsEnabledMethod != null || leawindEnabledField != null;
            }
            LOGGER.info("Leawind init: {}", isLeawindInitialized ? "OK" : "basic fallback");
        } catch (Exception e) {
            LOGGER.warn("Leawind init failed: {}", e.getMessage());
            isLeawindInitialized = false;
        }
    }

    // =========================================================
    //  DETECTION
    // =========================================================

    private static ActiveThirdPersonMod detectActiveMod() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options == null) return ActiveThirdPersonMod.NONE;
            if (mc.options.getCameraType().isFirstPerson()) return ActiveThirdPersonMod.NONE;

            if (isLeawindLoaded && isLeawindEnabled())
                return ActiveThirdPersonMod.LEAWIND;

            if (isShoulderSurfingLoaded && ShoulderSurfingCompat.isActive())
                return ActiveThirdPersonMod.SHOULDER_SURFING;

            return ActiveThirdPersonMod.MINECRAFT_VANILLA;
        } catch (Exception e) {
            LOGGER.debug("detectActiveMod error: {}", e.getMessage());
            return ActiveThirdPersonMod.NONE;
        }
    }

    private static boolean isLeawindEnabled() {
        if (!isLeawindLoaded) return false;
        try {
            if (leawindIsEnabledMethod != null) {
                Boolean r = (Boolean) leawindIsEnabledMethod.invoke(null);
                return r != null && r;
            }
            if (leawindEnabledField != null) {
                Boolean r = (Boolean) leawindEnabledField.get(null);
                return r != null && r;
            }
        } catch (Exception e) {
            LOGGER.debug("isLeawindEnabled error: {}", e.getMessage());
        }
        return true; // assume enabled if we can't check
    }

    // =========================================================
    //  PUBLIC API
    // =========================================================

    public static boolean isThirdPersonActive() {
        try {
            activeMod = detectActiveMod();
            return activeMod != ActiveThirdPersonMod.NONE;
        } catch (Exception e) {
            LOGGER.debug("isThirdPersonActive error: {}", e.getMessage());
            return false;
        }
    }

    public static ActiveThirdPersonMod getActiveMod() {
        try { return detectActiveMod(); }
        catch (Exception e) { return ActiveThirdPersonMod.NONE; }
    }

    public static boolean isModLoaded() {
        return isLeawindLoaded || isShoulderSurfingLoaded;
    }

    public static boolean isShoulderSurfingDecoupled() {
        return isShoulderSurfingLoaded && ShoulderSurfingCompat.isCameraDecoupled();
    }

    // Stubs for LockOnSystem compatibility
    public static void ensurePlayerRotationSettings() {}
    public static void restorePlayerRotationSettings() {}
    public static void disableShoulderSurfingInput()   {}
    public static void enableShoulderSurfingInput()    {}

    // =========================================================
    //  CAMERA OFFSET
    // =========================================================

    public static Vec3 getThirdPersonCameraOffset() {
        try {
            activeMod = detectActiveMod();
            switch (activeMod) {
                case LEAWIND:          return new Vec3(0, 0, -4.0);
                case SHOULDER_SURFING: return getShoulderSurfingOffset();
                case MINECRAFT_VANILLA: return getVanillaOffset();
                default: return Vec3.ZERO;
            }
        } catch (Exception e) {
            LOGGER.debug("getThirdPersonCameraOffset error: {}", e.getMessage());
            return Vec3.ZERO;
        }
    }

    public static Vec3 getCachedThirdPersonCameraOffset() {
        long now = System.currentTimeMillis();
        if (now - lastCacheTime > CACHE_DURATION_MS) {
            cachedCameraOffset = getThirdPersonCameraOffset();
            lastCacheTime = now;
        }
        return cachedCameraOffset;
    }

    private static Vec3 getShoulderSurfingOffset() {
        Vec3 offset = ShoulderSurfingCompat.getCameraRenderOffset();
        return offset != Vec3.ZERO ? offset : new Vec3(1.5, 0, -3.0);
    }

    private static Vec3 getVanillaOffset() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) {
                switch (mc.options.getCameraType()) {
                    case THIRD_PERSON_BACK:  return new Vec3(0, 0, -4.0);
                    case THIRD_PERSON_FRONT: return new Vec3(0, 0,  4.0);
                    default: return Vec3.ZERO;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("getVanillaOffset error: {}", e.getMessage());
        }
        return Vec3.ZERO;
    }

    // =========================================================
    //  ADJUSTMENTS
    // =========================================================

    public static double getAdjustedTargetingRange(double baseRange) {
        if (!isThirdPersonActive()) return baseRange;
        double dist = getCachedThirdPersonCameraOffset().length();
        switch (activeMod) {
            case SHOULDER_SURFING: return baseRange * 1.3 + dist;
            case LEAWIND:          return baseRange * 1.2 + dist;
            default:               return baseRange * 1.1 + dist;
        }
    }

    public static double getAdjustedTargetingAngle(double baseAngle) {
        if (!isThirdPersonActive()) return baseAngle;
        switch (activeMod) {
            case SHOULDER_SURFING: return Math.min(baseAngle * 1.4, 180.0);
            case LEAWIND:          return Math.min(baseAngle * 1.3, 180.0);
            default:               return Math.min(baseAngle * 1.15, 180.0);
        }
    }

    public static float getAdjustedRotationSpeed(float baseSpeed) {
        if (!isThirdPersonActive()) return baseSpeed;
        switch (activeMod) {
            case SHOULDER_SURFING: return baseSpeed * 0.8f;
            case LEAWIND:          return baseSpeed * 0.85f;
            default:               return baseSpeed * 0.9f;
        }
    }

    public static float getAdjustedIndicatorSize(float baseSize) {
        if (!isThirdPersonActive()) return baseSize;
        double dist = getCachedThirdPersonCameraOffset().length();
        float scale = 1.0f + (float)(dist * 0.05);
        if (activeMod == ActiveThirdPersonMod.SHOULDER_SURFING) scale *= 1.1f;
        return baseSize * Math.min(scale, 1.5f);
    }

    public static Vec3 getAdjustedTargetPosition(Entity target, Vec3 basePosition) {
        if (!isThirdPersonActive() || target == null) return basePosition;
        Vec3 offset = getCachedThirdPersonCameraOffset();
        return activeMod == ActiveThirdPersonMod.SHOULDER_SURFING
                ? basePosition.add(offset.scale(0.15))
                : basePosition.add(offset.scale(0.1));
    }

    public static boolean shouldUseEnhancedSmoothing() {
        return isThirdPersonActive();
    }

    public static float getThirdPersonSmoothingFactor(float baseFactor) {
        if (!isThirdPersonActive()) return baseFactor;
        switch (activeMod) {
            case SHOULDER_SURFING: return Math.min(baseFactor * 1.2f, 1.0f);
            case LEAWIND:          return Math.min(baseFactor * 1.15f, 1.0f);
            default:               return Math.min(baseFactor * 1.1f, 1.0f);
        }
    }

    // =========================================================
    //  STATUS
    // =========================================================

    public static String getCompatibilityStatus() {
        activeMod = detectActiveMod();
        StringBuilder sb = new StringBuilder("Third Person (1.19.2):");
        if (isLeawindLoaded)         sb.append(" Leawind(").append(isLeawindInitialized ? "OK" : "basic").append(")");
        if (isShoulderSurfingLoaded) sb.append(" ShoulderSurfing(OK)");
        if (!isLeawindLoaded && !isShoulderSurfingLoaded) sb.append(" None");
        sb.append(" | Active: ").append(activeMod.name());
        return sb.toString();
    }

    public static String getDetailedStatus() {
        StringBuilder sb = new StringBuilder("Third Person Compat (1.19.2):\n");
        sb.append("- Leawind: ").append(isLeawindLoaded ? "Loaded" : "Not found");
        if (isLeawindLoaded) sb.append(" (").append(isLeawindInitialized ? "OK" : "basic").append(")");
        sb.append("\n- Shoulder Surfing: ").append(isShoulderSurfingLoaded ? "Loaded" : "Not found");
        sb.append("\n- Active: ").append(detectActiveMod().name());
        return sb.toString();
    }

    public static boolean isCameraCenteredForFlying() {
        try {
            LocalPlayer player = Minecraft.getInstance().player;
            return activeMod == ActiveThirdPersonMod.LEAWIND
                    && player != null && player.isFallFlying();
        } catch (Exception e) { return false; }
    }
}