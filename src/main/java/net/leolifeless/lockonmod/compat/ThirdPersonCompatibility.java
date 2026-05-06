package net.leolifeless.lockonmod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Compatibility handler for third person mods (1.16.5)
 * - Shoulder Surfing Reloaded (supported)
 * - Vanilla Third Person (supported)
 *
 * Note: Leawind's Third Person is not available on 1.16.5.
 * Note: Better Third Person support has been dropped.
 */
public class ThirdPersonCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SHOULDER_SURFING_MOD_ID = "shouldersurfing";

    private static boolean isShoulderSurfingLoaded = false;
    private static boolean isShoulderSurfingInitialized = false;

    private static ActiveThirdPersonMod activeMod = ActiveThirdPersonMod.NONE;

    // Reflection for Shoulder Surfing (1.16.5)
    private static Class<?> shoulderSurfingConfigClass;
    private static Class<?> shoulderSurfingApiClass;
    private static Method shoulderSurfingIsEnabledMethod;
    private static Method shoulderSurfingGetOffsetMethod;
    private static Method shoulderSurfingIsActiveMethod;

    // Camera offset cache
    private static Vector3d cachedCameraOffset = Vector3d.ZERO;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 16;

    public enum ActiveThirdPersonMod {
        NONE,
        SHOULDER_SURFING,
        MINECRAFT_VANILLA
    }

    static {
        initialize();
    }

    public static void initialize() {
        try {
            isShoulderSurfingLoaded = ModList.get().isLoaded(SHOULDER_SURFING_MOD_ID);
            LOGGER.info("Shoulder Surfing Reloaded: {}", isShoulderSurfingLoaded ? "Found" : "Not found");

            if (isShoulderSurfingLoaded) initializeShoulderSurfing();
        } catch (Exception e) {
            LOGGER.warn("Third person mod detection failed: {}", e.getMessage());
        }
    }

    // =========================================================
    //  SHOULDER SURFING INIT
    // =========================================================

    private static void initializeShoulderSurfing() {
        try {
            String[] configClasses = {
                    "com.teamderpy.shouldersurfing.config.Config",
                    "shouldersurfing.config.Config",
                    "shouldersurfing.Config",
                    "com.exopandora.shouldersurfing.config.Config",
                    "com.exopandora.shouldersurfing.Config"
            };

            String[] apiClasses = {
                    "com.teamderpy.shouldersurfing.ShoulderSurfing",
                    "shouldersurfing.ShoulderSurfing",
                    "shouldersurfing.api.ShoulderSurfingAPI",
                    "com.exopandora.shouldersurfing.ShoulderSurfing",
                    "com.exopandora.shouldersurfing.api.ShoulderSurfingAPI"
            };

            for (String c : configClasses) {
                try { shoulderSurfingConfigClass = Class.forName(c); break; }
                catch (ClassNotFoundException ignored) {}
            }

            for (String c : apiClasses) {
                try { shoulderSurfingApiClass = Class.forName(c); break; }
                catch (ClassNotFoundException ignored) {}
            }

            if (shoulderSurfingConfigClass != null || shoulderSurfingApiClass != null) {
                setupShoulderSurfingMethods();
                isShoulderSurfingInitialized = true;
                LOGGER.info("Shoulder Surfing initialized successfully");
            } else {
                LOGGER.warn("Shoulder Surfing classes not found despite mod being loaded");
            }
        } catch (Exception e) {
            LOGGER.warn("Shoulder Surfing init failed: {}", e.getMessage());
            isShoulderSurfingInitialized = false;
        }
    }

    private static void setupShoulderSurfingMethods() {
        Class<?> target = shoulderSurfingApiClass != null ? shoulderSurfingApiClass : shoulderSurfingConfigClass;

        for (String name : new String[]{"isEnabled", "isActive", "isShoulderSurfingEnabled", "enabled"}) {
            try { shoulderSurfingIsEnabledMethod = target.getMethod(name); break; }
            catch (NoSuchMethodException ignored) {}
        }

        for (String name : new String[]{"getCameraOffset", "getOffset", "getShoulderOffset", "getCameraPosition"}) {
            try { shoulderSurfingGetOffsetMethod = target.getMethod(name); break; }
            catch (NoSuchMethodException ignored) {}
        }

        for (String name : new String[]{"isThirdPersonEnabled", "isActive", "isInThirdPerson"}) {
            try { shoulderSurfingIsActiveMethod = target.getMethod(name); break; }
            catch (NoSuchMethodException ignored) {}
        }
    }

    // =========================================================
    //  DETECTION
    // =========================================================

    private static ActiveThirdPersonMod detectActiveMod() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options == null) return ActiveThirdPersonMod.NONE;

            boolean isThirdPerson = mc.options.getCameraType()
                    != net.minecraft.client.settings.PointOfView.FIRST_PERSON;
            if (!isThirdPerson) return ActiveThirdPersonMod.NONE;

            if (isShoulderSurfingLoaded && isShoulderSurfingEnabled())
                return ActiveThirdPersonMod.SHOULDER_SURFING;

            return ActiveThirdPersonMod.MINECRAFT_VANILLA;

        } catch (Exception e) {
            LOGGER.debug("Error detecting active mod: {}", e.getMessage());
            return ActiveThirdPersonMod.NONE;
        }
    }

    private static boolean isShoulderSurfingEnabled() {
        if (!isShoulderSurfingLoaded) return false;
        try {
            if (isShoulderSurfingInitialized) {
                if (shoulderSurfingIsEnabledMethod != null) {
                    Boolean result = (Boolean) shoulderSurfingIsEnabledMethod.invoke(null);
                    return result != null && result;
                }
                if (shoulderSurfingIsActiveMethod != null) {
                    Boolean result = (Boolean) shoulderSurfingIsActiveMethod.invoke(null);
                    return result != null && result;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking Shoulder Surfing state: {}", e.getMessage());
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
        return isShoulderSurfingLoaded;
    }

    public static boolean isShoulderSurfingDecoupled() {
        return isShoulderSurfingLoaded && isShoulderSurfingInitialized;
    }

    // =========================================================
    //  CAMERA OFFSET
    // =========================================================

    public static Vector3d getThirdPersonCameraOffset() {
        try {
            activeMod = detectActiveMod();
            switch (activeMod) {
                case SHOULDER_SURFING: return getShoulderSurfingCameraOffset();
                case MINECRAFT_VANILLA: return getVanillaCameraOffset();
                default: return Vector3d.ZERO;
            }
        } catch (Exception e) {
            LOGGER.debug("getThirdPersonCameraOffset error: {}", e.getMessage());
            return Vector3d.ZERO;
        }
    }

    public static Vector3d getCachedThirdPersonCameraOffset() {
        long now = System.currentTimeMillis();
        if (now - lastCacheTime > CACHE_DURATION_MS) {
            cachedCameraOffset = getThirdPersonCameraOffset();
            lastCacheTime = now;
        }
        return cachedCameraOffset;
    }

    private static Vector3d getShoulderSurfingCameraOffset() {
        try {
            if (shoulderSurfingGetOffsetMethod != null) {
                Object offset = shoulderSurfingGetOffsetMethod.invoke(null);
                if (offset instanceof Vector3d) return (Vector3d) offset;
            }
        } catch (Exception e) {
            LOGGER.debug("SS camera offset error: {}", e.getMessage());
        }
        return new Vector3d(1.5, 0, -3.0); // fallback estimate
    }

    private static Vector3d getVanillaCameraOffset() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) {
                switch (mc.options.getCameraType()) {
                    case THIRD_PERSON_BACK:  return new Vector3d(0, 0, -4.0);
                    case THIRD_PERSON_FRONT: return new Vector3d(0, 0,  4.0);
                    default: return Vector3d.ZERO;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Vanilla camera offset error: {}", e.getMessage());
        }
        return Vector3d.ZERO;
    }

    // =========================================================
    //  ADJUSTMENTS
    // =========================================================

    public static double getAdjustedTargetingRange(double baseRange) {
        if (!isThirdPersonActive()) return baseRange;
        double offsetDist = getCachedThirdPersonCameraOffset().length();
        return activeMod == ActiveThirdPersonMod.SHOULDER_SURFING
                ? baseRange * 1.3 + offsetDist
                : baseRange * 1.1 + offsetDist;
    }

    public static double getAdjustedTargetingAngle(double baseAngle) {
        if (!isThirdPersonActive()) return baseAngle;
        return activeMod == ActiveThirdPersonMod.SHOULDER_SURFING
                ? Math.min(baseAngle * 1.4, 180.0)
                : Math.min(baseAngle * 1.15, 180.0);
    }

    public static float getAdjustedRotationSpeed(float baseSpeed) {
        if (!isThirdPersonActive()) return baseSpeed;
        return activeMod == ActiveThirdPersonMod.SHOULDER_SURFING
                ? baseSpeed * 0.8f
                : baseSpeed * 0.9f;
    }

    public static float getAdjustedIndicatorSize(float baseSize) {
        if (!isThirdPersonActive()) return baseSize;
        double dist = getCachedThirdPersonCameraOffset().length();
        float scale = 1.0f + (float)(dist * 0.05);
        if (activeMod == ActiveThirdPersonMod.SHOULDER_SURFING) scale *= 1.1f;
        return baseSize * Math.min(scale, 1.5f);
    }

    public static Vector3d getAdjustedTargetPosition(Entity target, Vector3d basePosition) {
        if (!isThirdPersonActive() || target == null) return basePosition;
        Vector3d offset = getCachedThirdPersonCameraOffset();
        return activeMod == ActiveThirdPersonMod.SHOULDER_SURFING
                ? basePosition.add(offset.scale(0.15))
                : basePosition.add(offset.scale(0.1));
    }

    public static boolean shouldUseEnhancedSmoothing() {
        return isThirdPersonActive();
    }

    public static float getThirdPersonSmoothingFactor(float baseFactor) {
        if (!isThirdPersonActive()) return baseFactor;
        return activeMod == ActiveThirdPersonMod.SHOULDER_SURFING
                ? Math.min(baseFactor * 1.2f, 1.0f)
                : Math.min(baseFactor * 1.1f, 1.0f);
    }

    // =========================================================
    //  STATUS
    // =========================================================

    public static String getCompatibilityStatus() {
        activeMod = detectActiveMod();
        if (!isShoulderSurfingLoaded)
            return "No third person mods detected (1.16.5)";
        return "Shoulder Surfing: " + (isShoulderSurfingInitialized ? "Enhanced" : "Basic")
                + " | Active: " + activeMod.name();
    }

    public static String getDetailedStatus() {
        StringBuilder sb = new StringBuilder("Third Person Compat (1.16.5):\n");
        sb.append("- Shoulder Surfing: ").append(isShoulderSurfingLoaded ? "Loaded" : "Not found");
        if (isShoulderSurfingLoaded)
            sb.append(" (").append(isShoulderSurfingInitialized ? "Enhanced" : "Basic").append(")");
        sb.append("\n- Active: ").append(detectActiveMod().name());
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null)
                sb.append("\n- Camera: ").append(mc.options.getCameraType());
        } catch (Exception ignored) {}
        return sb.toString();
    }

    public static boolean isCameraCenteredForFlying() {
        try {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            return player != null && player.isFallFlying();
        } catch (Exception e) {
            return false;
        }
    }

    // Stubs kept for LockOnSystem compatibility
    public static void ensurePlayerRotationSettings() {}
    public static void restorePlayerRotationSettings() {}
    public static void disableShoulderSurfingInput() {}
    public static void enableShoulderSurfingInput() {}
}