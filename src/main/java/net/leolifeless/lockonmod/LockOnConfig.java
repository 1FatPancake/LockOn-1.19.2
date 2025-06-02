package net.leolifeless.lockonmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Enhanced Configuration handler for the Lock-On Mod with extensive customization options
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
     * Enhanced client-side configuration options
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

        // === CAMERA SETTINGS ===
        public final ForgeConfigSpec.DoubleValue rotationSpeed;
        public final ForgeConfigSpec.DoubleValue minRotationSpeed;
        public final ForgeConfigSpec.DoubleValue maxRotationSpeed;
        public final ForgeConfigSpec.DoubleValue distanceWeight;
        public final ForgeConfigSpec.BooleanValue enableSmoothCamera;
        public final ForgeConfigSpec.DoubleValue smoothingFactor;
        public final ForgeConfigSpec.BooleanValue predictiveTargeting;
        public final ForgeConfigSpec.DoubleValue predictionStrength;
        public final ForgeConfigSpec.BooleanValue autoBreakOnObstruction;
        public final ForgeConfigSpec.DoubleValue cameraOffset;

        // === VISUAL SETTINGS ===
        public final ForgeConfigSpec.EnumValue<IndicatorType> indicatorType;
        public final ForgeConfigSpec.DoubleValue indicatorSize;
        public final ForgeConfigSpec.DoubleValue pulseSpeed;
        public final ForgeConfigSpec.DoubleValue pulseAmplitude;
        public final ForgeConfigSpec.BooleanValue enablePulse;
        public final ForgeConfigSpec.BooleanValue enableGlow;
        public final ForgeConfigSpec.DoubleValue glowIntensity;
        public final ForgeConfigSpec.BooleanValue showDistance;
        public final ForgeConfigSpec.BooleanValue showHealthBar;
        public final ForgeConfigSpec.BooleanValue showTargetName;
        public final ForgeConfigSpec.EnumValue<DistanceUnit> distanceUnit;

        // === COLOR SETTINGS ===
        public final ForgeConfigSpec.IntValue indicatorColorRed;
        public final ForgeConfigSpec.IntValue indicatorColorGreen;
        public final ForgeConfigSpec.IntValue indicatorColorBlue;
        public final ForgeConfigSpec.IntValue indicatorColorAlpha;
        public final ForgeConfigSpec.IntValue outlineColorRed;
        public final ForgeConfigSpec.IntValue outlineColorGreen;
        public final ForgeConfigSpec.IntValue outlineColorBlue;
        public final ForgeConfigSpec.IntValue outlineColorAlpha;
        public final ForgeConfigSpec.IntValue textColorRed;
        public final ForgeConfigSpec.IntValue textColorGreen;
        public final ForgeConfigSpec.IntValue textColorBlue;
        public final ForgeConfigSpec.IntValue textColorAlpha;
        public final ForgeConfigSpec.BooleanValue dynamicColorBasedOnHealth;
        public final ForgeConfigSpec.BooleanValue dynamicColorBasedOnDistance;

        // === TARGET FILTER SETTINGS ===
        public final ForgeConfigSpec.BooleanValue targetPlayers;
        public final ForgeConfigSpec.BooleanValue targetHostileMobs;
        public final ForgeConfigSpec.BooleanValue targetPassiveMobs;
        public final ForgeConfigSpec.BooleanValue targetNeutralMobs;
        public final ForgeConfigSpec.BooleanValue targetBosses;
        public final ForgeConfigSpec.BooleanValue targetUndead;
        public final ForgeConfigSpec.BooleanValue targetAnimals;
        public final ForgeConfigSpec.BooleanValue targetVillagers;
        public final ForgeConfigSpec.BooleanValue targetTameable;
        public final ForgeConfigSpec.BooleanValue targetOwned;
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

        // === KEYBINDING SETTINGS ===
        public final ForgeConfigSpec.BooleanValue holdToMaintainLock;
        public final ForgeConfigSpec.BooleanValue toggleMode;
        public final ForgeConfigSpec.BooleanValue cycleThroughTargets;
        public final ForgeConfigSpec.BooleanValue reverseScrollCycling;

        // === PERFORMANCE SETTINGS ===
        public final ForgeConfigSpec.IntValue updateFrequency;
        public final ForgeConfigSpec.IntValue maxTargetsToSearch;
        public final ForgeConfigSpec.BooleanValue disableInCreative;
        public final ForgeConfigSpec.BooleanValue disableInSpectator;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Enhanced Lock-On Mod Client Configuration")
                    .push("general");

            // === TARGETING SETTINGS ===
            builder.comment("Advanced Targeting Settings").push("targeting");

            maxLockOnDistance = builder
                    .comment("Maximum distance to lock onto entities")
                    .defineInRange("maxLockOnDistance", 32.0, 5.0, 100.0);

            searchRadius = builder
                    .comment("Radius around player to search for entities")
                    .defineInRange("searchRadius", 10.0, 2.0, 50.0);

            targetingMode = builder
                    .comment("Targeting mode: CLOSEST, MOST_DAMAGED, CROSSHAIR_CENTERED, THREAT_LEVEL")
                    .defineEnum("targetingMode", TargetingMode.CROSSHAIR_CENTERED);

            requireLineOfSight = builder
                    .comment("Require line of sight to target entities")
                    .define("requireLineOfSight", true);

            penetrateGlass = builder
                    .comment("Allow targeting through glass blocks")
                    .define("penetrateGlass", true);

            targetingAngle = builder
                    .comment("Maximum angle from crosshair to consider entities (degrees)")
                    .defineInRange("targetingAngle", 45.0, 10.0, 180.0);

            smartTargeting = builder
                    .comment("Use intelligent targeting that considers multiple factors")
                    .define("smartTargeting", true);

            healthPriorityWeight = builder
                    .comment("Weight for health in smart targeting (lower health = higher priority)")
                    .defineInRange("healthPriorityWeight", 0.3, 0.0, 1.0);

            distancePriorityWeight = builder
                    .comment("Weight for distance in smart targeting (closer = higher priority)")
                    .defineInRange("distancePriorityWeight", 0.4, 0.0, 1.0);

            anglePriorityWeight = builder
                    .comment("Weight for crosshair angle in smart targeting")
                    .defineInRange("anglePriorityWeight", 0.3, 0.0, 1.0);

            builder.pop();

            // === CAMERA SETTINGS ===
            builder.comment("Enhanced Camera Settings").push("camera");

            rotationSpeed = builder
                    .comment("Base camera rotation speed")
                    .defineInRange("rotationSpeed", 0.25, 0.01, 2.0);

            minRotationSpeed = builder
                    .comment("Minimum rotation speed")
                    .defineInRange("minRotationSpeed", 0.1, 0.01, 1.0);

            maxRotationSpeed = builder
                    .comment("Maximum rotation speed")
                    .defineInRange("maxRotationSpeed", 1.0, 0.1, 3.0);

            distanceWeight = builder
                    .comment("How much distance affects rotation speed")
                    .defineInRange("distanceWeight", 0.5, 0.0, 1.0);

            enableSmoothCamera = builder
                    .comment("Enable smooth camera interpolation")
                    .define("enableSmoothCamera", true);

            smoothingFactor = builder
                    .comment("Camera smoothing strength (higher = smoother)")
                    .defineInRange("smoothingFactor", 0.7, 0.1, 0.95);

            predictiveTargeting = builder
                    .comment("Predict target movement for better tracking")
                    .define("predictiveTargeting", true);

            predictionStrength = builder
                    .comment("Strength of movement prediction")
                    .defineInRange("predictionStrength", 0.5, 0.0, 2.0);

            autoBreakOnObstruction = builder
                    .comment("Automatically break lock when target is obstructed")
                    .define("autoBreakOnObstruction", false);

            cameraOffset = builder
                    .comment("Vertical offset for camera targeting (0 = center, 1 = head)")
                    .defineInRange("cameraOffset", 0.5, 0.0, 1.0);

            builder.pop();

            // === VISUAL SETTINGS ===
            builder.comment("Enhanced Visual Settings").push("visual");

            indicatorType = builder
                    .comment("Type of lock-on indicator: CIRCLE, CROSSHAIR, DIAMOND, SQUARE, CUSTOM")
                    .defineEnum("indicatorType", IndicatorType.CIRCLE);

            indicatorSize = builder
                    .comment("Size of the lock-on indicator")
                    .defineInRange("indicatorSize", 0.5, 0.1, 3.0);

            pulseSpeed = builder
                    .comment("Speed of the pulse animation")
                    .defineInRange("pulseSpeed", 1.5, 0.1, 10.0);

            pulseAmplitude = builder
                    .comment("Size variation in pulse")
                    .defineInRange("pulseAmplitude", 0.15, 0.0, 1.0);

            enablePulse = builder
                    .comment("Enable pulsing animation")
                    .define("enablePulse", true);

            enableGlow = builder
                    .comment("Enable glow effect around indicator")
                    .define("enableGlow", true);

            glowIntensity = builder
                    .comment("Intensity of the glow effect")
                    .defineInRange("glowIntensity", 0.5, 0.0, 2.0);

            showDistance = builder
                    .comment("Show distance to target")
                    .define("showDistance", true);

            showHealthBar = builder
                    .comment("Show target health bar")
                    .define("showHealthBar", true);

            showTargetName = builder
                    .comment("Show target name/type")
                    .define("showTargetName", true);

            distanceUnit = builder
                    .comment("Unit for distance display: BLOCKS, METERS")
                    .defineEnum("distanceUnit", DistanceUnit.BLOCKS);

            builder.pop();

            // === COLOR SETTINGS ===
            builder.comment("Enhanced Color Settings").push("colors");

            // Indicator Color
            builder.comment("Primary Indicator Color").push("indicatorColor");
            indicatorColorRed = builder.defineInRange("red", 255, 0, 255);
            indicatorColorGreen = builder.defineInRange("green", 50, 0, 255);
            indicatorColorBlue = builder.defineInRange("blue", 50, 0, 255);
            indicatorColorAlpha = builder.defineInRange("alpha", 180, 0, 255);
            builder.pop();

            // Outline Color
            builder.comment("Outline Color").push("outlineColor");
            outlineColorRed = builder.defineInRange("red", 255, 0, 255);
            outlineColorGreen = builder.defineInRange("green", 255, 0, 255);
            outlineColorBlue = builder.defineInRange("blue", 255, 0, 255);
            outlineColorAlpha = builder.defineInRange("alpha", 220, 0, 255);
            builder.pop();

            // Text Color
            builder.comment("Text Color").push("textColor");
            textColorRed = builder.defineInRange("red", 255, 0, 255);
            textColorGreen = builder.defineInRange("green", 255, 0, 255);
            textColorBlue = builder.defineInRange("blue", 255, 0, 255);
            textColorAlpha = builder.defineInRange("alpha", 255, 0, 255);
            builder.pop();

            dynamicColorBasedOnHealth = builder
                    .comment("Change indicator color based on target health (red=low, green=high)")
                    .define("dynamicColorBasedOnHealth", false);

            dynamicColorBasedOnDistance = builder
                    .comment("Change indicator color based on distance (blue=close, red=far)")
                    .define("dynamicColorBasedOnDistance", false);

            builder.pop();

            // === TARGET FILTER SETTINGS ===
            builder.comment("Advanced Target Filter Settings").push("filters");

            targetPlayers = builder.define("targetPlayers", true);
            targetHostileMobs = builder.define("targetHostileMobs", true);
            targetPassiveMobs = builder.define("targetPassiveMobs", true);
            targetNeutralMobs = builder.define("targetNeutralMobs", true);
            targetBosses = builder.define("targetBosses", true);
            targetUndead = builder.define("targetUndead", true);
            targetAnimals = builder.define("targetAnimals", true);
            targetVillagers = builder.define("targetVillagers", false);
            targetTameable = builder.define("targetTameable", false);
            targetOwned = builder.define("targetOwned", false);

            minTargetHealth = builder
                    .comment("Minimum target health to consider")
                    .defineInRange("minTargetHealth", 0.0, 0.0, 1000.0);

            maxTargetHealth = builder
                    .comment("Maximum target health to consider (0 = no limit)")
                    .defineInRange("maxTargetHealth", 0.0, 0.0, 1000.0);

            entityBlacklist = builder
                    .comment("List of entity types to never target (e.g., 'minecraft:pig', 'minecraft:cow')")
                    .defineList("entityBlacklist", Arrays.asList(), obj -> obj instanceof String);

            entityWhitelist = builder
                    .comment("List of entity types to exclusively target (empty = target all allowed)")
                    .defineList("entityWhitelist", Arrays.asList(), obj -> obj instanceof String);

            useWhitelist = builder
                    .comment("Use whitelist instead of blacklist")
                    .define("useWhitelist", false);

            builder.pop();

            // === AUDIO SETTINGS ===
            builder.comment("Audio Settings").push("audio");

            enableSounds = builder.define("enableSounds", true);
            soundVolume = builder.defineInRange("soundVolume", 1.0, 0.0, 2.0);
            lockOnSound = builder.define("lockOnSound", true);
            targetSwitchSound = builder.define("targetSwitchSound", true);
            targetLostSound = builder.define("targetLostSound", true);

            builder.pop();

            // === KEYBINDING SETTINGS ===
            builder.comment("Keybinding Behavior Settings").push("keybinds");

            holdToMaintainLock = builder
                    .comment("Require holding key to maintain lock")
                    .define("holdToMaintainLock", false);

            toggleMode = builder
                    .comment("Toggle lock-on instead of hold")
                    .define("toggleMode", true);

            cycleThroughTargets = builder
                    .comment("Allow cycling through multiple targets")
                    .define("cycleThroughTargets", true);

            reverseScrollCycling = builder
                    .comment("Reverse direction when cycling targets")
                    .define("reverseScrollCycling", false);

            builder.pop();

            // === PERFORMANCE SETTINGS ===
            builder.comment("Performance Settings").push("performance");

            updateFrequency = builder
                    .comment("How often to update targeting (ticks, lower = more responsive)")
                    .defineInRange("updateFrequency", 1, 1, 20);

            maxTargetsToSearch = builder
                    .comment("Maximum number of entities to consider when targeting")
                    .defineInRange("maxTargetsToSearch", 50, 10, 200);

            disableInCreative = builder
                    .comment("Disable lock-on in creative mode")
                    .define("disableInCreative", false);

            disableInSpectator = builder
                    .comment("Disable lock-on in spectator mode")
                    .define("disableInSpectator", true);

            builder.pop();
            builder.pop(); // End general
        }
    }

    // === ENUMS FOR CONFIGURATION ===
    public enum TargetingMode {
        CLOSEST("Closest"),
        MOST_DAMAGED("Most Damaged"),
        CROSSHAIR_CENTERED("Crosshair Centered"),
        THREAT_LEVEL("Threat Level");

        private final String displayName;

        TargetingMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum IndicatorType {
        CIRCLE("Circle"),
        CROSSHAIR("Crosshair"),
        DIAMOND("Diamond"),
        SQUARE("Square"),
        CUSTOM("Custom");

        private final String displayName;

        IndicatorType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum DistanceUnit {
        BLOCKS("Blocks"),
        METERS("Meters");

        private final String displayName;

        DistanceUnit(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // === HELPER METHODS ===
    public static float getMaxLockOnDistance() { return CLIENT.maxLockOnDistance.get().floatValue(); }
    public static float getSearchRadius() { return CLIENT.searchRadius.get().floatValue(); }
    public static TargetingMode getTargetingMode() { return CLIENT.targetingMode.get(); }
    public static boolean requireLineOfSight() { return CLIENT.requireLineOfSight.get(); }
    public static boolean canPenetrateGlass() { return CLIENT.penetrateGlass.get(); }
    public static float getTargetingAngle() { return CLIENT.targetingAngle.get().floatValue(); }
    public static boolean isSmartTargeting() { return CLIENT.smartTargeting.get(); }
    public static float getHealthPriorityWeight() { return CLIENT.healthPriorityWeight.get().floatValue(); }
    public static float getDistancePriorityWeight() { return CLIENT.distancePriorityWeight.get().floatValue(); }
    public static float getAnglePriorityWeight() { return CLIENT.anglePriorityWeight.get().floatValue(); }

    public static float getRotationSpeed() { return CLIENT.rotationSpeed.get().floatValue(); }
    public static float getMinRotationSpeed() { return CLIENT.minRotationSpeed.get().floatValue(); }
    public static float getMaxRotationSpeed() { return CLIENT.maxRotationSpeed.get().floatValue(); }
    public static float getDistanceWeight() { return CLIENT.distanceWeight.get().floatValue(); }
    public static boolean isSmoothCameraEnabled() { return CLIENT.enableSmoothCamera.get(); }
    public static float getSmoothingFactor() { return CLIENT.smoothingFactor.get().floatValue(); }
    public static boolean isPredictiveTargeting() { return CLIENT.predictiveTargeting.get(); }
    public static float getPredictionStrength() { return CLIENT.predictionStrength.get().floatValue(); }
    public static boolean autoBreakOnObstruction() { return CLIENT.autoBreakOnObstruction.get(); }
    public static float getCameraOffset() { return CLIENT.cameraOffset.get().floatValue(); }

    public static IndicatorType getIndicatorType() { return CLIENT.indicatorType.get(); }
    public static float getIndicatorSize() { return CLIENT.indicatorSize.get().floatValue(); }
    public static float getPulseSpeed() { return CLIENT.pulseSpeed.get().floatValue(); }
    public static float getPulseAmplitude() { return CLIENT.pulseAmplitude.get().floatValue(); }
    public static boolean isPulseEnabled() { return CLIENT.enablePulse.get(); }
    public static boolean isGlowEnabled() { return CLIENT.enableGlow.get(); }
    public static float getGlowIntensity() { return CLIENT.glowIntensity.get().floatValue(); }
    public static boolean showDistance() { return CLIENT.showDistance.get(); }
    public static boolean showHealthBar() { return CLIENT.showHealthBar.get(); }
    public static boolean showTargetName() { return CLIENT.showTargetName.get(); }
    public static DistanceUnit getDistanceUnit() { return CLIENT.distanceUnit.get(); }

    public static Color getIndicatorColor() {
        return new Color(CLIENT.indicatorColorRed.get(), CLIENT.indicatorColorGreen.get(),
                CLIENT.indicatorColorBlue.get(), CLIENT.indicatorColorAlpha.get());
    }

    public static Color getOutlineColor() {
        return new Color(CLIENT.outlineColorRed.get(), CLIENT.outlineColorGreen.get(),
                CLIENT.outlineColorBlue.get(), CLIENT.outlineColorAlpha.get());
    }

    public static Color getTextColor() {
        return new Color(CLIENT.textColorRed.get(), CLIENT.textColorGreen.get(),
                CLIENT.textColorBlue.get(), CLIENT.textColorAlpha.get());
    }

    public static boolean isDynamicColorBasedOnHealth() { return CLIENT.dynamicColorBasedOnHealth.get(); }
    public static boolean isDynamicColorBasedOnDistance() { return CLIENT.dynamicColorBasedOnDistance.get(); }

    public static boolean canTargetPlayers() { return CLIENT.targetPlayers.get(); }
    public static boolean canTargetHostileMobs() { return CLIENT.targetHostileMobs.get(); }
    public static boolean canTargetPassiveMobs() { return CLIENT.targetPassiveMobs.get(); }
    public static boolean canTargetNeutralMobs() { return CLIENT.targetNeutralMobs.get(); }
    public static boolean canTargetBosses() { return CLIENT.targetBosses.get(); }
    public static boolean canTargetUndead() { return CLIENT.targetUndead.get(); }
    public static boolean canTargetAnimals() { return CLIENT.targetAnimals.get(); }
    public static boolean canTargetVillagers() { return CLIENT.targetVillagers.get(); }
    public static boolean canTargetTameable() { return CLIENT.targetTameable.get(); }
    public static boolean canTargetOwned() { return CLIENT.targetOwned.get(); }
    public static float getMinTargetHealth() { return CLIENT.minTargetHealth.get().floatValue(); }
    public static float getMaxTargetHealth() { return CLIENT.maxTargetHealth.get().floatValue(); }
    public static List<? extends String> getEntityBlacklist() { return CLIENT.entityBlacklist.get(); }
    public static List<? extends String> getEntityWhitelist() { return CLIENT.entityWhitelist.get(); }
    public static boolean useWhitelist() { return CLIENT.useWhitelist.get(); }

    public static boolean areSoundsEnabled() { return CLIENT.enableSounds.get(); }
    public static float getSoundVolume() { return CLIENT.soundVolume.get().floatValue(); }
    public static boolean playLockOnSound() { return CLIENT.lockOnSound.get(); }
    public static boolean playTargetSwitchSound() { return CLIENT.targetSwitchSound.get(); }
    public static boolean playTargetLostSound() { return CLIENT.targetLostSound.get(); }

    public static boolean holdToMaintainLock() { return CLIENT.holdToMaintainLock.get(); }
    public static boolean isToggleMode() { return CLIENT.toggleMode.get(); }
    public static boolean canCycleThroughTargets() { return CLIENT.cycleThroughTargets.get(); }
    public static boolean reverseScrollCycling() { return CLIENT.reverseScrollCycling.get(); }

    public static int getUpdateFrequency() { return CLIENT.updateFrequency.get(); }
    public static int getMaxTargetsToSearch() { return CLIENT.maxTargetsToSearch.get(); }
    public static boolean disableInCreative() { return CLIENT.disableInCreative.get(); }
    public static boolean disableInSpectator() { return CLIENT.disableInSpectator.get(); }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        LockOnMod.LOGGER.debug("Loaded Enhanced Target Lock Mod config file {}", configEvent.getConfig().getFileName());
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        LockOnMod.LOGGER.debug("Enhanced Target Lock Mod config reloaded");
    }
}