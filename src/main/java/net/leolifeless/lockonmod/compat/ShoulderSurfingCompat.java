package net.leolifeless.lockonmod.compat;

import com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfingCamera;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shoulder Surfing Reloaded compatibility for 1.16.5 using the official API.
 * Package: com.github.exopandora.shouldersurfing.api
 *
 * Differences from 1.20.1:
 * - Uses Vector3d instead of Vec3
 * - No isCameraDecoupled() method on this version
 */
public class ShoulderSurfingCompat {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "shouldersurfing";

    public static void initialize() {
        if (!isLoaded()) {
            LOGGER.info("Shoulder Surfing not present, skipping");
            return;
        }
        LOGGER.info("Shoulder Surfing detected - using official API");
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isInitialized() {
        return isLoaded();
    }

    public static boolean isActive() {
        if (!isLoaded()) return false;
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            return api != null && api.isShoulderSurfing();
        } catch (Exception e) {
            LOGGER.debug("isActive() failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sets camera yaw and pitch directly on the SS camera object.
     * On 1.16.5 there is no ICameraCouplingCallback so we drive the
     * camera rotation directly every render frame instead.
     */
    public static void setCameraRotation(float yaw, float pitch) {
        if (!isLoaded()) return;
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            if (api != null) {
                IShoulderSurfingCamera camera = api.getCamera();
                if (camera != null) {
                    camera.setYRot(yaw);
                    camera.setXRot(pitch);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("setCameraRotation failed: {}", e.getMessage());
        }
    }

    public static float[] getCameraRotation() {
        if (!isLoaded()) return new float[]{0f, 0f};
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            if (api != null) {
                IShoulderSurfingCamera camera = api.getCamera();
                if (camera != null) {
                    return new float[]{camera.getYRot(), camera.getXRot()};
                }
            }
        } catch (Exception e) {
            LOGGER.debug("getCameraRotation failed: {}", e.getMessage());
        }
        return new float[]{0f, 0f};
    }

    public static Vector3d getCameraRenderOffset() {
        if (!isLoaded()) return Vector3d.ZERO;
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            if (api != null) {
                IShoulderSurfingCamera camera = api.getCamera();
                if (camera != null) {
                    Vector3d offset = camera.getRenderOffset();
                    return offset != null ? offset : Vector3d.ZERO;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("getCameraRenderOffset failed: {}", e.getMessage());
        }
        return Vector3d.ZERO;
    }

    public static Vector3d getCameraTargetOffset() {
        if (!isLoaded()) return Vector3d.ZERO;
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            if (api != null) {
                IShoulderSurfingCamera camera = api.getCamera();
                if (camera != null) {
                    Vector3d offset = camera.getTargetOffset();
                    return offset != null ? offset : Vector3d.ZERO;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("getCameraTargetOffset failed: {}", e.getMessage());
        }
        return Vector3d.ZERO;
    }

    public static double getTargetingRangeMultiplier() { return isActive() ? 1.2 : 1.0; }
    public static double getTargetingAngleMultiplier() { return isActive() ? 1.1 : 1.0; }
    public static float  getIndicatorSizeMultiplier()  { return isActive() ? 1.15f : 1.0f; }
    public static float  getRotationSpeedMultiplier()  { return 1.0f; }

    public static String getStatusInfo() {
        if (!isLoaded()) return "Not Loaded";
        return "Loaded | Active: " + isActive();
    }
}