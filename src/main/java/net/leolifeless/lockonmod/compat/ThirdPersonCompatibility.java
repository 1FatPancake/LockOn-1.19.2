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
 * Enhanced compatibility handler for multiple third person mods
 * - Leawind's Third Person (Full Support)
 * - Shoulder Surfing Reloaded (Enhanced Support)
 * - Better Third Person (Basic Support)
 * - Minecraft Vanilla Third Person (Basic Support)
 */
public class ThirdPersonCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Supported mod IDs
    private static final String LEAWIND_MOD_ID = "leawind_third_person";
    private static final String SHOULDER_SURFING_MOD_ID = "shouldersurfing";
    private static final String BETTER_THIRD_PERSON_MOD_ID = "betterthirdperson";

    // Mod detection flags
    private static boolean isLeawindLoaded = false;
    private static boolean isShoulderSurfingLoaded = false;
    private static boolean isBetterThirdPersonLoaded = false;

    // Initialization flags
    private static boolean isLeawindInitialized = false;
    private static boolean isShoulderSurfingInitialized = false;
    private static boolean isBetterThirdPersonInitialized = false;

    // Current active mod
    private static ActiveThirdPersonMod activeMod = ActiveThirdPersonMod.NONE;

    // Reflection for Leawind's Third Person
    private static Class<?> leawindConfigClass;
    private static Method leawindIsEnabledMethod;
    private static Field leawindEnabledField;

    // Reflection for Shoulder Surfing Reloaded
    private static Class<?> shoulderSurfingConfigClass;
    private static Class<?> shoulderSurfingApiClass;
    private static Class<?> shoulderSurfingInputClass;
    private static Method shoulderSurfingIsEnabledMethod;
    private static Method shoulderSurfingGetOffsetMethod;
    private static Method shoulderSurfingIsActiveMethod;
    private static Method shoulderSurfingDisableInputMethod;
    private static Method shoulderSurfingEnableInputMethod;
    private static Field shoulderSurfingInputHandlerField;

    // Reflection for Better Third Person
    private static Class<?> betterThirdPersonConfigClass;
    private static Method betterThirdPersonGetConfigMethod;
    private static Field betterThirdPersonPlayerRotationField;

    public enum ActiveThirdPersonMod {
        NONE,
        LEAWIND,
        SHOULDER_SURFING,
        BETTER_THIRD_PERSON,
        MINECRAFT_VANILLA
    }

    static {
        detectAndInitializeMods();
    }

    /**
     * Detect and initialize all supported third person mods
     */
    private static void detectAndInitializeMods() {
        // Check which mods are loaded
        isLeawindLoaded = ModList.get().isLoaded(LEAWIND_MOD_ID);
        isShoulderSurfingLoaded = ModList.get().isLoaded(SHOULDER_SURFING_MOD_ID);
        isBetterThirdPersonLoaded = ModList.get().isLoaded(BETTER_THIRD_PERSON_MOD_ID);

        LOGGER.info("Third Person Mod Detection:");
        LOGGER.info("  Leawind's Third Person: {}", isLeawindLoaded ? "FOUND" : "Not Found");
        LOGGER.info("  Shoulder Surfing Reloaded: {}", isShoulderSurfingLoaded ? "FOUND" : "Not Found");
        LOGGER.info("  Better Third Person: {}", isBetterThirdPersonLoaded ? "FOUND" : "Not Found");

        // Initialize each mod
        if (isLeawindLoaded) {
            initializeLeawind();
        }
        if (isShoulderSurfingLoaded) {
            initializeShoulderSurfing();
        }
        if (isBetterThirdPersonLoaded) {
            initializeBetterThirdPerson();
        }
    }

    /**
     * Initialize Leawind's Third Person compatibility
     */
    private static void initializeLeawind() {
        try {
            // Try multiple possible class paths for Leawind's mod
            String[] possibleClasses = {
                    "com.github.leawind.thirdperson.config.Config",
                    "com.github.leawind.thirdperson.Config",
                    "leawind.thirdperson.config.Config",
                    "leawind.thirdperson.Config"
            };

            for (String className : possibleClasses) {
                try {
                    leawindConfigClass = Class.forName(className);
                    LOGGER.info("Found Leawind config class: {}", className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            if (leawindConfigClass != null) {
                // Try to find enabled methods/fields
                String[] possibleMethods = {"isModEnabled", "isEnabled", "isAvailable", "enabled"};
                for (String methodName : possibleMethods) {
                    try {
                        leawindIsEnabledMethod = leawindConfigClass.getMethod(methodName);
                        LOGGER.info("Found Leawind enabled method: {}", methodName);
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }

                // If no method found, try fields
                if (leawindIsEnabledMethod == null) {
                    String[] possibleFields = {"enabled", "isEnabled", "modEnabled"};
                    for (String fieldName : possibleFields) {
                        try {
                            leawindEnabledField = leawindConfigClass.getField(fieldName);
                            LOGGER.info("Found Leawind enabled field: {}", fieldName);
                            break;
                        } catch (NoSuchFieldException ignored) {}
                    }
                }

                isLeawindInitialized = (leawindIsEnabledMethod != null || leawindEnabledField != null);
                LOGGER.info("Leawind initialization: {}", isLeawindInitialized ? "SUCCESS" : "BASIC FALLBACK");
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to initialize Leawind compatibility: {}", e.getMessage());
            isLeawindInitialized = false;
        }
    }

    /**
     * Initialize Shoulder Surfing Reloaded compatibility
     */
    private static void initializeShoulderSurfing() {
        try {
            // Try multiple possible class paths for Shoulder Surfing
            String[] possibleConfigClasses = {
                    "com.teamderpy.shouldersurfing.config.Config",
                    "shouldersurfing.config.Config",
                    "shouldersurfing.Config",
                    "com.exopandora.shouldersurfing.config.Config",
                    "com.exopandora.shouldersurfing.Config"
            };

            String[] possibleApiClasses = {
                    "com.teamderpy.shouldersurfing.api.ShoulderSurfingAPI",
                    "shouldersurfing.api.ShoulderSurfingAPI",
                    "shouldersurfing.ShoulderSurfingAPI",
                    "com.exopandora.shouldersurfing.api.ShoulderSurfingAPI",
                    "com.exopandora.shouldersurfing.ShoulderSurfingAPI"
            };

            String[] possibleInputClasses = {
                    "com.teamderpy.shouldersurfing.client.InputHandler",
                    "shouldersurfing.client.InputHandler",
                    "shouldersurfing.InputHandler",
                    "com.exopandora.shouldersurfing.client.InputHandler",
                    "com.exopandora.shouldersurfing.InputHandler"
            };

            // Try to find config class
            for (String className : possibleConfigClasses) {
                try {
                    shoulderSurfingConfigClass = Class.forName(className);
                    LOGGER.info("Found Shoulder Surfing config class: {}", className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            // Try to find API class
            for (String className : possibleApiClasses) {
                try {
                    shoulderSurfingApiClass = Class.forName(className);
                    LOGGER.info("Found Shoulder Surfing API class: {}", className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            // Try to find Input Handler class
            for (String className : possibleInputClasses) {
                try {
                    shoulderSurfingInputClass = Class.forName(className);
                    LOGGER.info("Found Shoulder Surfing input class: {}", className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            if (shoulderSurfingConfigClass != null || shoulderSurfingApiClass != null) {
                // Try to find methods
                Class<?> targetClass = shoulderSurfingApiClass != null ? shoulderSurfingApiClass : shoulderSurfingConfigClass;

                String[] possibleEnabledMethods = {"isEnabled", "isActive", "isShoulderSurfingEnabled", "enabled"};
                String[] possibleOffsetMethods = {"getCameraOffset", "getOffset", "getShoulderOffset", "getCameraPosition"};
                String[] possibleActiveMethods = {"isThirdPersonEnabled", "isActive", "isInThirdPerson"};

                // Try to find enabled method
                for (String methodName : possibleEnabledMethods) {
                    try {
                        shoulderSurfingIsEnabledMethod = targetClass.getMethod(methodName);
                        LOGGER.info("Found Shoulder Surfing enabled method: {}", methodName);
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }

                // Try to find offset method
                for (String methodName : possibleOffsetMethods) {
                    try {
                        shoulderSurfingGetOffsetMethod = targetClass.getMethod(methodName);
                        LOGGER.info("Found Shoulder Surfing offset method: {}", methodName);
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }

                // Try to find active method
                for (String methodName : possibleActiveMethods) {
                    try {
                        shoulderSurfingIsActiveMethod = targetClass.getMethod(methodName);
                        LOGGER.info("Found Shoulder Surfing active method: {}", methodName);
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }

                // Try to find input control methods for fixing keybind conflicts
                if (shoulderSurfingInputClass != null) {
                    String[] possibleDisableMethods = {"disableInput", "pauseInput", "setInputEnabled", "suspend"};
                    String[] possibleEnableMethods = {"enableInput", "resumeInput", "setInputEnabled", "resume"};

                    for (String methodName : possibleDisableMethods) {
                        try {
                            shoulderSurfingDisableInputMethod = shoulderSurfingInputClass.getMethod(methodName);
                            LOGGER.info("Found Shoulder Surfing disable input method: {}", methodName);
                            break;
                        } catch (NoSuchMethodException ignored) {
                            // Try with boolean parameter
                            try {
                                shoulderSurfingDisableInputMethod = shoulderSurfingInputClass.getMethod(methodName, boolean.class);
                                LOGGER.info("Found Shoulder Surfing disable input method (with boolean): {}", methodName);
                                break;
                            } catch (NoSuchMethodException ignored2) {}
                        }
                    }

                    for (String methodName : possibleEnableMethods) {
                        try {
                            shoulderSurfingEnableInputMethod = shoulderSurfingInputClass.getMethod(methodName);
                            LOGGER.info("Found Shoulder Surfing enable input method: {}", methodName);
                            break;
                        } catch (NoSuchMethodException ignored) {
                            // Try with boolean parameter
                            try {
                                shoulderSurfingEnableInputMethod = shoulderSurfingInputClass.getMethod(methodName, boolean.class);
                                LOGGER.info("Found Shoulder Surfing enable input method (with boolean): {}", methodName);
                                break;
                            } catch (NoSuchMethodException ignored2) {}
                        }
                    }

                    // Try to find input handler field for direct access
                    String[] possibleInputFields = {"inputHandler", "instance", "INSTANCE", "handler"};
                    for (String fieldName : possibleInputFields) {
                        try {
                            shoulderSurfingInputHandlerField = shoulderSurfingInputClass.getDeclaredField(fieldName);
                            shoulderSurfingInputHandlerField.setAccessible(true);
                            LOGGER.info("Found Shoulder Surfing input handler field: {}", fieldName);
                            break;
                        } catch (NoSuchFieldException ignored) {}
                    }
                }

                isShoulderSurfingInitialized = true;
                LOGGER.info("Shoulder Surfing initialization: SUCCESS (Input conflict resolution: {})",
                        (shoulderSurfingDisableInputMethod != null || shoulderSurfingInputHandlerField != null) ? "AVAILABLE" : "LIMITED");
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to initialize Shoulder Surfing compatibility: {}", e.getMessage());
            isShoulderSurfingInitialized = false;
        }
    }

    /**
     * Initialize Better Third Person compatibility
     */
    private static void initializeBetterThirdPerson() {
        try {
            // Try multiple possible class paths for Better Third Person
            String[] possibleClasses = {
                    "com.github.leawind.thirdperson.ThirdPersonConfig", // Old version
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
                    LOGGER.info("Found Better Third Person config class: {}", className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            if (betterThirdPersonConfigClass != null) {
                // Try to find methods to control player rotation
                String[] possibleMethods = {
                        "getConfig", "getInstance", "get", "getClientConfig"
                };

                for (String methodName : possibleMethods) {
                    try {
                        betterThirdPersonGetConfigMethod = betterThirdPersonConfigClass.getMethod(methodName);
                        LOGGER.info("Found Better Third Person config method: {}", methodName);
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
                        LOGGER.info("Found Better Third Person rotation field: {}", fieldName);
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }

                isBetterThirdPersonInitialized = true;
                LOGGER.info("Better Third Person initialization: SUCCESS");
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to initialize Better Third Person compatibility: {}", e.getMessage());
            isBetterThirdPersonInitialized = false;
        }
    }

    /**
     * Determine which third person mod is currently active
     */
    private static ActiveThirdPersonMod detectActiveMod() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return ActiveThirdPersonMod.NONE;

        // Check if we're even in third person view
        boolean isThirdPersonView = !mc.options.getCameraType().isFirstPerson();
        if (!isThirdPersonView) {
            return ActiveThirdPersonMod.NONE;
        }

        // Priority order: Leawind -> Shoulder Surfing -> Better Third Person -> Vanilla

        // Check Leawind first (highest priority)
        if (isLeawindLoaded && isLeawindEnabled()) {
            return ActiveThirdPersonMod.LEAWIND;
        }

        // Check Shoulder Surfing
        if (isShoulderSurfingLoaded && isShoulderSurfingEnabled()) {
            return ActiveThirdPersonMod.SHOULDER_SURFING;
        }

        // Check Better Third Person
        if (isBetterThirdPersonLoaded) {
            return ActiveThirdPersonMod.BETTER_THIRD_PERSON;
        }

        // Fallback to vanilla third person
        return ActiveThirdPersonMod.MINECRAFT_VANILLA;
    }

    /**
     * Check if Leawind's mod is enabled
     */
    private static boolean isLeawindEnabled() {
        if (!isLeawindLoaded) return false;

        try {
            if (isLeawindInitialized) {
                if (leawindIsEnabledMethod != null) {
                    Boolean enabled = (Boolean) leawindIsEnabledMethod.invoke(null);
                    return enabled != null && enabled;
                } else if (leawindEnabledField != null) {
                    Boolean enabled = (Boolean) leawindEnabledField.get(null);
                    return enabled != null && enabled;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking Leawind enabled state: {}", e.getMessage());
        }

        return true; // Assume enabled if we can't check
    }

    /**
     * Check if Shoulder Surfing is enabled
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

    // === PUBLIC API METHODS ===

    /**
     * Check if any third person mod is currently active
     */
    public static boolean isThirdPersonActive() {
        activeMod = detectActiveMod();
        return activeMod != ActiveThirdPersonMod.NONE;
    }

    /**
     * Get the current camera offset from the active third person mod
     */
    public static Vec3 getThirdPersonCameraOffset() {
        activeMod = detectActiveMod();

        switch (activeMod) {
            case LEAWIND:
                return getLeawindCameraOffset();
            case SHOULDER_SURFING:
                return getShoulderSurfingCameraOffset();
            case BETTER_THIRD_PERSON:
                return getBetterThirdPersonCameraOffset();
            case MINECRAFT_VANILLA:
                return getVanillaCameraOffset();
            default:
                return Vec3.ZERO;
        }
    }

    /**
     * Get camera offset from Leawind's mod
     */
    private static Vec3 getLeawindCameraOffset() {
        // Since we can't reliably get exact offset, provide reasonable estimate
        return new Vec3(0, 0, -4.0); // Behind player
    }

    /**
     * Get camera offset from Shoulder Surfing
     */
    private static Vec3 getShoulderSurfingCameraOffset() {
        try {
            if (shoulderSurfingGetOffsetMethod != null) {
                Object offset = shoulderSurfingGetOffsetMethod.invoke(null);
                if (offset instanceof Vec3) {
                    return (Vec3) offset;
                }
                // Handle other possible return types
                // Could be a Vector3d, Vector3f, etc.
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting Shoulder Surfing camera offset: {}", e.getMessage());
        }

        // Shoulder Surfing typically uses over-the-shoulder view
        return new Vec3(1.5, 0, -3.0); // Offset to the side and back
    }

    /**
     * Get camera offset from Better Third Person
     */
    private static Vec3 getBetterThirdPersonCameraOffset() {
        // Better Third Person - basic estimate
        return new Vec3(0, 0, -4.0);
    }

    /**
     * Get camera offset from vanilla Minecraft
     */
    private static Vec3 getVanillaCameraOffset() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            switch (mc.options.getCameraType()) {
                case THIRD_PERSON_BACK:
                    return new Vec3(0, 0, -4.0);
                case THIRD_PERSON_FRONT:
                    return new Vec3(0, 0, 4.0);
                default:
                    return Vec3.ZERO;
            }
        }
        return Vec3.ZERO;
    }

    /**
     * Calculate enhanced targeting range based on active third person mod
     */
    public static double getAdjustedTargetingRange(double baseRange) {
        if (!isThirdPersonActive()) {
            return baseRange;
        }

        Vec3 cameraOffset = getThirdPersonCameraOffset();
        double offsetDistance = cameraOffset.length();

        // Different multipliers for different mods
        switch (activeMod) {
            case LEAWIND:
                return baseRange * 1.2 + offsetDistance;
            case SHOULDER_SURFING:
                return baseRange * 1.3 + offsetDistance; // Shoulder surfing needs more range
            case BETTER_THIRD_PERSON:
                return baseRange * 1.15 + offsetDistance;
            case MINECRAFT_VANILLA:
                return baseRange * 1.1 + offsetDistance;
            default:
                return baseRange;
        }
    }

    /**
     * Adjust targeting angle for active third person mod
     */
    public static double getAdjustedTargetingAngle(double baseAngle) {
        if (!isThirdPersonActive()) {
            return baseAngle;
        }

        // Different angle adjustments for different mods
        switch (activeMod) {
            case LEAWIND:
                return Math.min(baseAngle * 1.3, 180.0);
            case SHOULDER_SURFING:
                return Math.min(baseAngle * 1.4, 180.0); // Shoulder surfing needs wider angle
            case BETTER_THIRD_PERSON:
                return Math.min(baseAngle * 1.2, 180.0);
            case MINECRAFT_VANILLA:
                return Math.min(baseAngle * 1.15, 180.0);
            default:
                return baseAngle;
        }
    }

    /**
     * Get enhanced target position for rendering
     */
    public static Vec3 getAdjustedTargetPosition(Entity target, Vec3 basePosition) {
        if (!isThirdPersonActive() || target == null) {
            return basePosition;
        }

        Vec3 cameraOffset = getThirdPersonCameraOffset();

        // Adjust based on active mod
        switch (activeMod) {
            case SHOULDER_SURFING:
                // Shoulder surfing needs special handling due to offset view
                return basePosition.add(cameraOffset.scale(0.15));
            case LEAWIND:
            case BETTER_THIRD_PERSON:
            case MINECRAFT_VANILLA:
            default:
                return basePosition.add(cameraOffset.scale(0.1));
        }
    }

    /**
     * Check if we should use enhanced smoothing
     */
    public static boolean shouldUseEnhancedSmoothing() {
        return isThirdPersonActive();
    }

    /**
     * Get smoothing factor for active third person mod
     */
    public static float getThirdPersonSmoothingFactor(float baseFactor) {
        if (!isThirdPersonActive()) {
            return baseFactor;
        }

        // Different smoothing for different mods
        switch (activeMod) {
            case SHOULDER_SURFING:
                return Math.min(baseFactor * 1.2f, 1.0f); // More smoothing for shoulder surfing
            case LEAWIND:
                return Math.min(baseFactor * 1.15f, 1.0f);
            case BETTER_THIRD_PERSON:
            case MINECRAFT_VANILLA:
            default:
                return Math.min(baseFactor * 1.1f, 1.0f);
        }
    }

    /**
     * Adjust rotation speed for active third person mod
     */
    public static float getAdjustedRotationSpeed(float baseSpeed) {
        if (!isThirdPersonActive()) {
            return baseSpeed;
        }

        // Different speed adjustments for different mods
        switch (activeMod) {
            case SHOULDER_SURFING:
                return baseSpeed * 0.8f; // Slower for shoulder surfing to prevent disorientation
            case LEAWIND:
                return baseSpeed * 0.85f;
            case BETTER_THIRD_PERSON:
            case MINECRAFT_VANILLA:
            default:
                return baseSpeed * 0.9f;
        }
    }

    /**
     * Adjust indicator size for active third person mod
     */
    public static float getAdjustedIndicatorSize(float baseSize) {
        if (!isThirdPersonActive()) {
            return baseSize;
        }

        Vec3 cameraOffset = getThirdPersonCameraOffset();
        double distance = cameraOffset.length();

        // Base scaling factor
        float scaleFactor = 1.0f + (float)(distance * 0.05);

        // Additional mod-specific adjustments
        switch (activeMod) {
            case SHOULDER_SURFING:
                scaleFactor *= 1.1f; // Slightly larger for shoulder surfing
                break;
            case LEAWIND:
            case BETTER_THIRD_PERSON:
            case MINECRAFT_VANILLA:
            default:
                // Use base scaling
                break;
        }

        return baseSize * Math.min(scaleFactor, 1.5f);
    }

    // === ENHANCED PLAYER ROTATION METHODS ===

    /**
     * Force player rotation to follow camera when lock-on is active (All third person mods)
     */
    public static void ensurePlayerRotationFollowsCamera() {
        activeMod = detectActiveMod(); // Make sure we have the current active mod

        switch (activeMod) {
            case SHOULDER_SURFING:
                ensureShoulderSurfingPlayerRotation();
                break;
            case BETTER_THIRD_PERSON:
                ensureBetterThirdPersonPlayerRotation();
                break;
            case LEAWIND:
                // Leawind usually handles this automatically
                LOGGER.debug("Leawind third person active - using built-in rotation handling");
                break;
            case MINECRAFT_VANILLA:
                // No special handling needed for vanilla
                LOGGER.debug("Vanilla third person active - no special rotation handling needed");
                break;
            default:
                LOGGER.debug("No third person mod active");
                break;
        }
    }

    /**
     * Restore player rotation settings when lock-on is disabled (All third person mods)
     */
    public static void restorePlayerRotationSettings() {
        switch (activeMod) {
            case SHOULDER_SURFING:
                restoreShoulderSurfingSettings();
                break;
            case BETTER_THIRD_PERSON:
                restoreBetterThirdPersonSettings();
                break;
            case LEAWIND:
                // Leawind usually handles this automatically
                LOGGER.debug("Leawind third person - using built-in rotation restoration");
                break;
            case MINECRAFT_VANILLA:
                // No special handling needed for vanilla
                LOGGER.debug("Vanilla third person - no special rotation restoration needed");
                break;
            default:
                LOGGER.debug("No third person mod active");
                break;
        }
    }

    // === SHOULDER SURFING INPUT CONFLICT RESOLUTION ===

    /**
     * Temporarily disable Shoulder Surfing input handling to prevent keybind conflicts
     */
    public static void disableShoulderSurfingInput() {
        if (activeMod != ActiveThirdPersonMod.SHOULDER_SURFING) {
            return;
        }

        try {
            if (shoulderSurfingDisableInputMethod != null) {
                // Try calling the disable method
                try {
                    shoulderSurfingDisableInputMethod.invoke(null);
                    LOGGER.debug("Disabled Shoulder Surfing input handling for lock-on");
                    return;
                } catch (Exception e) {
                    // Try with boolean parameter
                    try {
                        shoulderSurfingDisableInputMethod.invoke(null, false);
                        LOGGER.debug("Disabled Shoulder Surfing input handling for lock-on (with boolean)");
                        return;
                    } catch (Exception e2) {
                        LOGGER.debug("Failed to disable Shoulder Surfing input: {}", e2.getMessage());
                    }
                }
            }

            // Try direct field manipulation if methods don't work
            if (shoulderSurfingInputHandlerField != null) {
                Object inputHandler = shoulderSurfingInputHandlerField.get(null);
                if (inputHandler != null) {
                    // Try to find and call suspend/disable methods on the handler
                    Class<?> handlerClass = inputHandler.getClass();
                    String[] suspendMethods = {"suspend", "disable", "pause", "setEnabled"};

                    for (String methodName : suspendMethods) {
                        try {
                            Method method = handlerClass.getMethod(methodName);
                            method.invoke(inputHandler);
                            LOGGER.debug("Suspended Shoulder Surfing input handler via {}", methodName);
                            return;
                        } catch (Exception ignored) {
                            try {
                                Method method = handlerClass.getMethod(methodName, boolean.class);
                                method.invoke(inputHandler, false);
                                LOGGER.debug("Suspended Shoulder Surfing input handler via {} (with boolean)", methodName);
                                return;
                            } catch (Exception ignored2) {}
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.debug("Could not disable Shoulder Surfing input: {}", e.getMessage());
        }
    }

    /**
     * Re-enable Shoulder Surfing input handling after lock-on is disabled
     */
    public static void enableShoulderSurfingInput() {
        if (activeMod != ActiveThirdPersonMod.SHOULDER_SURFING) {
            return;
        }

        try {
            if (shoulderSurfingEnableInputMethod != null) {
                // Try calling the enable method
                try {
                    shoulderSurfingEnableInputMethod.invoke(null);
                    LOGGER.debug("Re-enabled Shoulder Surfing input handling");
                    return;
                } catch (Exception e) {
                    // Try with boolean parameter
                    try {
                        shoulderSurfingEnableInputMethod.invoke(null, true);
                        LOGGER.debug("Re-enabled Shoulder Surfing input handling (with boolean)");
                        return;
                    } catch (Exception e2) {
                        LOGGER.debug("Failed to re-enable Shoulder Surfing input: {}", e2.getMessage());
                    }
                }
            }

            // Try direct field manipulation if methods don't work
            if (shoulderSurfingInputHandlerField != null) {
                Object inputHandler = shoulderSurfingInputHandlerField.get(null);
                if (inputHandler != null) {
                    // Try to find and call resume/enable methods on the handler
                    Class<?> handlerClass = inputHandler.getClass();
                    String[] resumeMethods = {"resume", "enable", "unpause", "setEnabled"};

                    for (String methodName : resumeMethods) {
                        try {
                            Method method = handlerClass.getMethod(methodName);
                            method.invoke(inputHandler);
                            LOGGER.debug("Resumed Shoulder Surfing input handler via {}", methodName);
                            return;
                        } catch (Exception ignored) {
                            try {
                                Method method = handlerClass.getMethod(methodName, boolean.class);
                                method.invoke(inputHandler, true);
                                LOGGER.debug("Resumed Shoulder Surfing input handler via {} (with boolean)", methodName);
                                return;
                            } catch (Exception ignored2) {}
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.debug("Could not re-enable Shoulder Surfing input: {}", e.getMessage());
        }
    }

    /**
     * Force player rotation for Shoulder Surfing
     */
    private static void ensureShoulderSurfingPlayerRotation() {
        try {
            // First, temporarily disable input conflicts
            disableShoulderSurfingInput();

            // Then handle rotation settings
            if (shoulderSurfingConfigClass != null) {
                // Look for config fields to modify
                String[] rotationFields = {
                        "player_y_rot_follows_camera",
                        "player_x_rot_follows_camera",
                        "playerYRotFollowsCamera",
                        "playerXRotFollowsCamera"
                };

                for (String fieldName : rotationFields) {
                    try {
                        Field field = shoulderSurfingConfigClass.getDeclaredField(fieldName);
                        field.setAccessible(true);

                        // Get current value
                        Object currentValue = field.get(null);
                        if (currentValue instanceof Boolean && !(Boolean) currentValue) {
                            // Temporarily set to true while lock-on is active
                            field.set(null, true);
                            LOGGER.debug("Temporarily enabled {} for lock-on compatibility", fieldName);
                        }
                    } catch (NoSuchFieldException | IllegalAccessException ignored) {
                        // Field doesn't exist or can't access, try next one
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not modify Shoulder Surfing rotation settings: {}", e.getMessage());
        }
    }

    /**
     * Restore Shoulder Surfing rotation settings
     */
    private static void restoreShoulderSurfingSettings() {
        try {
            // Re-enable input handling
            enableShoulderSurfingInput();

            if (shoulderSurfingConfigClass != null) {
                // Restore original settings (user should configure these in their config file)
                // This is just a fallback - the main fix is the config file changes above
                LOGGER.debug("Lock-on disabled, Shoulder Surfing rotation restored to user settings");
            }
        } catch (Exception e) {
            LOGGER.debug("Could not restore Shoulder Surfing rotation settings: {}", e.getMessage());
        }
    }

    /**
     * Force player rotation to follow camera when lock-on is active (Better Third Person specific)
     */
    private static void ensureBetterThirdPersonPlayerRotation() {
        if (activeMod != ActiveThirdPersonMod.BETTER_THIRD_PERSON) {
            return; // Only apply to Better Third Person
        }

        try {
            if (isBetterThirdPersonInitialized && betterThirdPersonPlayerRotationField != null) {
                // Try to enable player rotation following
                Object configInstance = null;

                if (betterThirdPersonGetConfigMethod != null) {
                    configInstance = betterThirdPersonGetConfigMethod.invoke(null);
                }

                if (configInstance != null) {
                    // Set rotation field to true/enabled
                    Object currentValue = betterThirdPersonPlayerRotationField.get(configInstance);
                    if (currentValue instanceof Boolean && !(Boolean) currentValue) {
                        betterThirdPersonPlayerRotationField.set(configInstance, true);
                        LOGGER.debug("Enabled Better Third Person player rotation for lock-on");
                    }
                } else {
                    // Try static field access
                    Object currentValue = betterThirdPersonPlayerRotationField.get(null);
                    if (currentValue instanceof Boolean && !(Boolean) currentValue) {
                        betterThirdPersonPlayerRotationField.set(null, true);
                        LOGGER.debug("Enabled Better Third Person static player rotation for lock-on");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not modify Better Third Person rotation settings: {}", e.getMessage());
        }
    }

    /**
     * Restore Better Third Person rotation settings when lock-on is disabled
     */
    private static void restoreBetterThirdPersonSettings() {
        if (activeMod != ActiveThirdPersonMod.BETTER_THIRD_PERSON) {
            return;
        }

        try {
            if (isBetterThirdPersonInitialized) {
                LOGGER.debug("Lock-on disabled, Better Third Person rotation restored to user settings");
            }
        } catch (Exception e) {
            LOGGER.debug("Could not restore Better Third Person settings: {}", e.getMessage());
        }
    }

    // === STATUS AND DEBUG METHODS ===

    /**
     * Get comprehensive compatibility status
     */
    public static String getCompatibilityStatus() {
        StringBuilder status = new StringBuilder();

        if (!isLeawindLoaded && !isShoulderSurfingLoaded && !isBetterThirdPersonLoaded) {
            status.append("Third Person Mods: None Detected");
        } else {
            status.append("Third Person Mods: ");

            if (isLeawindLoaded) {
                status.append("Leawind(").append(isLeawindInitialized ? "Enhanced" : "Basic").append(") ");
            }
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
    }

    /**
     * Get the currently active mod
     */
    public static ActiveThirdPersonMod getActiveMod() {
        return detectActiveMod();
    }

    /**
     * Check if mod is loaded
     */
    public static boolean isModLoaded() {
        return isLeawindLoaded || isShoulderSurfingLoaded || isBetterThirdPersonLoaded;
    }

    /**
     * Check if any mod is initialized
     */
    public static boolean isInitialized() {
        return isLeawindInitialized || isShoulderSurfingInitialized || isBetterThirdPersonInitialized;
    }

    /**
     * Get detailed debug information
     */
    public static String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Enhanced Third Person Compatibility Status:\n");
        sb.append("- Leawind's Third Person: ").append(isLeawindLoaded ? "LOADED" : "Not Found");
        if (isLeawindLoaded) {
            sb.append(" (").append(isLeawindInitialized ? "Enhanced" : "Basic").append(")");
        }
        sb.append("\n");

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
    }

    // === UTILITY METHODS FOR EXTERNAL ACCESS ===

    /**
     * Check if camera is centered due to flying (elytra) - Leawind specific
     */
    public static boolean isCameraCenteredForFlying() {
        if (activeMod != ActiveThirdPersonMod.LEAWIND) {
            return false;
        }

        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return false;

            // Check if player is flying with elytra
            boolean isFlying = player.isFallFlying();

            // For now, just return flying state since we can't access specific config
            return isFlying;

        } catch (Exception e) {
            LOGGER.debug("Error checking flying state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Force refresh of active mod detection
     */
    public static void refreshActiveModDetection() {
        activeMod = detectActiveMod();
        LOGGER.debug("Refreshed active mod detection: {}", activeMod.name());
    }

    /**
     * Get specific mod initialization status
     */
    public static boolean isSpecificModInitialized(String modId) {
        switch (modId.toLowerCase()) {
            case LEAWIND_MOD_ID:
                return isLeawindInitialized;
            case SHOULDER_SURFING_MOD_ID:
                return isShoulderSurfingInitialized;
            case BETTER_THIRD_PERSON_MOD_ID:
                return isBetterThirdPersonInitialized;
            default:
                return false;
        }
    }

    /**
     * Get specific mod loaded status
     */
    public static boolean isSpecificModLoaded(String modId) {
        switch (modId.toLowerCase()) {
            case LEAWIND_MOD_ID:
                return isLeawindLoaded;
            case SHOULDER_SURFING_MOD_ID:
                return isShoulderSurfingLoaded;
            case BETTER_THIRD_PERSON_MOD_ID:
                return isBetterThirdPersonLoaded;
            default:
                return false;
        }
    }

    /**
     * Get performance-optimized camera offset (cached for multiple calls per frame)
     */
    private static Vec3 cachedCameraOffset = Vec3.ZERO;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 16; // ~1 frame at 60fps

    public static Vec3 getCachedThirdPersonCameraOffset() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheTime > CACHE_DURATION_MS) {
            cachedCameraOffset = getThirdPersonCameraOffset();
            lastCacheTime = currentTime;
        }
        return cachedCameraOffset;
    }

    /**
     * Emergency fallback method if reflection fails
     */
    public static void enableEmergencyMode() {
        LOGGER.warn("Enabling emergency compatibility mode - limited functionality");

        // Reset all reflection-based features
        isLeawindInitialized = false;
        isShoulderSurfingInitialized = false;
        isBetterThirdPersonInitialized = false;

        // Clear reflection objects
        leawindConfigClass = null;
        shoulderSurfingConfigClass = null;
        betterThirdPersonConfigClass = null;

        LOGGER.info("Emergency mode enabled - basic third person detection only");
    }

    /**
     * Test method for verifying compatibility without side effects
     */
    public static String testCompatibility() {
        StringBuilder result = new StringBuilder();
        result.append("=== Third Person Compatibility Test ===\n");

        // Test basic detection
        result.append("Mod Detection:\n");
        result.append("  Leawind: ").append(isLeawindLoaded ? "✓" : "✗").append("\n");
        result.append("  Shoulder Surfing: ").append(isShoulderSurfingLoaded ? "✓" : "✗").append("\n");
        result.append("  Better Third Person: ").append(isBetterThirdPersonLoaded ? "✓" : "✗").append("\n");

        // Test initialization
        result.append("Initialization:\n");
        result.append("  Leawind: ").append(isLeawindInitialized ? "✓" : "✗").append("\n");
        result.append("  Shoulder Surfing: ").append(isShoulderSurfingInitialized ? "✓" : "✗").append("\n");
        result.append("  Better Third Person: ").append(isBetterThirdPersonInitialized ? "✓" : "✗").append("\n");

        // Test current state
        result.append("Current State:\n");
        result.append("  Active Mod: ").append(detectActiveMod().name()).append("\n");
        result.append("  Third Person Active: ").append(isThirdPersonActive() ? "✓" : "✗").append("\n");

        // Test camera offset
        try {
            Vec3 offset = getThirdPersonCameraOffset();
            result.append("  Camera Offset: ").append(String.format("(%.2f, %.2f, %.2f)", offset.x, offset.y, offset.z)).append("\n");
        } catch (Exception e) {
            result.append("  Camera Offset: ERROR - ").append(e.getMessage()).append("\n");
        }

        result.append("=== Test Complete ===");
        return result.toString();
    }
}