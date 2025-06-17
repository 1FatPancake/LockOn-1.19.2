package net.leolifeless.lockonmod.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Compatibility handler for Leawind's Third Person mod
 * Provides enhanced lock-on functionality when third person mod is present
 */
public class ThirdPersonCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String THIRD_PERSON_MOD_ID = "leawind_third_person";

    private static boolean isThirdPersonModLoaded = false;
    private static boolean isInitialized = false;

    // Reflection fields/methods for third person mod integration
    private static Class<?> thirdPersonConfigClass;
    private static Class<?> thirdPersonCameraClass;
    private static Method isModEnabledMethod;
    private static Method getCameraOffsetMethod;
    private static Method isFlyingCenteredMethod;
    private static Field perspectiveField;

    static {
        checkModPresence();
        if (isThirdPersonModLoaded) {
            initializeCompatibility();
        }
    }

    /**
     * Check if Leawind's Third Person mod is loaded
     */
    private static void checkModPresence() {
        isThirdPersonModLoaded = ModList.get().isLoaded(THIRD_PERSON_MOD_ID);
        if (isThirdPersonModLoaded) {
            LOGGER.info("Leawind's Third Person mod detected - enabling enhanced compatibility");
        }
    }

    /**
     * Initialize compatibility features using reflection
     */
    private static void initializeCompatibility() {
        try {
            // Try to access third person mod classes
            thirdPersonConfigClass = Class.forName("com.github.leawind.thirdperson.config.Config");
            thirdPersonCameraClass = Class.forName("com.github.leawind.thirdperson.core.CameraAgent");

            // Get methods for checking mod state
            isModEnabledMethod = thirdPersonConfigClass.getMethod("isModEnabled");
            getCameraOffsetMethod = thirdPersonCameraClass.getMethod("getCameraOffset");
            isFlyingCenteredMethod = thirdPersonConfigClass.getMethod("centerOffsetWhenFlying");

            // Get perspective field (if available)
            try {
                Class<?> minecraft = Minecraft.class;
                perspectiveField = minecraft.getDeclaredField("options");
            } catch (NoSuchFieldException e) {
                LOGGER.debug("Could not access perspective field - using alternative methods");
            }

            isInitialized = true;
            LOGGER.info("Third person compatibility initialized successfully");

        } catch (ClassNotFoundException | NoSuchMethodException e) {
            LOGGER.warn("Failed to initialize third person compatibility - using basic integration: {}", e.getMessage());
            isInitialized = false;
        }
    }

    /**
     * Check if the third person mod is currently active and enabled
     */
    public static boolean isThirdPersonActive() {
        if (!isThirdPersonModLoaded || !isInitialized) {
            return false;
        }

        try {
            // Check if mod is enabled in config
            Boolean modEnabled = (Boolean) isModEnabledMethod.invoke(null);
            if (modEnabled == null || !modEnabled) {
                return false;
            }

            // Check if we're actually in third person view
            Minecraft mc = Minecraft.getInstance();
            return mc.options.getCameraType().isFirstPerson() == false;

        } catch (Exception e) {
            LOGGER.debug("Error checking third person state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the current camera offset from third person mod
     */
    public static Vec3 getThirdPersonCameraOffset() {
        if (!isThirdPersonActive()) {
            return Vec3.ZERO;
        }

        try {
            Object offset = getCameraOffsetMethod.invoke(null);
            if (offset instanceof Vec3) {
                return (Vec3) offset;
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting camera offset: {}", e.getMessage());
        }

        return Vec3.ZERO;
    }

    /**
     * Check if camera is centered due to flying (elytra)
     */
    public static boolean isCameraCenteredForFlying() {
        if (!isThirdPersonActive()) {
            return false;
        }

        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return false;

            // Check if player is flying with elytra
            boolean isFlying = player.isFallFlying();

            // Check if third person mod centers camera when flying
            Boolean centerWhenFlying = (Boolean) isFlyingCenteredMethod.invoke(null);

            return isFlying && (centerWhenFlying != null && centerWhenFlying);

        } catch (Exception e) {
            LOGGER.debug("Error checking flying state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calculate enhanced targeting range based on third person perspective
     */
    public static double getAdjustedTargetingRange(double baseRange) {
        if (!isThirdPersonActive()) {
            return baseRange;
        }

        // Increase range slightly in third person for better UX
        Vec3 cameraOffset = getThirdPersonCameraOffset();
        double offsetDistance = cameraOffset.length();

        // Add 20% to base range plus the camera offset distance
        return baseRange * 1.2 + offsetDistance;
    }

    /**
     * Adjust targeting angle for third person perspective
     */
    public static double getAdjustedTargetingAngle(double baseAngle) {
        if (!isThirdPersonActive()) {
            return baseAngle;
        }

        // Slightly increase targeting angle in third person for easier targeting
        return Math.min(baseAngle * 1.3, 180.0);
    }

    /**
     * Get enhanced target position for rendering indicators in third person
     */
    public static Vec3 getAdjustedTargetPosition(Entity target, Vec3 basePosition) {
        if (!isThirdPersonActive() || target == null) {
            return basePosition;
        }

        try {
            // Account for camera offset when positioning indicators
            Vec3 cameraOffset = getThirdPersonCameraOffset();

            // Slightly adjust indicator position to account for perspective
            return basePosition.add(cameraOffset.scale(0.1));

        } catch (Exception e) {
            return basePosition;
        }
    }

    /**
     * Check if we should use enhanced smoothing in third person
     */
    public static boolean shouldUseEnhancedSmoothing() {
        return isThirdPersonActive();
    }

    /**
     * Get camera smoothing factor for third person
     */
    public static float getThirdPersonSmoothingFactor(float baseFactor) {
        if (!isThirdPersonActive()) {
            return baseFactor;
        }

        // Use slightly more smoothing in third person for better camera feel
        return Math.min(baseFactor * 1.15f, 1.0f);
    }

    /**
     * Adjust rotation speed based on third person state
     */
    public static float getAdjustedRotationSpeed(float baseSpeed) {
        if (!isThirdPersonActive()) {
            return baseSpeed;
        }

        // Slightly reduce rotation speed in third person to prevent disorientation
        return baseSpeed * 0.85f;
    }

    /**
     * Check if we should adjust indicator size for third person
     */
    public static float getAdjustedIndicatorSize(float baseSize) {
        if (!isThirdPersonActive()) {
            return baseSize;
        }

        Vec3 cameraOffset = getThirdPersonCameraOffset();
        double distance = cameraOffset.length();

        // Scale indicator slightly based on camera distance
        float scaleFactor = 1.0f + (float)(distance * 0.05);
        return baseSize * Math.min(scaleFactor, 1.5f);
    }

    /**
     * Get mod compatibility status for display
     */
    public static String getCompatibilityStatus() {
        if (!isThirdPersonModLoaded) {
            return "Third Person Mod: Not Detected";
        } else if (!isInitialized) {
            return "Third Person Mod: Detected (Basic Compatibility)";
        } else if (isThirdPersonActive()) {
            return "Third Person Mod: Active (Enhanced Features)";
        } else {
            return "Third Person Mod: Detected (Inactive)";
        }
    }

    /**
     * Public getter for mod loaded status
     */
    public static boolean isModLoaded() {
        return isThirdPersonModLoaded;
    }

    /**
     * Public getter for initialization status
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
}