package net.leolifeless.lockonmod.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LeawindCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "leawind_third_person";

    private static boolean loaded = false;
    private static boolean initialized = false;

    // Cached once at init - never looked up per frame
    private static Object cameraAgentInstance = null;
    private static Class<?> vector2dClass = null;
    private static Field relativeRotationField = null;
    private static Field vec2dX = null;
    private static Field vec2dY = null;
    private static Object entityAgentInstance = null;
    private static Method setRotateTargetMethod = null;
    private static Method setRotationSmoothTypeMethod = null;
    private static Object cameraRotationEnum = null;
    private static Object hardSmoothEnum = null;

    // State cache - refreshed every 1 second max
    private static boolean cachedEnabled = false;
    private static long lastCacheUpdate = 0;
    private static final long CACHE_DURATION_MS = 1000;

    public static void initialize() {
        loaded = ModList.get().isLoaded(MOD_ID);
        if (!loaded) {
            LOGGER.info("Leawind's Third Person not present, skipping");
            return;
        }

        try {
            // Step 1: Get ThirdPerson main class
            Class<?> thirdPersonClass = Class.forName(
                    "com.github.leawind.thirdperson.ThirdPerson"
            );

            // Step 2: Get CAMERA_AGENT singleton instance
            Field cameraAgentField = thirdPersonClass.getDeclaredField("CAMERA_AGENT");
            cameraAgentField.setAccessible(true);
            cameraAgentInstance = cameraAgentField.get(null);

            if (cameraAgentInstance == null) {
                LOGGER.warn("Leawind: CAMERA_AGENT is null at init - will retry on first use");
            }

            // Step 3: Get CameraAgent class
            Class<?> cameraAgentClass = Class.forName(
                    "com.github.leawind.thirdperson.core.CameraAgent"
            );

            // Step 4: Get Vector2d class (JOML - already on classpath)
            vector2dClass = Class.forName("org.joml.Vector2d");

            // Step 5: Cache the relativeRotation field and its x/y accessors
            // relativeRotation is private in CameraAgent - need getDeclaredField
            relativeRotationField = cameraAgentClass.getDeclaredField("relativeRotation");
            relativeRotationField.setAccessible(true);

            // x and y are public fields on Vector2d
            vec2dX = vector2dClass.getField("x");
            vec2dY = vector2dClass.getField("y");
            // Get EntityAgent
            Field entityAgentField = thirdPersonClass.getDeclaredField("ENTITY_AGENT");
            entityAgentField.setAccessible(true);
            entityAgentInstance = entityAgentField.get(null);

            Class<?> entityAgentClass = Class.forName(
                    "com.github.leawind.thirdperson.core.EntityAgent"
            );

            // Get RotateTargetEnum and SmoothTypeEnum
            Class<?> rotateTargetClass = Class.forName(
                    "com.github.leawind.thirdperson.core.rotation.RotateTargetEnum"
            );
            Class<?> smoothTypeClass = Class.forName(
                    "com.github.leawind.thirdperson.core.rotation.SmoothTypeEnum"
            );

            // Cache the enum values we need
            cameraRotationEnum = Enum.valueOf(
                    (Class<Enum>) rotateTargetClass, "CAMERA_ROTATION"
            );
            hardSmoothEnum = Enum.valueOf(
                    (Class<Enum>) smoothTypeClass, "HARD"
            );

            // Cache the setter methods
            setRotateTargetMethod = entityAgentClass.getMethod(
                    "setRotateTarget", rotateTargetClass
            );
            setRotationSmoothTypeMethod = entityAgentClass.getMethod(
                    "setRotationSmoothType", smoothTypeClass
            );

            initialized = true;
            LOGGER.info("Leawind compat initialized - relativeRotation field cached");

        } catch (Exception e) {
            LOGGER.warn("Leawind compat init failed: {}", e.getMessage());
            initialized = false;
        }
    }

    /**
     * Retry getting CAMERA_AGENT if it was null at init time
     * (mod might not have finished loading when initialize() was called)
     */
    private static Object getCameraAgent() {
        if (cameraAgentInstance != null) return cameraAgentInstance;
        try {
            Class<?> thirdPersonClass = Class.forName(
                    "com.github.leawind.thirdperson.ThirdPerson"
            );
            Field f = thirdPersonClass.getDeclaredField("CAMERA_AGENT");
            f.setAccessible(true);
            cameraAgentInstance = f.get(null);
        } catch (Exception e) {
            LOGGER.debug("getCameraAgent retry failed: {}", e.getMessage());
        }
        return cameraAgentInstance;
    }

    /**
     * Set Leawind camera rotation by writing directly to relativeRotation.
     *
     * Leawind stores rotation as:
     *   relativeRotation.x = -pitch
     *   relativeRotation.y = yaw - 180
     *
     * We write directly here instead of using setRotation() because
     * onRenderTickStart() overwrites relativeRotation every frame when
     * shouldCameraTurnWithEntity() is false (free rotation mode).
     * Writing directly here (called from AFTER_PARTICLES stage) runs
     * AFTER Leawind's overwrite, so our values stick.
     */
    public static void setCameraRotation(float yaw, float pitch) {
        if (!initialized || relativeRotationField == null) return;
        try {
            Object agent = getCameraAgent();
            if (agent == null) return;
            Object relRot = relativeRotationField.get(agent);
            vec2dX.set(relRot, (double) -pitch);       // relativeRotation.x = -pitch
            vec2dY.set(relRot, (double) (yaw - 180.0)); // relativeRotation.y = yaw - 180
        } catch (Exception e) {
            LOGGER.debug("Leawind setCameraRotation failed: {}", e.getMessage());
        }
    }

    /**
     * Read current Leawind camera rotation from relativeRotation.
     * Returns float[]{yaw, pitch}
     */
    public static float[] getCameraRotation() {
        if (!initialized || relativeRotationField == null) return new float[]{0f, 0f};
        try {
            Object agent = getCameraAgent();
            if (agent == null) return new float[]{0f, 0f};
            Object relRot = relativeRotationField.get(agent);
            double x = (double) vec2dX.get(relRot); // = -pitch
            double y = (double) vec2dY.get(relRot); // = yaw - 180
            return new float[]{(float) (y + 180), (float) (-x)}; // {yaw, pitch}
        } catch (Exception e) {
            LOGGER.debug("Leawind getCameraRotation failed: {}", e.getMessage());
        }
        return new float[]{0f, 0f};
    }


    /**
     * Force Leawind into "camera follows entity" mode.
     * This makes shouldCameraTurnWithEntity() return true,
     * which means Leawind syncs relativeRotation to player rotation
     * automatically every render tick.
     */
    public static void forceCameraFollowEntity() {
        if (!initialized || setRotateTargetMethod == null) return;
        try {
            Object agent = getEntityAgent();
            if (agent == null) return;
            setRotateTargetMethod.invoke(agent, cameraRotationEnum);
            setRotationSmoothTypeMethod.invoke(agent, hardSmoothEnum);
        } catch (Exception e) {
            LOGGER.debug("forceCameraFollowEntity failed: {}", e.getMessage());
        }
    }

    private static Object getEntityAgent() {
        if (entityAgentInstance != null) return entityAgentInstance;
        try {
            Class<?> thirdPersonClass = Class.forName(
                    "com.github.leawind.thirdperson.ThirdPerson"
            );
            Field f = thirdPersonClass.getDeclaredField("ENTITY_AGENT");
            f.setAccessible(true);
            entityAgentInstance = f.get(null);
        } catch (Exception e) {
            LOGGER.debug("getEntityAgent retry failed: {}", e.getMessage());
        }
        return entityAgentInstance;
    }

    private static void refreshCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_DURATION_MS) return;
        lastCacheUpdate = now;
        cachedEnabled = loaded;
    }

    public static boolean isLoaded() { return loaded; }
    public static boolean isInitialized() { return initialized; }

    public static boolean isEnabled() {
        if (!loaded) return false;
        refreshCache();
        return cachedEnabled;
    }

    public static double getTargetingRangeMultiplier() { return isEnabled() ? 1.2 : 1.0; }
    public static double getTargetingAngleMultiplier() { return isEnabled() ? 1.1 : 1.0; }
    public static float getIndicatorSizeMultiplier()   { return isEnabled() ? 1.1f : 1.0f; }
    public static float getRotationSpeedMultiplier()   { return 1.0f; }

    public static String getStatusInfo() {
        if (!loaded) return "Not Loaded";
        if (!initialized) return "Loaded (reflection failed)";
        return "Loaded | Initialized: true | Agent: " + (cameraAgentInstance != null ? "OK" : "null");
    }
}