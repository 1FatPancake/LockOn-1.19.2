package net.leolifeless.lockonmod;

import net.leolifeless.lockonmod.compat.ThirdPersonCompatibility;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Enhanced Configuration handler for the Lock-On Mod with third person compatibility
 */
@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LockOnConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    /**
     * Enhanced client-side configuration options with third person support
     */
    public static class ClientConfig {
        // === TARGETING SETTINGS ===
        public final ForgeConfigSpec.DoubleValue maxLockOnDistance;
        public final ForgeConfigSpec.DoubleValue searchRadius;
        public final ForgeConfigSpec.EnumValue<TargetingMode> targetingMode;
        public final ForgeConfigSpec.BooleanValue requireLineOfSight;
        public final ForgeConfigSpec.BooleanValue penetrateGlass;
        public final ForgeConfigSpec.DoubleValue targetingAngle;
        public final ForgeConfigSpec.BooleanValue smartTargeting;
        public final ForgeConfigSpec.DoubleValue healthPriorityWeight;
        public final ForgeConfigSpec.DoubleValue distancePriorityWeight;
        public final ForgeConfigSpec.DoubleValue anglePriorityWeight;

        // === THIRD PERSON COMPATIBILITY SETTINGS ===
        public final ForgeConfigSpec.BooleanValue enableThirdPersonEnhancements;
        public final ForgeConfigSpec.DoubleValue thirdPersonRangeMultiplier;
        public final ForgeConfigSpec.DoubleValue thirdPersonAngleMultiplier;
        public final ForgeConfigSpec.DoubleValue thirdPersonSmoothingFactor;
        public final ForgeConfigSpec.DoubleValue thirdPersonRotationSpeedMultiplier;
        public final ForgeConfigSpec.DoubleValue thirdPersonIndicatorSizeMultiplier;
        public final ForgeConfigSpec.BooleanValue adjustForCameraOffset;
        public final ForgeConfigSpec.BooleanValue enhancedThirdPersonSmoothing;
        public final ForgeConfigSpec.BooleanValue autoDetectThirdPerson;

        // === CAMERA SETTINGS ===
        public final ForgeConfigSpec.DoubleValue rotationSpeed;
        public final ForgeConfigSpec.DoubleValue minRotationSpeed;
        public final ForgeConfigSpec.DoubleValue maxRotationSpeed;
        public final ForgeConfigSpec.BooleanValue smoothCameraEnabled;
        public final ForgeConfigSpec.DoubleValue cameraSmoothness;
        public final ForgeConfigSpec.BooleanValue adaptiveRotationEnabled;
        public final ForgeConfigSpec.BooleanValue predictiveTargeting;
        public final ForgeConfigSpec.BooleanValue autoBreakOnObstruction;

        // === VISUAL SETTINGS ===
        public final ForgeConfigSpec.EnumValue<IndicatorType> indicatorType;
        public final ForgeConfigSpec.DoubleValue indicatorSize;
        public final ForgeConfigSpec.BooleanValue pulseEnabled;
        public final ForgeConfigSpec.BooleanValue glowEnabled;
        public final ForgeConfigSpec.BooleanValue showTargetDistance;
        public final ForgeConfigSpec.BooleanValue showTargetHealth;
        public final ForgeConfigSpec.BooleanValue showTargetName;
        public final ForgeConfigSpec.BooleanValue showDistance;
        public final ForgeConfigSpec.BooleanValue showHealthBar;
        public final ForgeConfigSpec.EnumValue<DistanceUnit> distanceUnit;

        // === COLOR SETTINGS ===
        public final ForgeConfigSpec.ConfigValue<String> indicatorColorHex;
        public final ForgeConfigSpec.ConfigValue<String> outlineColorHex;
        public final ForgeConfigSpec.ConfigValue<String> textColorHex;
        public final ForgeConfigSpec.BooleanValue dynamicHealthColorEnabled;
        public final ForgeConfigSpec.BooleanValue dynamicDistanceColorEnabled;

        // === FILTER SETTINGS ===
        public final ForgeConfigSpec.BooleanValue canTargetPlayers;
        public final ForgeConfigSpec.BooleanValue canTargetHostileMobs;
        public final ForgeConfigSpec.BooleanValue canTargetPassiveMobs;
        public final ForgeConfigSpec.BooleanValue canTargetBosses;
        public final ForgeConfigSpec.DoubleValue minTargetHealth;
        public final ForgeConfigSpec.DoubleValue maxTargetHealth;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityBlacklist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityWhitelist;
        public final ForgeConfigSpec.BooleanValue useWhitelist;

        // === AUDIO SETTINGS ===
        public final ForgeConfigSpec.BooleanValue enableSounds;
        public final ForgeConfigSpec.DoubleValue soundVolume;
        public final ForgeConfigSpec.BooleanValue lockOnSound;
        public final ForgeConfigSpec.BooleanValue targetSwitchSound;
        public final ForgeConfigSpec.BooleanValue targetLostSound;

        // === KEYBINDING BEHAVIOR ===
        public final ForgeConfigSpec.BooleanValue holdToMaintainLock;
        public final ForgeConfigSpec.BooleanValue toggleMode;
        public final ForgeConfigSpec.BooleanValue cycleThroughTargets;
        public final ForgeConfigSpec.BooleanValue reverseScrollCycling;

        // === PERFORMANCE SETTINGS ===
        public final ForgeConfigSpec.IntValue updateFrequency;
        public final ForgeConfigSpec.IntValue maxTargetsToSearch;
        public final ForgeConfigSpec.BooleanValue disableInCreative;
        public final ForgeConfigSpec.BooleanValue disableInSpectator;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            // === TARGETING SETTINGS ===
            builder.comment("Targeting Settings")
                    .push("targeting");

            maxLockOnDistance = builder
                    .comment("Maximum distance to lock onto targets")
                    .defineInRange("maxLockOnDistance", 50.0, 5.0, 200.0);

            searchRadius = builder
                    .comment("Radius around player to search for targets")
                    .defineInRange("searchRadius", 30.0, 5.0, 100.0);

            targetingMode = builder
                    .comment("Default targeting mode")
                    .defineEnum("targetingMode", TargetingMode.CLOSEST);

            requireLineOfSight = builder
                    .comment("Require line of sight to target")
                    .define("requireLineOfSight", true);

            penetrateGlass = builder
                    .comment("Allow targeting through glass blocks")
                    .define("penetrateGlass", true);

            targetingAngle = builder
                    .comment("Maximum angle from crosshair to consider targets (degrees)")
                    .defineInRange("targetingAngle", 45.0, 10.0, 180.0);

            smartTargeting = builder
                    .comment("Use intelligent targeting with multiple factors")
                    .define("smartTargeting", true);

            healthPriorityWeight = builder
                    .comment("Weight for health-based targeting priority")
                    .defineInRange("healthPriorityWeight", 0.3, 0.0, 1.0);

            distancePriorityWeight = builder
                    .comment("Weight for distance-based targeting priority")
                    .defineInRange("distancePriorityWeight", 0.4, 0.0, 1.0);

            anglePriorityWeight = builder
                    .comment("Weight for angle-based targeting priority")
                    .defineInRange("anglePriorityWeight", 0.3, 0.0, 1.0);

            builder.pop();

            // === THIRD PERSON COMPATIBILITY SETTINGS ===
            builder.comment("Third Person Compatibility Settings",
                            "These settings enhance the targeting experience when using third-person camera mods",
                            "like Leawind's Third Person mod")
                    .push("thirdPersonCompat");

            enableThirdPersonEnhancements = builder
                    .comment("Enable enhanced features when third-person mods are detected")
                    .define("enableThirdPersonEnhancements", true);

            thirdPersonRangeMultiplier = builder
                    .comment("Multiplier for targeting range in third person mode")
                    .defineInRange("thirdPersonRangeMultiplier", 1.2, 0.5, 3.0);

            thirdPersonAngleMultiplier = builder
                    .comment("Multiplier for targeting angle in third person mode")
                    .defineInRange("thirdPersonAngleMultiplier", 1.3, 0.5, 2.0);

            thirdPersonSmoothingFactor = builder
                    .comment("Additional smoothing factor for third person camera")
                    .defineInRange("thirdPersonSmoothingFactor", 1.15, 0.5, 2.0);

            thirdPersonRotationSpeedMultiplier = builder
                    .comment("Rotation speed multiplier for third person mode")
                    .defineInRange("thirdPersonRotationSpeedMultiplier", 0.85, 0.1, 2.0);

            thirdPersonIndicatorSizeMultiplier = builder
                    .comment("Indicator size multiplier for third person mode")
                    .defineInRange("thirdPersonIndicatorSizeMultiplier", 1.0, 0.5, 2.0);

            adjustForCameraOffset = builder
                    .comment("Adjust targeting calculations for third person camera offset")
                    .define("adjustForCameraOffset", true);

            enhancedThirdPersonSmoothing = builder
                    .comment("Use enhanced smoothing algorithms in third person")
                    .define("enhancedThirdPersonSmoothing", true);

            autoDetectThirdPerson = builder
                    .comment("Automatically detect and adjust for third person perspective")
                    .define("autoDetectThirdPerson", true);

            builder.pop();

            // === CAMERA SETTINGS ===
            builder.comment("Camera Settings")
                    .push("camera");

            rotationSpeed = builder
                    .comment("Base camera rotation speed")
                    .defineInRange("rotationSpeed", 0.15, 0.01, 1.0);

            minRotationSpeed = builder
                    .comment("Minimum adaptive rotation speed")
                    .defineInRange("minRotationSpeed", 0.05, 0.01, 0.5);

            maxRotationSpeed = builder
                    .comment("Maximum adaptive rotation speed")
                    .defineInRange("maxRotationSpeed", 0.5, 0.1, 2.0);

            smoothCameraEnabled = builder
                    .comment("Enable smooth camera interpolation")
                    .define("smoothCameraEnabled", true);

            cameraSmoothness = builder
                    .comment("Camera smoothness factor (higher = smoother)")
                    .defineInRange("cameraSmoothness", 0.1, 0.01, 1.0);

            adaptiveRotationEnabled = builder
                    .comment("Enable adaptive rotation speed based on distance and angle")
                    .define("adaptiveRotationEnabled", true);

            predictiveTargeting = builder
                    .comment("Predict target movement for better tracking")
                    .define("predictiveTargeting", false);

            autoBreakOnObstruction = builder
                    .comment("Automatically break lock when target is obstructed")
                    .define("autoBreakOnObstruction", true);

            builder.pop();

            // === VISUAL SETTINGS ===
            builder.comment("Visual Settings")
                    .push("visual");

            indicatorType = builder
                    .comment("Type of lock-on indicator to display")
                    .defineEnum("indicatorType", IndicatorType.CIRCLE);

            indicatorSize = builder
                    .comment("Size of the lock-on indicator")
                    .defineInRange("indicatorSize", 1.0, 0.1, 5.0);

            pulseEnabled = builder
                    .comment("Enable pulsing animation for indicators")
                    .define("pulseEnabled", true);

            glowEnabled = builder
                    .comment("Enable glow effect around indicators")
                    .define("glowEnabled", true);

            showTargetDistance = builder
                    .comment("Display distance to target")
                    .define("showTargetDistance", true);

            showTargetHealth = builder
                    .comment("Display target health information")
                    .define("showTargetHealth", true);

            showTargetName = builder
                    .comment("Display target name/type")
                    .define("showTargetName", true);

            // Legacy compatibility
            showDistance = builder
                    .comment("Display distance to target (legacy)")
                    .define("showDistance", true);

            showHealthBar = builder
                    .comment("Display target health bar (legacy)")
                    .define("showHealthBar", true);

            distanceUnit = builder
                    .comment("Unit for displaying distance")
                    .defineEnum("distanceUnit", DistanceUnit.BLOCKS);

            builder.pop();

            // === COLOR SETTINGS ===
            builder.comment("Color Settings (use hex format: #RRGGBB or #AARRGGBB)")
                    .push("colors");

            indicatorColorHex = builder
                    .comment("Primary color of the lock-on indicator")
                    .define("indicatorColorHex", "#FF6600");

            outlineColorHex = builder
                    .comment("Color of the indicator outline")
                    .define("outlineColorHex", "#FFFFFF");

            textColorHex = builder
                    .comment("Color of information text")
                    .define("textColorHex", "#FFFFFF");

            dynamicHealthColorEnabled = builder
                    .comment("Change indicator color based on target health")
                    .define("dynamicHealthColorEnabled", true);

            dynamicDistanceColorEnabled = builder
                    .comment("Change indicator color based on distance")
                    .define("dynamicDistanceColorEnabled", false);

            builder.pop();

            // === FILTER SETTINGS ===
            builder.comment("Target Filter Settings")
                    .push("filters");

            canTargetPlayers = builder
                    .comment("Allow targeting other players")
                    .define("canTargetPlayers", true);

            canTargetHostileMobs = builder
                    .comment("Allow targeting hostile mobs")
                    .define("canTargetHostileMobs", true);

            canTargetPassiveMobs = builder
                    .comment("Allow targeting passive mobs")
                    .define("canTargetPassiveMobs", false);

            canTargetBosses = builder
                    .comment("Allow targeting boss mobs")
                    .define("canTargetBosses", true);

            minTargetHealth = builder
                    .comment("Minimum target health to consider")
                    .defineInRange("minTargetHealth", 1.0, 0.1, 1000.0);

            maxTargetHealth = builder
                    .comment("Maximum target health to consider (0 = no limit)")
                    .defineInRange("maxTargetHealth", 0.0, 0.0, 10000.0);

            entityBlacklist = builder
                    .comment("List of entity IDs to never target")
                    .defineList("entityBlacklist", Arrays.asList("minecraft:villager", "minecraft:cat"), obj -> obj instanceof String);

            entityWhitelist = builder
                    .comment("List of entity IDs to exclusively target (when whitelist is enabled)")
                    .defineList("entityWhitelist", Arrays.asList(), obj -> obj instanceof String);

            useWhitelist = builder
                    .comment("Use whitelist instead of blacklist")
                    .define("useWhitelist", false);

            builder.pop();

            // === AUDIO SETTINGS ===
            builder.comment("Audio Settings")
                    .push("audio");

            enableSounds = builder
                    .comment("Enable audio feedback")
                    .define("enableSounds", true);

            soundVolume = builder
                    .comment("Volume level for lock-on sounds")
                    .defineInRange("soundVolume", 1.0, 0.0, 2.0);

            lockOnSound = builder
                    .comment("Play sound when locking onto target")
                    .define("lockOnSound", true);

            targetSwitchSound = builder
                    .comment("Play sound when switching targets")
                    .define("targetSwitchSound", true);

            targetLostSound = builder
                    .comment("Play sound when losing target")
                    .define("targetLostSound", true);

            builder.pop();

            // === KEYBINDING BEHAVIOR ===
            builder.comment("Keybinding Behavior")
                    .push("keybinds");

            holdToMaintainLock = builder
                    .comment("Require holding key to maintain target lock")
                    .define("holdToMaintainLock", false);

            toggleMode = builder
                    .comment("Use toggle mode (press once to lock, press again to unlock)")
                    .define("toggleMode", true);

            cycleThroughTargets = builder
                    .comment("Enable cycling through multiple targets")
                    .define("cycleThroughTargets", true);

            reverseScrollCycling = builder
                    .comment("Reverse scroll direction for target cycling")
                    .define("reverseScrollCycling", false);

            builder.pop();

            // === PERFORMANCE SETTINGS ===
            builder.comment("Performance Settings")
                    .push("performance");

            updateFrequency = builder
                    .comment("Update frequency in ticks (lower = more responsive)")
                    .defineInRange("updateFrequency", 1, 1, 20);

            maxTargetsToSearch = builder
                    .comment("Maximum number of targets to search")
                    .defineInRange("maxTargetsToSearch", 50, 5, 200);

            disableInCreative = builder
                    .comment("Disable lock-on in creative mode")
                    .define("disableInCreative", false);

            disableInSpectator = builder
                    .comment("Disable lock-on in spectator mode")
                    .define("disableInSpectator", true);

            builder.pop();
        }
    }

    // === ENUMS ===
    public enum TargetingMode {
        CLOSEST,
        MOST_DAMAGED,
        CROSSHAIR_CENTERED,
        THREAT_LEVEL,
        SMART
    }

    public enum IndicatorType {
        CIRCLE,
        CROSSHAIR,
        DIAMOND,
        SQUARE,
        CUSTOM
    }

    public enum DistanceUnit {
        BLOCKS,
        METERS
    }

    // === STATIC ACCESSORS ===

    // Targeting Settings
    public static double getMaxLockOnDistance() {
        double baseDistance = CLIENT.maxLockOnDistance.get();
        if (ThirdPersonCompatibility.isThirdPersonActive() && CLIENT.enableThirdPersonEnhancements.get()) {
            return ThirdPersonCompatibility.getAdjustedTargetingRange(baseDistance);
        }
        return baseDistance;
    }

    public static double getSearchRadius() { return CLIENT.searchRadius.get(); }
    public static TargetingMode getTargetingMode() { return CLIENT.targetingMode.get(); }
    public static boolean requireLineOfSight() { return CLIENT.requireLineOfSight.get(); }
    public static boolean penetrateGlass() { return CLIENT.penetrateGlass.get(); }

    public static double getTargetingAngle() {
        double baseAngle = CLIENT.targetingAngle.get();
        if (ThirdPersonCompatibility.isThirdPersonActive() && CLIENT.enableThirdPersonEnhancements.get()) {
            return ThirdPersonCompatibility.getAdjustedTargetingAngle(baseAngle);
        }
        return baseAngle;
    }

    public static boolean isSmartTargetingEnabled() { return CLIENT.smartTargeting.get(); }
    public static double getHealthPriorityWeight() { return CLIENT.healthPriorityWeight.get(); }
    public static double getDistancePriorityWeight() { return CLIENT.distancePriorityWeight.get(); }
    public static double getAnglePriorityWeight() { return CLIENT.anglePriorityWeight.get(); }

    // Third Person Settings
    public static boolean areThirdPersonEnhancementsEnabled() { return CLIENT.enableThirdPersonEnhancements.get(); }
    public static double getThirdPersonRangeMultiplier() { return CLIENT.thirdPersonRangeMultiplier.get(); }
    public static double getThirdPersonAngleMultiplier() { return CLIENT.thirdPersonAngleMultiplier.get(); }
    public static double getThirdPersonSmoothingFactor() { return CLIENT.thirdPersonSmoothingFactor.get(); }
    public static double getThirdPersonRotationSpeedMultiplier() { return CLIENT.thirdPersonRotationSpeedMultiplier.get(); }
    public static double getThirdPersonIndicatorSizeMultiplier() { return CLIENT.thirdPersonIndicatorSizeMultiplier.get(); }
    public static boolean shouldAdjustForCameraOffset() { return CLIENT.adjustForCameraOffset.get(); }
    public static boolean isEnhancedThirdPersonSmoothingEnabled() { return CLIENT.enhancedThirdPersonSmoothing.get(); }
    public static boolean isAutoDetectThirdPersonEnabled() { return CLIENT.autoDetectThirdPerson.get(); }

    // Camera Settings
    public static float getRotationSpeed() { return CLIENT.rotationSpeed.get().floatValue(); }
    public static float getMinRotationSpeed() { return CLIENT.minRotationSpeed.get().floatValue(); }
    public static float getMaxRotationSpeed() { return CLIENT.maxRotationSpeed.get().floatValue(); }
    public static boolean isSmoothCameraEnabled() { return CLIENT.smoothCameraEnabled.get(); }
    public static float getCameraSmoothness() { return CLIENT.cameraSmoothness.get().floatValue(); }
    public static boolean isAdaptiveRotationEnabled() { return CLIENT.adaptiveRotationEnabled.get(); }
    public static boolean isPredictiveTargetingEnabled() { return CLIENT.predictiveTargeting.get(); }
    public static boolean isAutoBreakOnObstructionEnabled() { return CLIENT.autoBreakOnObstruction.get(); }

    // Visual Settings
    public static IndicatorType getIndicatorType() { return CLIENT.indicatorType.get(); }
    public static float getIndicatorSize() {
        float baseSize = CLIENT.indicatorSize.get().floatValue();
        if (ThirdPersonCompatibility.isThirdPersonActive() && CLIENT.enableThirdPersonEnhancements.get()) {
            return ThirdPersonCompatibility.getAdjustedIndicatorSize(baseSize);
        }
        return baseSize;
    }
    public static boolean isPulseEnabled() { return CLIENT.pulseEnabled.get(); }
    public static boolean isGlowEnabled() { return CLIENT.glowEnabled.get(); }
    public static boolean showTargetDistance() { return CLIENT.showTargetDistance.get(); }
    public static boolean showTargetHealth() { return CLIENT.showTargetHealth.get(); }
    public static boolean showTargetName() { return CLIENT.showTargetName.get(); }

    // Legacy method compatibility
    public static boolean showDistance() { return CLIENT.showDistance.get(); }
    public static boolean showHealthBar() { return CLIENT.showHealthBar.get(); }
    public static DistanceUnit getDistanceUnit() { return CLIENT.distanceUnit.get(); }

    // Color Settings
    public static Color getIndicatorColor() { return parseColor(CLIENT.indicatorColorHex.get(), Color.ORANGE); }
    public static Color getOutlineColor() { return parseColor(CLIENT.outlineColorHex.get(), Color.WHITE); }
    public static Color getTextColor() { return parseColor(CLIENT.textColorHex.get(), Color.WHITE); }
    public static boolean isDynamicHealthColorEnabled() { return CLIENT.dynamicHealthColorEnabled.get(); }
    public static boolean isDynamicDistanceColorEnabled() { return CLIENT.dynamicDistanceColorEnabled.get(); }

    // Filter Settings
    public static boolean canTargetPlayers() { return CLIENT.canTargetPlayers.get(); }
    public static boolean canTargetHostileMobs() { return CLIENT.canTargetHostileMobs.get(); }
    public static boolean canTargetPassiveMobs() { return CLIENT.canTargetPassiveMobs.get(); }
    public static boolean canTargetBosses() { return CLIENT.canTargetBosses.get(); }
    public static double getMinTargetHealth() { return CLIENT.minTargetHealth.get(); }
    public static double getMaxTargetHealth() { return CLIENT.maxTargetHealth.get(); }
    public static List<? extends String> getEntityBlacklist() { return CLIENT.entityBlacklist.get(); }
    public static List<? extends String> getEntityWhitelist() { return CLIENT.entityWhitelist.get(); }
    public static boolean useWhitelist() { return CLIENT.useWhitelist.get(); }

    // Audio Settings
    public static boolean areSoundsEnabled() { return CLIENT.enableSounds.get(); }
    public static float getSoundVolume() { return CLIENT.soundVolume.get().floatValue(); }
    public static boolean playLockOnSound() { return CLIENT.lockOnSound.get(); }
    public static boolean playTargetSwitchSound() { return CLIENT.targetSwitchSound.get(); }
    public static boolean playTargetLostSound() { return CLIENT.targetLostSound.get(); }

    // Keybinding Settings
    public static boolean holdToMaintainLock() { return CLIENT.holdToMaintainLock.get(); }
    public static boolean isToggleMode() { return CLIENT.toggleMode.get(); }
    public static boolean canCycleThroughTargets() { return CLIENT.cycleThroughTargets.get(); }
    public static boolean reverseScrollCycling() { return CLIENT.reverseScrollCycling.get(); }

    // Performance Settings
    public static int getUpdateFrequency() { return CLIENT.updateFrequency.get(); }
    public static int getMaxTargetsToSearch() { return CLIENT.maxTargetsToSearch.get(); }
    public static boolean disableInCreative() { return CLIENT.disableInCreative.get(); }
    public static boolean disableInSpectator() { return CLIENT.disableInSpectator.get(); }

    /**
     * Parse color from hex string
     */
    private static Color parseColor(String hex, Color fallback) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }

            if (hex.length() == 6) {
                return new Color(Integer.parseInt(hex, 16));
            } else if (hex.length() == 8) {
                return new Color(Integer.parseUnsignedInt(hex, 16), true);
            }
        } catch (NumberFormatException e) {
            LockOnMod.LOGGER.warn("Invalid color format: {}, using fallback", hex);
        }
        return fallback;
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        LockOnMod.LOGGER.debug("Loaded Enhanced Target Lock Mod config file {}", configEvent.getConfig().getFileName());
        LockOnMod.LOGGER.info("Third Person Compatibility Status: {}", ThirdPersonCompatibility.getCompatibilityStatus());
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        LockOnMod.LOGGER.debug("Enhanced Target Lock Mod config reloaded");
        LockOnMod.LOGGER.info("Third Person Compatibility Status: {}", ThirdPersonCompatibility.getCompatibilityStatus());
    }
}