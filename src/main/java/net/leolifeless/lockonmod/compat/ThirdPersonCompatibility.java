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
 * Fixed compatibility handler for Leawind's Third Person mod
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
    private static Field configEnabledField;

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
     * Initialize compatibility features using reflection with better error handling
     */
    private static void initializeCompatibility() {
        try {
            // Try the most common class paths for different versions
            String[] possibleConfigClasses = {
                    "com.github.leawind.thirdperson.config.Config",
                    "com.github.leawind.thirdperson.Config",
                    "leawind.thirdperson.config.Config",
                    "leawind.thirdperson.Config"
            };

            // Try to find the config class
            for (String className : possibleConfigClasses) {
                try {
                    thirdPersonConfigClass = Class.forName(className);
                    LOGGER.info("Found third person config class: {}", className);
                    break;
                } catch (ClassNotFoundException e) {
                    // Try next class name
                }
            }

            if (thirdPersonConfigClass == null) {
                LOGGER.warn("Could not find third person config class, using basic detection");
                isInitialized = false;
                return;
            }

            // Try to find available methods - be flexible about method names
            String[] possibleEnabledMethods = {
                    "isModEnabled",
                    "isEnabled",
                    "isAvailable",
                    "enabled"
            };

            for (String methodName : possibleEnabledMethods) {
                try {
                    isModEnabledMethod = thirdPersonConfigClass.getMethod(methodName);
                    LOGGER.info("Found enabled method: {}", methodName);
                    break;
                } catch (NoSuchMethodException e) {
                    // Try next method name
                }
            }

            // Try to find enabled field if no method found
            if (isModEnabledMethod == null) {
                String[] possibleEnabledFields = {
                        "enabled",
                        "isEnabled",
                        "modEnabled"
                };

                for (String fieldName : possibleEnabledFields) {
                    try {
                        configEnabledField = thirdPersonConfigClass.getField(fieldName);
                        LOGGER.info("Found enabled field: {}", fieldName);
                        break;
                    } catch (NoSuchFieldException e) {
                        // Try next field name
                    }
                }
            }

            isInitialized = (isModEnabledMethod != null || configEnabledField != null);

            if (isInitialized) {
                LOGGER.info("Third person compatibility initialized successfully");
            } else {
                LOGGER.warn("Could not find enabled method/field, using basic camera detection");
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to initialize third person compatibility - using basic integration: {}", e.getMessage());
            LOGGER.warn("This might be due to version differences in Leawind's Third Person mod");
            isInitialized = false;
        }
    }

    /**
     * Check if the third person mod is currently active and enabled
     */
    public static boolean isThirdPersonActive() {
        if (!isThirdPersonModLoaded) {
            return false;
        }

        try {
            // Method 1: Try to check via config if available
            if (isInitialized) {
                if (isModEnabledMethod != null) {
                    Boolean modEnabled = (Boolean) isModEnabledMethod.invoke(null);
                    if (modEnabled == null || !modEnabled) {
                        return false;
                    }
                } else if (configEnabledField != null) {
                    Boolean modEnabled = (Boolean) configEnabledField.get(null);
                    if (modEnabled == null || !modEnabled) {
                        return false;
                    }
                }
            }

            // Method 2: Always check camera perspective as fallback
            Minecraft mc = Minecraft.getInstance();
            if (mc.options == null) return false;

            // Check if we're in third person view
            boolean isThirdPerson = !mc.options.getCameraType().isFirstPerson();

            return isThirdPerson;

        } catch (Exception e) {
            LOGGER.debug("Error checking third person state, using basic detection: {}", e.getMessage());

            // Fallback: just check camera type
            try {
                Minecraft mc = Minecraft.getInstance();
                return mc.options != null && !mc.options.getCameraType().isFirstPerson();
            } catch (Exception fallbackError) {
                return false;
            }
        }
    }

    /**
     * Get the current camera offset from third person mod (with fallback)
     */
    public static Vec3 getThirdPersonCameraOffset() {
        if (!isThirdPersonActive()) {
            return Vec3.ZERO;
        }

        // Since we can't reliably get the exact offset, provide a reasonable default
        // This is based on typical third person camera distances
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Estimate based on camera type
            switch (mc.options.getCameraType()) {
                case THIRD_PERSON_BACK:
                    return new Vec3(0, 0, -4.0); // Behind player
                case THIRD_PERSON_FRONT:
                    return new Vec3(0, 0, 4.0);  // In front of player
                default:
                    return Vec3.ZERO;
            }
        }

        return Vec3.ZERO;
    }

    /**
     * Calculate enhanced targeting range based on third person perspective
     */
    public static double getAdjustedTargetingRange(double baseRange) {
        if (!isThirdPersonActive()) {
            return baseRange;
        }

        // Increase range in third person for better UX
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

    /**
     * Debug method to get detailed information
     */
    public static String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Third Person Compatibility Status:\n");
        sb.append("- Mod Loaded: ").append(isThirdPersonModLoaded).append("\n");
        sb.append("- Initialized: ").append(isInitialized).append("\n");
        sb.append("- Currently Active: ").append(isThirdPersonActive()).append("\n");
        sb.append("- Config Class Found: ").append(thirdPersonConfigClass != null ? thirdPersonConfigClass.getName() : "None").append("\n");
        sb.append("- Enabled Method Found: ").append(isModEnabledMethod != null ? isModEnabledMethod.getName() : "None").append("\n");
        sb.append("- Enabled Field Found: ").append(configEnabledField != null ? configEnabledField.getName() : "None").append("\n");

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) {
                sb.append("- Camera Type: ").append(mc.options.getCameraType()).append("\n");
            }
        } catch (Exception e) {
            sb.append("- Camera Type: Error getting camera type\n");
        }

        return sb.toString();
    }
}