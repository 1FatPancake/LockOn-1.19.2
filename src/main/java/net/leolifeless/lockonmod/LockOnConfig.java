package net.leolifeless.lockonmod;

import net.leolifeless.lockonmod.compat.ThirdPersonCompatibility;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Enhanced Configuration handler for the Lock-On Mod (1.16.5) with third person compatibility
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
     * Enhanced client-side configuration options with third person support (1.16.5)
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
            builder.comment("Third Person Compatibility Settings (1.16.5)",
                            "These settings enhance the targeting experience when using third-person camera mods",
                            "like Shoulder Surfing Reloaded")
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

    // === STATIC ACCESSORS WITH SAFE ERROR HANDLING FOR 1.16.5 ===

    // Targeting Settings
    public static double getMaxLockOnDistance() {
        try {
            double baseDistance = CLIENT.maxLockOnDistance.get();
            if (ThirdPersonCompatibility.isThirdPersonActive() && CLIENT.enableThirdPersonEnhancements.get()) {
                return ThirdPersonCompatibility.getAdjustedTargetingRange(baseDistance);
            }
            return baseDistance;
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting max lock-on distance, using default: {}", e.getMessage());
            return 50.0;
        }
    }

    public static double getSearchRadius() {
        try {
            return CLIENT.searchRadius.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting search radius, using default: {}", e.getMessage());
            return 30.0;
        }
    }

    public static TargetingMode getTargetingMode() {
        try {
            return CLIENT.targetingMode.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting targeting mode, using default: {}", e.getMessage());
            return TargetingMode.CLOSEST;
        }
    }

    public static boolean requireLineOfSight() {
        try {
            return CLIENT.requireLineOfSight.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting line of sight setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean penetrateGlass() {
        try {
            return CLIENT.penetrateGlass.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting penetrate glass setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static double getTargetingAngle() {
        try {
            double baseAngle = CLIENT.targetingAngle.get();
            if (ThirdPersonCompatibility.isThirdPersonActive() && CLIENT.enableThirdPersonEnhancements.get()) {
                return ThirdPersonCompatibility.getAdjustedTargetingAngle(baseAngle);
            }
            return baseAngle;
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting targeting angle, using default: {}", e.getMessage());
            return 45.0;
        }
    }

    public static boolean isSmartTargetingEnabled() {
        try {
            return CLIENT.smartTargeting.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting smart targeting setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static double getHealthPriorityWeight() {
        try {
            return CLIENT.healthPriorityWeight.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting health priority weight, using default: {}", e.getMessage());
            return 0.3;
        }
    }

    public static double getDistancePriorityWeight() {
        try {
            return CLIENT.distancePriorityWeight.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting distance priority weight, using default: {}", e.getMessage());
            return 0.4;
        }
    }

    public static double getAnglePriorityWeight() {
        try {
            return CLIENT.anglePriorityWeight.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting angle priority weight, using default: {}", e.getMessage());
            return 0.3;
        }
    }

    // Third Person Settings
    public static boolean areThirdPersonEnhancementsEnabled() {
        try {
            return CLIENT.enableThirdPersonEnhancements.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting third person enhancements setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static double getThirdPersonRangeMultiplier() {
        try {
            return CLIENT.thirdPersonRangeMultiplier.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting third person range multiplier, using default: {}", e.getMessage());
            return 1.2;
        }
    }

    public static double getThirdPersonAngleMultiplier() {
        try {
            return CLIENT.thirdPersonAngleMultiplier.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting third person angle multiplier, using default: {}", e.getMessage());
            return 1.3;
        }
    }

    // Camera Settings
    public static float getRotationSpeed() {
        try {
            return CLIENT.rotationSpeed.get().floatValue();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting rotation speed, using default: {}", e.getMessage());
            return 0.15f;
        }
    }

    public static float getMinRotationSpeed() {
        try {
            return CLIENT.minRotationSpeed.get().floatValue();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting min rotation speed, using default: {}", e.getMessage());
            return 0.05f;
        }
    }

    public static float getMaxRotationSpeed() {
        try {
            return CLIENT.maxRotationSpeed.get().floatValue();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting max rotation speed, using default: {}", e.getMessage());
            return 0.5f;
        }
    }

    public static boolean isSmoothCameraEnabled() {
        try {
            return CLIENT.smoothCameraEnabled.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting smooth camera setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static float getCameraSmoothness() {
        try {
            return CLIENT.cameraSmoothness.get().floatValue();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting camera smoothness, using default: {}", e.getMessage());
            return 0.1f;
        }
    }

    public static boolean isAdaptiveRotationEnabled() {
        try {
            return CLIENT.adaptiveRotationEnabled.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting adaptive rotation setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean isPredictiveTargetingEnabled() {
        try {
            return CLIENT.predictiveTargeting.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting predictive targeting setting, using default: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isAutoBreakOnObstructionEnabled() {
        try {
            return CLIENT.autoBreakOnObstruction.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting auto break setting, using default: {}", e.getMessage());
            return true;
        }
    }

    // Visual Settings
    public static IndicatorType getIndicatorType() {
        try {
            return CLIENT.indicatorType.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting indicator type, using default: {}", e.getMessage());
            return IndicatorType.CIRCLE;
        }
    }

    public static float getIndicatorSize() {
        try {
            float baseSize = CLIENT.indicatorSize.get().floatValue();
            if (ThirdPersonCompatibility.isThirdPersonActive() && CLIENT.enableThirdPersonEnhancements.get()) {
                return ThirdPersonCompatibility.getAdjustedIndicatorSize(baseSize);
            }
            return baseSize;
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting indicator size, using default: {}", e.getMessage());
            return 1.0f;
        }
    }

    public static boolean isPulseEnabled() {
        try {
            return CLIENT.pulseEnabled.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting pulse setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean isGlowEnabled() {
        try {
            return CLIENT.glowEnabled.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting glow setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean showTargetDistance() {
        try {
            return CLIENT.showTargetDistance.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting show target distance setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean showTargetHealth() {
        try {
            return CLIENT.showTargetHealth.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting show target health setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean showTargetName() {
        try {
            return CLIENT.showTargetName.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting show target name setting, using default: {}", e.getMessage());
            return true;
        }
    }

    // Legacy method compatibility
    public static boolean showDistance() {
        try {
            return CLIENT.showDistance.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting show distance setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean showHealthBar() {
        try {
            return CLIENT.showHealthBar.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting show health bar setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static DistanceUnit getDistanceUnit() {
        try {
            return CLIENT.distanceUnit.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting distance unit, using default: {}", e.getMessage());
            return DistanceUnit.BLOCKS;
        }
    }

    // Color Settings
    public static Color getIndicatorColor() {
        try {
            return parseColor(CLIENT.indicatorColorHex.get(), Color.ORANGE);
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting indicator color, using default: {}", e.getMessage());
            return Color.ORANGE;
        }
    }

    public static Color getOutlineColor() {
        try {
            return parseColor(CLIENT.outlineColorHex.get(), Color.WHITE);
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting outline color, using default: {}", e.getMessage());
            return Color.WHITE;
        }
    }

    public static Color getTextColor() {
        try {
            return parseColor(CLIENT.textColorHex.get(), Color.WHITE);
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting text color, using default: {}", e.getMessage());
            return Color.WHITE;
        }
    }

    public static boolean isDynamicHealthColorEnabled() {
        try {
            return CLIENT.dynamicHealthColorEnabled.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting dynamic health color setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean isDynamicDistanceColorEnabled() {
        try {
            return CLIENT.dynamicDistanceColorEnabled.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting dynamic distance color setting, using default: {}", e.getMessage());
            return false;
        }
    }

    // Filter Settings
    public static boolean canTargetPlayers() {
        try {
            return CLIENT.canTargetPlayers.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting can target players setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean canTargetHostileMobs() {
        try {
            return CLIENT.canTargetHostileMobs.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting can target hostiles setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean canTargetPassiveMobs() {
        try {
            return CLIENT.canTargetPassiveMobs.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting can target passives setting, using default: {}", e.getMessage());
            return false;
        }
    }

    public static boolean canTargetBosses() {
        try {
            return CLIENT.canTargetBosses.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting can target bosses setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static double getMinTargetHealth() {
        try {
            return CLIENT.minTargetHealth.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting min target health, using default: {}", e.getMessage());
            return 1.0;
        }
    }

    public static double getMaxTargetHealth() {
        try {
            return CLIENT.maxTargetHealth.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting max target health, using default: {}", e.getMessage());
            return 0.0;
        }
    }

    public static List<? extends String> getEntityBlacklist() {
        try {
            return CLIENT.entityBlacklist.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting entity blacklist, using default: {}", e.getMessage());
            return Arrays.asList("minecraft:villager", "minecraft:cat");
        }
    }

    public static List<? extends String> getEntityWhitelist() {
        try {
            return CLIENT.entityWhitelist.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting entity whitelist, using default: {}", e.getMessage());
            return Arrays.asList();
        }
    }

    public static boolean useWhitelist() {
        try {
            return CLIENT.useWhitelist.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting use whitelist setting, using default: {}", e.getMessage());
            return false;
        }
    }

    // Audio Settings
    public static boolean areSoundsEnabled() {
        try {
            return CLIENT.enableSounds.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting sounds enabled setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static float getSoundVolume() {
        try {
            return CLIENT.soundVolume.get().floatValue();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting sound volume, using default: {}", e.getMessage());
            return 1.0f;
        }
    }

    public static boolean playLockOnSound() {
        try {
            return CLIENT.lockOnSound.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting lock on sound setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean playTargetSwitchSound() {
        try {
            return CLIENT.targetSwitchSound.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting target switch sound setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean playTargetLostSound() {
        try {
            return CLIENT.targetLostSound.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting target lost sound setting, using default: {}", e.getMessage());
            return true;
        }
    }

    // Keybinding Settings
    public static boolean holdToMaintainLock() {
        try {
            return CLIENT.holdToMaintainLock.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting hold to maintain lock setting, using default: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isToggleMode() {
        try {
            return CLIENT.toggleMode.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting toggle mode setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean canCycleThroughTargets() {
        try {
            return CLIENT.cycleThroughTargets.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting cycle targets setting, using default: {}", e.getMessage());
            return true;
        }
    }

    public static boolean reverseScrollCycling() {
        try {
            return CLIENT.reverseScrollCycling.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting reverse scroll setting, using default: {}", e.getMessage());
            return false;
        }
    }

    // Performance Settings
    public static int getUpdateFrequency() {
        try {
            return CLIENT.updateFrequency.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting update frequency, using default: {}", e.getMessage());
            return 1;
        }
    }

    public static int getMaxTargetsToSearch() {
        try {
            return CLIENT.maxTargetsToSearch.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting max targets to search, using default: {}", e.getMessage());
            return 50;
        }
    }

    public static boolean disableInCreative() {
        try {
            return CLIENT.disableInCreative.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting disable in creative setting, using default: {}", e.getMessage());
            return false;
        }
    }

    public static boolean disableInSpectator() {
        try {
            return CLIENT.disableInSpectator.get();
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error getting disable in spectator setting, using default: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Parse color from hex string with enhanced error handling for 1.16.5
     */
    private static Color parseColor(String hex, Color fallback) {
        try {
            if (hex == null || hex.isEmpty()) {
                return fallback;
            }

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
        } catch (Exception e) {
            LockOnMod.LOGGER.warn("Error parsing color: {}, using fallback: {}", hex, e.getMessage());
        }
        return fallback;
    }
}