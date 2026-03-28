package net.leolifeless.lockonmod.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * Third Person Compatibility Manager
 *
 * Lightweight coordinator that delegates to specific compatibility classes.
 * Much cleaner and easier to debug than one giant file!
 */
public class ThirdPersonCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean isInitialized = false;

    public enum ActiveThirdPersonMod {
        NONE,
        LEAWIND,
        SHOULDER_SURFING,
        MINECRAFT_VANILLA
    }

    /**
     * Initialize all third person mod compatibility
     */
    public static void initialize() {
        if (isInitialized) {
            LOGGER.warn("Compatibility system already initialized!");
            return;
        }

        LOGGER.info("═════════════════════════════════════════");
        LOGGER.info("  Third Person Compatibility Manager");
        LOGGER.info("═════════════════════════════════════════");

        // Initialize each compatibility layer
        ShoulderSurfingCompat.initialize();
        LeawindCompat.initialize();

        isInitialized = true;

        LOGGER.info("═════════════════════════════════════════");
        LOGGER.info("  Initialization Complete");
        LOGGER.info("═════════════════════════════════════════");
        logStatus();
    }

    /**
     * Log current status of all compatibility layers
     */
    private static void logStatus() {
        LOGGER.info("Compatibility Status:");
        LOGGER.info("  Shoulder Surfing: {}", ShoulderSurfingCompat.getStatusInfo());
        LOGGER.info("  Leawind: {}", LeawindCompat.getStatusInfo());
        LOGGER.info("  Active Mod: {}", getActiveMod().name());
    }

    /**
     * Determine which third person mod is currently active
     */
    public static ActiveThirdPersonMod getActiveMod() {
        if (!isThirdPersonActive()) {
            return ActiveThirdPersonMod.NONE;
        }

        // Priority order: Leawind > Shoulder Surfing > Better Third Person > Vanilla
        if (LeawindCompat.isLoaded() && LeawindCompat.isEnabled()) {
            return ActiveThirdPersonMod.LEAWIND;
        }

        if (ShoulderSurfingCompat.isLoaded() && ShoulderSurfingCompat.isInitialized()) {
            return ActiveThirdPersonMod.SHOULDER_SURFING;
        }

        return ActiveThirdPersonMod.MINECRAFT_VANILLA;
    }

    /**
     * Check if any third person view is active
     */
    public static boolean isThirdPersonActive() {
        Minecraft mc = Minecraft.getInstance();
        return mc.options != null && !mc.options.getCameraType().isFirstPerson();
    }

    /**
     * Check if any third person mod is loaded
     */
    public static boolean isModLoaded() {
        return ShoulderSurfingCompat.isLoaded() ||
                LeawindCompat.isLoaded();
    }

    // === LOCK-ON MODE CONTROL ===

    /**
     * Enable lock-on mode (primarily for Shoulder Surfing)
     */
    public static void ensurePlayerRotationSettings() {
        ActiveThirdPersonMod activeMod = getActiveMod();

        LOGGER.info("Lock-On Starting | Active Mod: {}", activeMod.name());

        if (activeMod == ActiveThirdPersonMod.SHOULDER_SURFING) {
            ShoulderSurfingCompat.enableLockOnMode();
        }

        // Other mods don't need special handling (yet)
    }

    /**
     * Disable lock-on mode and restore settings
     */
    public static void restorePlayerRotationSettings() {
        LOGGER.info("Lock-On Ending");

        if (ShoulderSurfingCompat.isLockOnModeActive()) {
            ShoulderSurfingCompat.disableLockOnMode();
        }
    }

    // === TARGETING ADJUSTMENTS ===

    /**
     * Get adjusted targeting range for third person camera
     */
    public static double getAdjustedTargetingRange(double baseRange) {
        if (!isThirdPersonActive()) {
            return baseRange;
        }

        ActiveThirdPersonMod activeMod = getActiveMod();
        double multiplier = 1.0;

        switch (activeMod) {
            case SHOULDER_SURFING:
                multiplier = ShoulderSurfingCompat.getTargetingRangeMultiplier();
                break;
            case LEAWIND:
                multiplier = LeawindCompat.getTargetingRangeMultiplier();
                break;
            case MINECRAFT_VANILLA:
                multiplier = 1.15; // 15% increase for vanilla
                break;
        }

        return baseRange * multiplier;
    }

    /**
     * Get adjusted targeting angle for third person camera
     */
    public static double getAdjustedTargetingAngle(double baseAngle) {
        if (!isThirdPersonActive()) {
            return baseAngle;
        }

        ActiveThirdPersonMod activeMod = getActiveMod();
        double multiplier = 1.0;

        switch (activeMod) {
            case SHOULDER_SURFING:
                multiplier = ShoulderSurfingCompat.getTargetingAngleMultiplier();
                break;
            case LEAWIND:
                multiplier = LeawindCompat.getTargetingAngleMultiplier();
                break;
            case MINECRAFT_VANILLA:
                multiplier = 1.05; // 5% wider for vanilla
                break;
        }

        return baseAngle * multiplier;
    }

    /**
     * Get adjusted indicator size for third person camera
     */
    public static float getAdjustedIndicatorSize(float baseSize) {
        if (!isThirdPersonActive()) {
            return baseSize;
        }

        ActiveThirdPersonMod activeMod = getActiveMod();
        float multiplier = 1.0f;

        switch (activeMod) {
            case SHOULDER_SURFING:
                multiplier = ShoulderSurfingCompat.getIndicatorSizeMultiplier();
                break;
            case LEAWIND:
                multiplier = LeawindCompat.getIndicatorSizeMultiplier();
                break;
            case MINECRAFT_VANILLA:
                multiplier = 1.1f; // 10% larger for vanilla
                break;
        }

        return baseSize * multiplier;
    }

    /**
     * Get adjusted rotation speed for third person camera
     */
    public static float getAdjustedRotationSpeed(float baseSpeed) {
        ActiveThirdPersonMod activeMod = getActiveMod();
        float multiplier = 1.0f;

        switch (activeMod) {
            case SHOULDER_SURFING:
                multiplier = ShoulderSurfingCompat.getRotationSpeedMultiplier();
                break;
            case LEAWIND:
                multiplier = LeawindCompat.getRotationSpeedMultiplier();
                break;
        }

        return baseSpeed * multiplier;
    }

    // === STATUS AND DEBUG ===

    /**
     * Get compatibility status string
     */
    public static String getCompatibilityStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Third Person Compatibility Status:\n");
        status.append("  Active Mod: ").append(getActiveMod().name()).append("\n");
        status.append("  Third Person Active: ").append(isThirdPersonActive()).append("\n");
        status.append("\n");
        status.append("Mod Status:\n");
        status.append("  Shoulder Surfing: ").append(ShoulderSurfingCompat.getStatusInfo()).append("\n");
        status.append("  Leawind: ").append(LeawindCompat.getStatusInfo()).append("\n");
        return status.toString();
    }

    /**
     * Get detailed status (alias for getCompatibilityStatus)
     */
    public static String getDetailedStatus() {
        return getCompatibilityStatus();
    }

    /**
     * Check if Shoulder Surfing camera is decoupled
     */
    public static boolean isShoulderSurfingDecoupled() {
        return ShoulderSurfingCompat.isCameraDecoupled();
    }

    // === LEGACY COMPATIBILITY (for existing code) ===

    /**
     * Placeholder for input control
     */
    public static void disableShoulderSurfingInput() {
        // Not implemented - may not be needed
    }

    /**
     * Placeholder for input control
     */
    public static void enableShoulderSurfingInput() {
        // Not implemented - may not be needed
    }

    /**
     * Get third person camera offset (basic implementation)
     */
    public static net.minecraft.world.phys.Vec3 getThirdPersonCameraOffset() {
        return net.minecraft.world.phys.Vec3.ZERO;
    }
}