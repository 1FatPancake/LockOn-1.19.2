package net.leolifeless.lockonmod.compat;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfingCamera;
import com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing;
import com.mojang.logging.LogUtils;
import net.leolifeless.lockonmod.LockOnSystem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Shoulder Surfing Reloaded compatibility using the official API.
 * Package: com.github.exopandora.shouldersurfing.api
 *
 * Key fixes:
 * 1. ICameraCouplingCallback (via ShoulderSurfingPlugin + service file) forces
 *    camera coupling while lock-on is active, so player body follows camera.
 * 2. setCameraRotation() sets pitch directly on the SS camera object to bypass
 *    the follow_player_rotations_delay (40 tick delay) that was fighting our pitch.
 */
public class ShoulderSurfingCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "shouldersurfing";

    public static void initialize() {
        if (!isLoaded()) {
            LOGGER.info("Shoulder Surfing not present, skipping");
            return;
        }
        LOGGER.info("Shoulder Surfing detected - using official Plugin API (ICameraCouplingCallback)");
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

    public static boolean isCameraDecoupled() {
        if (!isLoaded()) return false;
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            return api != null && api.isCameraDecoupled(); // removed the !
        } catch (Exception e) {
            LOGGER.debug("isCameraDecoupled() failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Called by ICameraCouplingCallback — forces coupling while lock-on has a target.
     */
    public static boolean shouldForceCoupling() {
        return LockOnSystem.hasTarget();
    }

    /**
     * Sets camera yaw AND pitch directly on the SS camera object.
     *
     * Why this is needed:
     * SS has a config option follow_player_rotations_delay (default 40 ticks).
     * When coupling is active, SS copies player rotation to camera but with a delay.
     * Setting player.setXRot() therefore gets delayed/overridden by SS each frame.
     * Going directly to IShoulderSurfingCamera.setXRot() bypasses this delay entirely.
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

    public static Vec3 getCameraRenderOffset() {
        if (!isLoaded()) return Vec3.ZERO;
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            if (api != null) {
                IShoulderSurfingCamera camera = api.getCamera();
                if (camera != null) {
                    Vec3 offset = camera.getRenderOffset();
                    return offset != null ? offset : Vec3.ZERO;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("getCameraRenderOffset failed: {}", e.getMessage());
        }
        return Vec3.ZERO;
    }

    public static Vec3 getCameraTargetOffset() {
        if (!isLoaded()) return Vec3.ZERO;
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            if (api != null) {
                IShoulderSurfingCamera camera = api.getCamera();
                if (camera != null) {
                    Vec3 offset = camera.getTargetOffset();
                    return offset != null ? offset : Vec3.ZERO;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("getCameraTargetOffset failed: {}", e.getMessage());
        }
        return Vec3.ZERO;
    }

    /**
     * Sets camera pitch only — used as fallback if yaw is handled elsewhere.
     */
    public static void forceCameraPitch(float pitch) {
        if (!isLoaded()) return;
        try {
            IShoulderSurfing api = ShoulderSurfing.getInstance();
            if (api == null) return;
            IShoulderSurfingCamera camera = api.getCamera();
            if (camera == null) return;
            camera.setXRot(pitch);
        } catch (Exception e) {
            LOGGER.debug("forceCameraPitch failed: {}", e.getMessage());
        }
    }

    // === Multipliers for ThirdPersonCompatibility ===

    public static void enableLockOnMode() {
        // Handled automatically by ICameraCouplingCallback
    }

    public static void disableLockOnMode() {
        // Handled automatically by ICameraCouplingCallback
    }

    public static boolean isLockOnModeActive() {
        return LockOnSystem.hasTarget();
    }

    public static double getTargetingRangeMultiplier() {
        return isActive() ? 1.2 : 1.0;
    }

    public static double getTargetingAngleMultiplier() {
        return isActive() ? 1.1 : 1.0;
    }

    public static float getIndicatorSizeMultiplier() {
        return isActive() ? 1.15f : 1.0f;
    }

    public static float getRotationSpeedMultiplier() {
        return 1.0f;
    }

    public static String getStatusInfo() {
        if (!isLoaded()) return "Not Loaded";
        return "Loaded | Active: " + isActive() + " | Decoupled: " + isCameraDecoupled();
    }
}