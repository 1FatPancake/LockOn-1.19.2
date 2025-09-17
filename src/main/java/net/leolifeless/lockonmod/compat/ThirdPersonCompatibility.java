package net.leolifeless.lockonmod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Enhanced compatibility handler for multiple third person mods (1.16.5)
 * - Shoulder Surfing Reloaded (Enhanced Support - most common for 1.16.5)
 * - Better Third Person (Basic Support)
 * - Minecraft Vanilla Third Person (Basic Support)
 *
 * Note: Leawind's Third Person wasn't available for 1.16.5
 */
public class ThirdPersonCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();

    // Supported mod IDs for 1.16.5
    private static final String SHOULDER_SURFING_MOD_ID = "shouldersurfing";
    private static final String BETTER_THIRD_PERSON_MOD_ID = "betterthirdperson";

    // Mod detection flags
    private static boolean isShoulderSurfingLoaded = false;
    private static boolean isBetterThirdPersonLoaded = false;

    // Initialization flags
    private static boolean isShoulderSurfingInitialized = false;
    private static boolean isBetterThirdPersonInitialized = false;

    // Current active mod
    private static ActiveThirdPersonMod activeMod = ActiveThirdPersonMod.NONE;

    // Reflection for Shoulder Surfing Reloaded (1.16.5 versions)
    private static Class<?> shoulderSurfingConfigClass;
    private static Class<?> shoulderSurfingApiClass;
    private static Method shoulderSurfingIsEnabledMethod;
    private static Method shoulderSurfingGetOffsetMethod;
    private static Method shoulderSurfingIsActiveMethod;

    // Reflection for Better Third Person (1.16.5 versions)
    private static Class<?> betterThirdPersonConfigClass;
    private static Method betterThirdPersonGetConfigMethod;
    private static Field betterThirdPersonPlayerRotationField;

    // Performance optimization cache
    private static Vector3d cachedCameraOffset = Vector3d.ZERO;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 16; // ~1 frame at 60fps

    public enum ActiveThirdPersonMod {
        NONE,
        SHOULDER_SURFING,
        BETTER_THIRD_PERSON,
        MINECRAFT_VANILLA
    }

    static {
        detectAndInitializeMods();
    }

    /**
     * Detect and initialize all supported third person mods for 1.16.5
     */
    private static void detectAndInitializeMods() {
        try {
            // Check which mods are loaded
            isShoulderSurfingLoaded = ModList.get().isLoaded(SHOULDER_SURFING_MOD_ID);
            isBetterThirdPersonLoaded = ModList.get().isLoaded(BETTER_THIRD_PERSON_MOD_ID);

            LOGGER.info("Third Person Mod Detection (1.16.5):");
            LOGGER.info("  Shoulder Surfing Reloaded: {}", isShoulderSurfingLoaded ? "FOUND" : "Not Found");
            LOGGER.info("  Better Third Person: {}", isBetterThirdPersonLoaded ? "FOUND" : "Not Found");

            // Initialize each mod with safe error handling
            if (isShoulderSurfingLoaded) {
                initializeShoulderSurfing();
            }
            if (isBetterThirdPersonLoaded) {
                initializeBetterThirdPerson();
            }
        } catch (Exception e) {
            LOGGER.warn("Error during third person mod detection: {}", e.getMessage());
            enableEmergencyMode();
        }
    }

    /**
     * Initialize Shoulder Surfing Reloaded compatibility for 1.16.5
     */
    private static void initializeShoulderSurfing() {
        try {
            // Try multiple possible class paths for Shoulder Surfing in 1.16.5
            String[] possibleConfigClasses = {
                    "com.teamderpy.shouldersurfing.config.Config",
                    "shouldersurfing.config.Config",
                    "shouldersurfing.Config",
                    "com.exopandora.shouldersurfing.config.Config",
                    "com.exopandora.shouldersurfing.Config"
            };

            String[] possibleApiClasses = {
                    "com.teamderpy.shouldersurfing.ShoulderSurfing",
                    "shouldersurfing.ShoulderSurfing",
                    "shouldersurfing.api.ShoulderSurfingAPI",
                    "com.exopandora.shouldersurfing.ShoulderSurfing",
                    "com.exopandora.shouldersurfing.api.ShoulderSurfingAPI"
            };

            // Try to find config class
            for (String className : possibleConfigClasses) {
                try {
                    shoulderSurfingConfigClass = Class.forName(className);
                    LOGGER.debug("Found Shoulder Surfing config class: {}", className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            // Try to find API class
            for (String className : possibleApiClasses) {
                try {
                    shoulderSurfingApiClass = Class.forName(className);
                    LOGGER.debug("Found Shoulder Surfing API class: {}", className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            if (shoulderSurfingConfigClass != null || shoulderSurfingApiClass != null) {
                setupShoulderSurfingMethods();
                isShoulderSurfingInitialized = true;
                LOGGER.info("Shoulder Surfing initialization: SUCCESS");
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to initialize Shoulder Surfing compatibility: {}", e.getMessage());
            isShoulderSurfingInitialized = false;
        }
    }

    /**
     * Setup Shoulder Surfing reflection methods for 1.16.5
     */
    private static void setupShoulderSurfingMethods() {
        Class<?> targetClass = shoulderSurfingApiClass != null ? shoulderSurfingApiClass : shoulderSurfingConfigClass;

        String[] possibleEnabledMethods = {"isEnabled", "isActive", "isShoulderSurfingEnabled", "enabled"};
        String[] possibleOffsetMethods = {"getCameraOffset", "getOffset", "getShoulderOffset", "getCameraPosition"};
        String[] possibleActiveMethods = {"isThirdPersonEnabled", "isActive", "isInThirdPerson"};

        // Try to find enabled method
        for (String methodName : possibleEnabledMethods) {
            try {
                shoulderSurfingIsEnabledMethod = targetClass.getMethod(methodName);
                LOGGER.debug("Found Shoulder Surfing enabled method: {}", methodName);
                break;
            } catch (NoSuchMethodException ignored) {}
        }

        // Try to find offset method
        for (String methodName : possibleOffsetMethods) {
            try {
                shoulderSurfingGetOffsetMethod = targetClass.getMethod(methodName);
                LOGGER.debug("Found Shoulder Surfing offset method: {}", methodName);
                break;
            } catch (NoSuchMethodException ignored) {}
        }

        // Try to find active method
        for (String methodName : possibleActiveMethods) {
            try {
                shoulderSurfingIsActiveMethod = targetClass.getMethod(methodName);
                LOGGER.debug("Found Shoulder Surfing active method: {}", methodName);
                break;
            } catch (NoSuchMethodException ignored) {}
        }
    }

    /**
     * Initialize Better Third Person compatibility for 1.16.5
     */
    private static void initializeBetterThirdPerson() {
        try {
            // Try multiple possible class paths for Better Third Person in 1.16.5
            String[] possibleClasses = {
                    "betterthirdperson.config.Config",
                    "betterthirdperson.Config",
                    "com.betterthirdperson.config.Config",
                    "com.betterthirdperson.Config",
                    "net.betterthirdperson.config.Config",
                    "net.betterthirdperson.Config"
            };

            for (String className : possibleClasses) {
                try {
                    betterThirdPersonConfigClass = Class.forName(className);
                    LOGGER.debug("Found Better Third Person config class: {}", className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            if (betterThirdPersonConfigClass != null) {
                setupBetterThirdPersonMethods();
                isBetterThirdPersonInitialized = true;
                LOGGER.info("Better Third Person initialization: SUCCESS");
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to initialize Better Third Person compatibility: {}", e.getMessage());
            isBetterThirdPersonInitialized = false;
        }
    }

    /**
     * Setup Better Third Person reflection methods for 1.16.5
     */
    private static void setupBetterThirdPersonMethods() {
        // Try to find methods to control player rotation
        String[] possibleMethods = {
                "getConfig", "getInstance", "get", "getClientConfig"
        };

        for (String methodName : possibleMethods) {
            try {
                betterThirdPersonGetConfigMethod = betterThirdPersonConfigClass.getMethod(methodName);
                LOGGER.debug("Found Better Third Person config method: {}", methodName);
                break;
            } catch (NoSuchMethodException ignored) {}
        }

        // Try to find rotation control fields
        String[] possibleFields = {
                "playerRotation", "playerFollowsCamera", "lockPlayerRotation",
                "rotationLock", "followCamera", "playerLooksAtCursor"
        };

        for (String fieldName : possibleFields) {
            try {
                betterThirdPersonPlayerRotationField = betterThirdPersonConfigClass.getDeclaredField(fieldName);
                betterThirdPersonPlayerRotationField.setAccessible(true);
                LOGGER.debug("Found Better Third Person rotation field: {}", fieldName);
                break;
            } catch (NoSuchFieldException ignored) {}
        }
    }

    /**
     * Determine which third person mod is currently active for 1.16.5
     */
    private static ActiveThirdPersonMod detectActiveMod() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options == null) return ActiveThirdPersonMod.NONE;

            // Check if we're even in third person view (1.16.5 API)
            boolean isThirdPersonView = mc.options.getCameraType() != net.minecraft.client.settings.PointOfView.FIRST_PERSON;
            if (!isThirdPersonView) {
                return ActiveThirdPersonMod.NONE;
            }

            // Priority order: Shoulder Surfing -> Better Third Person -> Vanilla
            if (isShoulderSurfingLoaded && isShoulderSurfingEnabled()) {
                return ActiveThirdPersonMod.SHOULDER_SURFING;
            }

            if (isBetterThirdPersonLoaded) {
                return ActiveThirdPersonMod.BETTER_THIRD_PERSON;
            }

            // Fallback to vanilla third person
            return ActiveThirdPersonMod.MINECRAFT_VANILLA;

        } catch (Exception e) {
            LOGGER.debug("Error detecting active mod: {}", e.getMessage());
            return ActiveThirdPersonMod.NONE;
        }
    }

    /**
     * Check if Shoulder Surfing is enabled with safe error handling
     */
    private static boolean isShoulderSurfingEnabled() {
        if (!isShoulderSurfingLoaded) return false;

        try {
            if (isShoulderSurfingInitialized) {
                if (shoulderSurfingIsEnabledMethod != null) {
                    Boolean enabled = (Boolean) shoulderSurfingIsEnabledMethod.invoke(null);
                    return enabled != null && enabled;
                } else if (shoulderSurfingIsActiveMethod != null) {
                    Boolean active = (Boolean) shoulderSurfingIsActiveMethod.invoke(null);
                    return active != null && active;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking Shoulder Surfing enabled state: {}", e.getMessage());
        }

        return true; // Assume enabled if we can't check
    }

    // === PUBLIC API METHODS FOR 1.16.5 ===

    /**
     * Check if any third person mod is currently active with performance optimization
     */
    public static boolean isThirdPersonActive() {
        try {
            activeMod = detectActiveMod();
            return activeMod != ActiveThirdPersonMod.NONE;
        } catch (Exception e) {
            LOGGER.debug("Error in isThirdPersonActive: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the current camera offset from the active third person mod (1.16.5 Vector3d)
     */
    public static Vector3d getThirdPersonCameraOffset() {
        try {
            activeMod = detectActiveMod();

            switch (activeMod) {
                case SHOULDER_SURFING:
                    return getShoulderSurfingCameraOffset();
                case BETTER_THIRD_PERSON:
                    return getBetterThirdPersonCameraOffset();
                case MINECRAFT_VANILLA:
                    return getVanillaCameraOffset();
                default:
                    return Vector3d.ZERO;
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting camera offset: {}", e.getMessage());
            return Vector3d.ZERO;
        }
    }

    /**
     * Get performance-optimized camera offset (cached for multiple calls per frame)
     */
    public static Vector3d getCachedThirdPersonCameraOffset() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheTime > CACHE_DURATION_MS) {
            cachedCameraOffset = getThirdPersonCameraOffset();
            lastCacheTime = currentTime;
        }
        return cachedCameraOffset;
    }

    /**
     * Get camera offset from Shoulder Surfing with reflection fallback (1.16.5)
     */
    private static Vector3d getShoulderSurfingCameraOffset() {
        try {
            if (shoulderSurfingGetOffsetMethod != null) {
                Object offset = shoulderSurfingGetOffsetMethod.invoke(null);
                if (offset instanceof Vector3d) {
                    return (Vector3d) offset;
                }
                // Handle other possible return types that might exist in different versions
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting Shoulder Surfing camera offset: {}", e.getMessage());
        }

        // Shoulder Surfing typically uses over-the-shoulder view
        return new Vector3d(1.5, 0, -3.0); // Offset to the side and back
    }

    /**
     * Get camera offset from Better Third Person with safe fallback (1.16.5)
     */
    private static Vector3d getBetterThirdPersonCameraOffset() {
        // Better Third Person - basic estimate
        return new Vector3d(0, 0, -4.0);
    }

    /**
     * Get camera offset from vanilla Minecraft with safe camera type checking (1.16.5)
     */
    private static Vector3d getVanillaCameraOffset() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) {
                switch (mc.options.getCameraType()) {
                    case THIRD_PERSON_BACK:
                        return new Vector3d(0, 0, -4.0);
                    case THIRD_PERSON_FRONT:
                        return new Vector3d(0, 0, 4.0);
                    default:
                        return Vector3d.ZERO;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting vanilla camera offset: {}", e.getMessage());
        }
        return Vector3d.ZERO;
    }

    /**
     * Calculate enhanced targeting range based on active third person mod
     */
    public static double getAdjustedTargetingRange(double baseRange) {
        try {
            if (!isThirdPersonActive()) {
                return baseRange;
            }

            Vector3d cameraOffset = getCachedThirdPersonCameraOffset();
            double offsetDistance = cameraOffset.length();

            // Different multipliers for different mods
            switch (activeMod) {
                case SHOULDER_SURFING:
                    return baseRange * 1.3 + offsetDistance; // Shoulder surfing needs more range
                case BETTER_THIRD_PERSON:
                    return baseRange * 1.15 + offsetDistance;
                case MINECRAFT_VANILLA:
                    return baseRange * 1.1 + offsetDistance;
                default:
                    return baseRange;
            }
        } catch (Exception e) {
            LOGGER.debug("Error adjusting targeting range: {}", e.getMessage());
            return baseRange;
        }
    }

    /**
     * Adjust targeting angle for active third person mod with safe fallbacks
     */
    public static double getAdjustedTargetingAngle(double baseAngle) {
        try {
            if (!isThirdPersonActive()) {
                return baseAngle;
            }

            // Different angle adjustments for different mods
            switch (activeMod) {
                case SHOULDER_SURFING:
                    return Math.min(baseAngle * 1.4, 180.0); // Shoulder surfing needs wider angle
                case BETTER_THIRD_PERSON:
                    return Math.min(baseAngle * 1.2, 180.0);
                case MINECRAFT_VANILLA:
                    return Math.min(baseAngle * 1.15, 180.0);
                default:
                    return baseAngle;
            }
        } catch (Exception e) {
            LOGGER.debug("Error adjusting targeting angle: {}", e.getMessage());
            return baseAngle;
        }
    }

    /**
     * Get enhanced target position for rendering with safe error handling (1.16.5)
     */
    public static Vector3d getAdjustedTargetPosition(Entity target, Vector3d basePosition) {
        try {
            if (!isThirdPersonActive() || target == null) {
                return basePosition;
            }

            Vector3d cameraOffset = getCachedThirdPersonCameraOffset();

            // Adjust based on active mod
            switch (activeMod) {
                case SHOULDER_SURFING:
                    // Shoulder surfing needs special handling due to offset view
                    return basePosition.add(cameraOffset.scale(0.15));
                case BETTER_THIRD_PERSON:
                case MINECRAFT_VANILLA:
                default:
                    return basePosition.add(cameraOffset.scale(0.1));
            }
        } catch (Exception e) {
            LOGGER.debug("Error adjusting target position: {}", e.getMessage());
            return basePosition;
        }
    }

    /**
     * Check if we should use enhanced smoothing with safe fallback
     */
    public static boolean shouldUseEnhancedSmoothing() {
        try {
            return isThirdPersonActive();
        } catch (Exception e) {
            LOGGER.debug("Error checking enhanced smoothing: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get smoothing factor for active third person mod with safe calculations
     */
    public static float getThirdPersonSmoothingFactor(float baseFactor) {
        try {
            if (!isThirdPersonActive()) {
                return baseFactor;
            }

            // Different smoothing for different mods
            switch (activeMod) {
                case SHOULDER_SURFING:
                    return Math.min(baseFactor * 1.2f, 1.0f); // More smoothing for shoulder surfing
                case BETTER_THIRD_PERSON:
                case MINECRAFT_VANILLA:
                default:
                    return Math.min(baseFactor * 1.1f, 1.0f);
            }
        } catch (Exception e) {
            LOGGER.debug("Error calculating smoothing factor: {}", e.getMessage());
            return baseFactor;
        }
    }

    /**
     * Adjust rotation speed for active third person mod with safe calculations
     */
    public static float getAdjustedRotationSpeed(float baseSpeed) {
        try {
            if (!isThirdPersonActive()) {
                return baseSpeed;
            }

            // Different speed adjustments for different mods
            switch (activeMod) {
                case SHOULDER_SURFING:
                    return baseSpeed * 0.8f; // Slower for shoulder surfing to prevent disorientation
                case BETTER_THIRD_PERSON:
                case MINECRAFT_VANILLA:
                default:
                    return baseSpeed * 0.9f;
            }
        } catch (Exception e) {
            LOGGER.debug("Error adjusting rotation speed: {}", e.getMessage());
            return baseSpeed;
        }
    }

    /**
     * Adjust indicator size for active third person mod with safe calculations
     */
    public static float getAdjustedIndicatorSize(float baseSize) {
        try {
            if (!isThirdPersonActive()) {
                return baseSize;
            }

            Vector3d cameraOffset = getCachedThirdPersonCameraOffset();
            double distance = cameraOffset.length();

            // Base scaling factor
            float scaleFactor = 1.0f + (float)(distance * 0.05);

            // Additional mod-specific adjustments
            switch (activeMod) {
                case SHOULDER_SURFING:
                    scaleFactor *= 1.1f; // Slightly larger for shoulder surfing
                    break;
                case BETTER_THIRD_PERSON:
                case MINECRAFT_VANILLA:
                default:
                    // Use base scaling
                    break;
            }

            return baseSize * Math.min(scaleFactor, 1.5f);
        } catch (Exception e) {
            LOGGER.debug("Error adjusting indicator size: {}", e.getMessage());
            return baseSize;
        }
    }

    // === STATUS AND DEBUG METHODS ===

    /**
     * Get comprehensive compatibility status with safe error handling
     */
    public static String getCompatibilityStatus() {
        try {
            StringBuilder status = new StringBuilder();

            if (!isShoulderSurfingLoaded && !isBetterThirdPersonLoaded) {
                status.append("Third Person Mods: None Detected (1.16.5)");
            } else {
                status.append("Third Person Mods (1.16.5): ");

                if (isShoulderSurfingLoaded) {
                    status.append("ShoulderSurfing(").append(isShoulderSurfingInitialized ? "Enhanced" : "Basic").append(") ");
                }
                if (isBetterThirdPersonLoaded) {
                    status.append("BetterThirdPerson(").append(isBetterThirdPersonInitialized ? "Enhanced" : "Basic").append(") ");
                }
            }

            activeMod = detectActiveMod();
            status.append("| Active: ").append(activeMod.name());

            return status.toString();
        } catch (Exception e) {
            LOGGER.debug("Error getting compatibility status: {}", e.getMessage());
            return "Third Person Mods: Error Detecting";
        }
    }

    /**
     * Get the currently active mod with safe error handling
     */
    public static ActiveThirdPersonMod getActiveMod() {
        try {
            return detectActiveMod();
        } catch (Exception e) {
            LOGGER.debug("Error getting active mod: {}", e.getMessage());
            return ActiveThirdPersonMod.NONE;
        }
    }

    /**
     * Check if any mod is loaded
     */
    public static boolean isModLoaded() {
        return isShoulderSurfingLoaded || isBetterThirdPersonLoaded;
    }

    /**
     * Check if any mod is initialized
     */
    public static boolean isInitialized() {
        return isShoulderSurfingInitialized || isBetterThirdPersonInitialized;
    }

    /**
     * Check if camera is centered due to flying (elytra) - Basic implementation for 1.16.5
     */
    public static boolean isCameraCenteredForFlying() {
        try {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player == null) return false;

            // Check if player is flying with elytra (1.16.5 API)
            return player.isFallFlying(); // 1.16.5 uses isFallFlying() instead of isElytraFlying()

        } catch (Exception e) {
            LOGGER.debug("Error checking flying state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Force refresh of active mod detection
     */
    public static void refreshActiveModDetection() {
        try {
            activeMod = detectActiveMod();
            LOGGER.debug("Refreshed active mod detection: {}", activeMod.name());
        } catch (Exception e) {
            LOGGER.debug("Error refreshing active mod detection: {}", e.getMessage());
            activeMod = ActiveThirdPersonMod.NONE;
        }
    }

    /**
     * Get specific mod initialization status
     */
    public static boolean isSpecificModInitialized(String modId) {
        try {
            switch (modId.toLowerCase()) {
                case SHOULDER_SURFING_MOD_ID:
                    return isShoulderSurfingInitialized;
                case BETTER_THIRD_PERSON_MOD_ID:
                    return isBetterThirdPersonInitialized;
                default:
                    return false;
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking specific mod initialization for {}: {}", modId, e.getMessage());
            return false;
        }
    }

    /**
     * Get specific mod loaded status
     */
    public static boolean isSpecificModLoaded(String modId) {
        try {
            switch (modId.toLowerCase()) {
                case SHOULDER_SURFING_MOD_ID:
                    return isShoulderSurfingLoaded;
                case BETTER_THIRD_PERSON_MOD_ID:
                    return isBetterThirdPersonLoaded;
                default:
                    return false;
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking specific mod load status for {}: {}", modId, e.getMessage());
            return false;
        }
    }

    /**
     * Emergency fallback method if reflection fails
     */
    public static void enableEmergencyMode() {
        try {
            LOGGER.warn("Enabling emergency compatibility mode - limited functionality");

            // Reset all reflection-based features
            isShoulderSurfingInitialized = false;
            isBetterThirdPersonInitialized = false;

            // Clear reflection objects
            shoulderSurfingConfigClass = null;
            betterThirdPersonConfigClass = null;

            // Clear methods and fields
            shoulderSurfingIsEnabledMethod = null;
            shoulderSurfingGetOffsetMethod = null;
            shoulderSurfingIsActiveMethod = null;
            betterThirdPersonGetConfigMethod = null;
            betterThirdPersonPlayerRotationField = null;

            LOGGER.info("Emergency mode enabled - basic third person detection only");
        } catch (Exception e) {
            LOGGER.error("Error enabling emergency mode: {}", e.getMessage());
        }
    }

    /**
     * Test method for verifying compatibility without side effects
     */
    public static String testCompatibility() {
        try {
            StringBuilder result = new StringBuilder();
            result.append("=== Third Person Compatibility Test (1.16.5) ===\n");

            // Test basic detection
            result.append("Mod Detection:\n");
            result.append("  Shoulder Surfing: ").append(isShoulderSurfingLoaded ? "✓" : "✗").append("\n");
            result.append("  Better Third Person: ").append(isBetterThirdPersonLoaded ? "✓" : "✗").append("\n");

            // Test initialization
            result.append("Initialization:\n");
            result.append("  Shoulder Surfing: ").append(isShoulderSurfingInitialized ? "✓" : "✗").append("\n");
            result.append("  Better Third Person: ").append(isBetterThirdPersonInitialized ? "✓" : "✗").append("\n");

            // Test current state
            result.append("Current State:\n");
            result.append("  Active Mod: ").append(detectActiveMod().name()).append("\n");
            result.append("  Third Person Active: ").append(isThirdPersonActive() ? "✓" : "✗").append("\n");

            // Test camera offset
            try {
                Vector3d offset = getThirdPersonCameraOffset();
                result.append("  Camera Offset: ").append(String.format("(%.2f, %.2f, %.2f)", offset.x, offset.y, offset.z)).append("\n");
            } catch (Exception e) {
                result.append("  Camera Offset: ERROR - ").append(e.getMessage()).append("\n");
            }

            // Test performance features
            result.append("Performance Features:\n");
            result.append("  Cached Offset: ");
            try {
                Vector3d cachedOffset = getCachedThirdPersonCameraOffset();
                result.append(String.format("(%.2f, %.2f, %.2f)", cachedOffset.x, cachedOffset.y, cachedOffset.z)).append("\n");
            } catch (Exception e) {
                result.append("ERROR - ").append(e.getMessage()).append("\n");
            }

            result.append("=== Test Complete ===");
            return result.toString();
        } catch (Exception e) {
            LOGGER.debug("Error running compatibility test: {}", e.getMessage());
            return "=== Compatibility Test Failed ===\nError: " + e.getMessage();
        }
    }

    /**
     * Get detailed debug information with comprehensive error handling
     */
    public static String getDetailedStatus() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Enhanced Third Person Compatibility Status (1.16.5):\n");
            sb.append("- Shoulder Surfing Reloaded: ").append(isShoulderSurfingLoaded ? "LOADED" : "Not Found");
            if (isShoulderSurfingLoaded) {
                sb.append(" (").append(isShoulderSurfingInitialized ? "Enhanced" : "Basic").append(")");
            }
            sb.append("\n");

            sb.append("- Better Third Person: ").append(isBetterThirdPersonLoaded ? "LOADED" : "Not Found");
            if (isBetterThirdPersonLoaded) {
                sb.append(" (").append(isBetterThirdPersonInitialized ? "Enhanced" : "Basic").append(")");
            }
            sb.append("\n");

            sb.append("- Currently Active Mod: ").append(detectActiveMod().name()).append("\n");

            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.options != null) {
                    sb.append("- Camera Type: ").append(mc.options.getCameraType()).append("\n");
                }
            } catch (Exception e) {
                sb.append("- Camera Type: Error getting camera type\n");
            }

            return sb.toString();
        } catch (Exception e) {
            LOGGER.debug("Error getting detailed status: {}", e.getMessage());
            return "Error generating detailed status";
        }
    }
}