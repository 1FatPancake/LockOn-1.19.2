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
 * Enhanced Configuration handler for the Lock-On Mod
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
     * Client-side configuration options
     */
    public static class ClientConfig {
        // Targeting Settings
        public final ForgeConfigSpec.DoubleValue maxLockOnDistance;
        public final ForgeConfigSpec.DoubleValue searchRadius;
        public final ForgeConfigSpec.BooleanValue requireLineOfSight;
        public final ForgeConfigSpec.BooleanValue prioritizeClosestTarget;
        public final ForgeConfigSpec.BooleanValue prioritizeLookDirection;
        public final ForgeConfigSpec.DoubleValue lookDirectionWeight;
        public final ForgeConfigSpec.DoubleValue distanceTargetingWeight;
        public final ForgeConfigSpec.BooleanValue autoSwitchOnTargetDeath;
        public final ForgeConfigSpec.BooleanValue autoLockOnCombat;

        // Camera Settings
        public final ForgeConfigSpec.DoubleValue rotationSpeed;
        public final ForgeConfigSpec.DoubleValue minRotationSpeed;
        public final ForgeConfigSpec.DoubleValue maxRotationSpeed;
        public final ForgeConfigSpec.DoubleValue distanceWeight;
        public final ForgeConfigSpec.BooleanValue enableSmoothCamera;
        public final ForgeConfigSpec.DoubleValue cameraSmoothing;
        public final ForgeConfigSpec.BooleanValue enablePredictiveTargeting;
        public final ForgeConfigSpec.DoubleValue predictionStrength;
        public final ForgeConfigSpec.BooleanValue enableAdaptiveSpeed;
        public final ForgeConfigSpec.DoubleValue cameraDeadzone;

        // Visual Settings - Indicator
        public final ForgeConfigSpec.DoubleValue indicatorSize;
        public final ForgeConfigSpec.DoubleValue indicatorThickness;
        public final ForgeConfigSpec.EnumValue<IndicatorStyle> indicatorStyle;
        public final ForgeConfigSpec.BooleanValue enableIndicatorPulse;
        public final ForgeConfigSpec.DoubleValue pulseSpeed;
        public final ForgeConfigSpec.DoubleValue pulseAmplitude;
        public final ForgeConfigSpec.BooleanValue enableIndicatorRotation;
        public final ForgeConfigSpec.IntValue indicatorSegments;

        // Visual Settings - Colors
        public final ForgeConfigSpec.IntValue indicatorColorRed;
        public final ForgeConfigSpec.IntValue indicatorColorGreen;
        public final ForgeConfigSpec.IntValue indicatorColorBlue;
        public final ForgeConfigSpec.IntValue indicatorColorAlpha;
        public final ForgeConfigSpec.IntValue outlineColorRed;
        public final ForgeConfigSpec.IntValue outlineColorGreen;
        public final ForgeConfigSpec.IntValue outlineColorBlue;
        public final ForgeConfigSpec.IntValue outlineColorAlpha;
        public final ForgeConfigSpec.IntValue lowHealthColorRed;
        public final ForgeConfigSpec.IntValue lowHealthColorGreen;
        public final ForgeConfigSpec.IntValue lowHealthColorBlue;
        public final ForgeConfigSpec.IntValue lowHealthColorAlpha;

        // Visual Settings - Additional Effects
        public final ForgeConfigSpec.BooleanValue enableDistanceFading;
        public final ForgeConfigSpec.DoubleValue fadeStartDistance;
        public final ForgeConfigSpec.DoubleValue fadeEndDistance;
        public final ForgeConfigSpec.BooleanValue enableHealthColorChange;
        public final ForgeConfigSpec.DoubleValue lowHealthThreshold;
        public final ForgeConfigSpec.BooleanValue enableTargetHealthBar;
        public final ForgeConfigSpec.BooleanValue enableTargetNameDisplay;
        public final ForgeConfigSpec.BooleanValue enableCrosshairHiding;

        // Target Filter Settings - Basic
        public final ForgeConfigSpec.BooleanValue targetPlayers;
        public final ForgeConfigSpec.BooleanValue targetHostileMobs;
        public final ForgeConfigSpec.BooleanValue targetPassiveMobs;
        public final ForgeConfigSpec.BooleanValue targetNeutrals;
        public final ForgeConfigSpec.BooleanValue targetBosses;
        public final ForgeConfigSpec.BooleanValue targetTameable;
        public final ForgeConfigSpec.BooleanValue targetVehicles;

        // Target Filter Settings - Advanced
        public final ForgeConfigSpec.BooleanValue onlyTargetHostileToPlayer;
        public final ForgeConfigSpec.BooleanValue excludeCreative;
        public final ForgeConfigSpec.BooleanValue excludeSpectator;
        public final ForgeConfigSpec.BooleanValue excludeInvisible;
        public final ForgeConfigSpec.BooleanValue excludeBabies;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityBlacklist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityWhitelist;
        public final ForgeConfigSpec.BooleanValue useWhitelistMode;

        // Audio Settings
        public final ForgeConfigSpec.BooleanValue enableLockOnSound;
        public final ForgeConfigSpec.BooleanValue enableLockOffSound;
        public final ForgeConfigSpec.BooleanValue enableTargetSwitchSound;
        public final ForgeConfigSpec.DoubleValue soundVolume;

        // Performance Settings
        public final ForgeConfigSpec.IntValue targetingUpdateRate;
        public final ForgeConfigSpec.IntValue renderUpdateRate;
        public final ForgeConfigSpec.BooleanValue enableEntityCulling;
        public final ForgeConfigSpec.IntValue maxTargetsToConsider;

        // Keybinding Behavior Settings
        public final ForgeConfigSpec.BooleanValue toggleMode;
        public final ForgeConfigSpec.BooleanValue requireHoldToMaintain;
        public final ForgeConfigSpec.BooleanValue enableQuickCycle;
        public final ForgeConfigSpec.IntValue cycleCooldownMs;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            ForgeConfigSpec.DoubleValue rotationSpeed1;
            builder.comment("Lock-On Mod Enhanced Client Configuration")
                    .push("general");

            // Targeting Settings Section
            builder.comment("=== TARGETING SETTINGS ===",
                    "Configure how targets are selected and maintained").push("targeting");

            maxLockOnDistance = builder
                    .comment("Maximum distance to lock onto entities")
                    .defineInRange("maxLockOnDistance", 32.0, 5.0, 100.0);

            searchRadius = builder
                    .comment("Radius around player to search for entities")
                    .defineInRange("searchRadius", 10.0, 2.0, 50.0);

            requireLineOfSight = builder
                    .comment("Require clear line of sight to target entity")
                    .define("requireLineOfSight", true);

            prioritizeClosestTarget = builder
                    .comment("Prioritize closest targets over those in look direction")
                    .define("prioritizeClosestTarget", false);

            prioritizeLookDirection = builder
                    .comment("Prioritize targets in the direction you're looking")
                    .define("prioritizeLookDirection", true);

            lookDirectionWeight = builder
                    .comment("How much to weight look direction vs distance (higher = more direction-based)")
                    .defineInRange("lookDirectionWeight", 0.8, 0.0, 1.0);

            distanceTargetingWeight = builder
                    .comment("How much to weight distance in target selection (higher = prefer closer)")
                    .defineInRange("distanceTargetingWeight", 0.2, 0.0, 1.0);

            autoSwitchOnTargetDeath = builder
                    .comment("Automatically switch to new target when current target dies")
                    .define("autoSwitchOnTargetDeath", true);

            autoLockOnCombat = builder
                    .comment("Automatically lock onto entities that attack you")
                    .define("autoLockOnCombat", false);

            builder.pop();

            // Camera Settings Section
            builder.comment("=== CAMERA SETTINGS ===",
                    "Configure camera behavior and smoothing").push("camera");

            rotationSpeed1 = builder
                    .comment("Base camera rotation speed (lower = smoother but slower)")
                    .defineInRange("rotationSpeed", 0.25, 0.01, 1.0);

            minRotationSpeed = builder
                    .comment("Minimum rotation speed")
                    .defineInRange("minRotationSpeed", 0.3, 0.01, 1.0);

            maxRotationSpeed = builder
                    .comment("Maximum rotation speed")
                    .defineInRange("maxRotationSpeed", 0.8, 0.1, 2.0);

            distanceWeight = builder
                    .comment("How much distance affects rotation speed")
                    .defineInRange("distanceWeight", 0.5, 0.0, 1.0);

            enableSmoothCamera = builder
                    .comment("Enable smooth camera interpolation")
                    .define("enableSmoothCamera", true);

            cameraSmoothing = builder
                    .comment("Camera smoothing factor (higher = smoother)")
                    .defineInRange("cameraSmoothing", 0.7, 0.1, 0.95);

            enablePredictiveTargeting = builder
                    .comment("Predict target movement for better tracking")
                    .define("enablePredictiveTargeting", true);

            predictionStrength = builder
                    .comment("Strength of movement prediction")
                    .defineInRange("predictionStrength", 0.5, 0.0, 2.0);

            enableAdaptiveSpeed = builder
                    .comment("Adapt rotation speed based on angle difference")
                    .define("enableAdaptiveSpeed", true);

            cameraDeadzone = builder
                    .comment("Deadzone angle where camera won't adjust (degrees)")
                    .defineInRange("cameraDeadzone", 1.0, 0.0, 10.0);

            builder.pop();

            // Visual Settings Section - Indicator
            builder.comment("=== VISUAL SETTINGS - INDICATOR ===",
                    "Configure the appearance of the lock-on indicator").push("indicator");

            indicatorSize = builder
                    .comment("Size of the lock-on indicator")
                    .defineInRange("indicatorSize", 0.5, 0.1, 3.0);

            indicatorThickness = builder
                    .comment("Thickness of the indicator outline")
                    .defineInRange("indicatorThickness", 1.0, 0.1, 5.0);

            indicatorStyle = builder
                    .comment("Style of the lock-on indicator")
                    .defineEnum("indicatorStyle", IndicatorStyle.CIRCLE);

            enableIndicatorPulse = builder
                    .comment("Enable pulsing animation for the indicator")
                    .define("enableIndicatorPulse", true);

            pulseSpeed = builder
                    .comment("Speed of the pulse animation")
                    .defineInRange("pulseSpeed", 1.5, 0.1, 5.0);

            pulseAmplitude = builder
                    .comment("Size variation in pulse (as fraction of indicator size)")
                    .defineInRange("pulseAmplitude", 0.15, 0.0, 1.0);

            enableIndicatorRotation = builder
                    .comment("Enable rotation animation for the indicator")
                    .define("enableIndicatorRotation", false);

            rotationSpeed1 = builder
                    .comment("Speed of indicator rotation")
                    .defineInRange("rotationSpeed", 1.0, 0.1, 5.0);

            rotationSpeed = rotationSpeed1;
            indicatorSegments = builder
                    .comment("Number of segments for circular indicators (higher = smoother)")
                    .defineInRange("indicatorSegments", 36, 8, 72);

            builder.pop();

            // Visual Settings - Colors
            builder.comment("=== VISUAL SETTINGS - COLORS ===",
                    "Configure colors for various indicator states").push("colors");

            // Normal Indicator Color
            builder.comment("Normal Indicator Color").push("indicatorColor");
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

            // Low Health Color
            builder.comment("Low Health Indicator Color").push("lowHealthColor");
            lowHealthColorRed = builder.defineInRange("red", 255, 0, 255);
            lowHealthColorGreen = builder.defineInRange("green", 0, 0, 255);
            lowHealthColorBlue = builder.defineInRange("blue", 0, 0, 255);
            lowHealthColorAlpha = builder.defineInRange("alpha", 200, 0, 255);
            builder.pop();

            builder.pop(); // End colors

            // Visual Settings - Effects
            builder.comment("=== VISUAL SETTINGS - EFFECTS ===",
                    "Configure additional visual effects").push("effects");

            enableDistanceFading = builder
                    .comment("Fade indicator based on distance")
                    .define("enableDistanceFading", true);

            fadeStartDistance = builder
                    .comment("Distance at which fading starts")
                    .defineInRange("fadeStartDistance", 15.0, 5.0, 50.0);

            fadeEndDistance = builder
                    .comment("Distance at which indicator is fully faded")
                    .defineInRange("fadeEndDistance", 30.0, 10.0, 100.0);

            enableHealthColorChange = builder
                    .comment("Change color based on target's health")
                    .define("enableHealthColorChange", true);

            lowHealthThreshold = builder
                    .comment("Health percentage threshold for low health color")
                    .defineInRange("lowHealthThreshold", 0.25, 0.1, 0.9);

            enableTargetHealthBar = builder
                    .comment("Show health bar above target")
                    .define("enableTargetHealthBar", false);

            enableTargetNameDisplay = builder
                    .comment("Show target name above indicator")
                    .define("enableTargetNameDisplay", false);

            enableCrosshairHiding = builder
                    .comment("Hide crosshair when locked on")
                    .define("enableCrosshairHiding", false);

            builder.pop();

            // Target Filter Settings - Basic
            builder.comment("=== TARGET FILTERS - BASIC ===",
                    "Configure which types of entities can be targeted").push("basicFilters");

            targetPlayers = builder
                    .comment("Allow targeting players")
                    .define("targetPlayers", true);

            targetHostileMobs = builder
                    .comment("Allow targeting hostile mobs")
                    .define("targetHostileMobs", true);

            targetPassiveMobs = builder
                    .comment("Allow targeting passive mobs")
                    .define("targetPassiveMobs", true);

            targetNeutrals = builder
                    .comment("Allow targeting neutral mobs")
                    .define("targetNeutrals", true);

            targetBosses = builder
                    .comment("Allow targeting boss mobs")
                    .define("targetBosses", true);

            targetTameable = builder
                    .comment("Allow targeting tameable mobs (pets, etc.)")
                    .define("targetTameable", false);

            targetVehicles = builder
                    .comment("Allow targeting vehicles (boats, minecarts, etc.)")
                    .define("targetVehicles", false);

            builder.pop();

            // Target Filter Settings - Advanced
            builder.comment("=== TARGET FILTERS - ADVANCED ===",
                    "Advanced filtering options").push("advancedFilters");

            onlyTargetHostileToPlayer = builder
                    .comment("Only target entities that are hostile to the player")
                    .define("onlyTargetHostileToPlayer", false);

            excludeCreative = builder
                    .comment("Exclude players in creative mode")
                    .define("excludeCreative", true);

            excludeSpectator = builder
                    .comment("Exclude players in spectator mode")
                    .define("excludeSpectator", true);

            excludeInvisible = builder
                    .comment("Exclude invisible entities")
                    .define("excludeInvisible", false);

            excludeBabies = builder
                    .comment("Exclude baby mobs")
                    .define("excludeBabies", false);

            entityBlacklist = builder
                    .comment("Entity types to never target (e.g., 'minecraft:villager', 'minecraft:iron_golem')")
                    .defineList("entityBlacklist", List.of("minecraft:villager"), obj -> obj instanceof String);

            entityWhitelist = builder
                    .comment("Entity types to exclusively target (empty = target all allowed types)")
                    .defineList("entityWhitelist", List.of(), obj -> obj instanceof String);

            useWhitelistMode = builder
                    .comment("Use whitelist mode (only target entities in whitelist)")
                    .define("useWhitelistMode", false);

            builder.pop();

            // Audio Settings
            builder.comment("=== AUDIO SETTINGS ===",
                    "Configure sound effects").push("audio");

            enableLockOnSound = builder
                    .comment("Play sound when locking onto target")
                    .define("enableLockOnSound", true);

            enableLockOffSound = builder
                    .comment("Play sound when unlocking from target")
                    .define("enableLockOffSound", true);

            enableTargetSwitchSound = builder
                    .comment("Play sound when switching targets")
                    .define("enableTargetSwitchSound", true);

            soundVolume = builder
                    .comment("Volume of lock-on sounds")
                    .defineInRange("soundVolume", 0.5, 0.0, 1.0);

            builder.pop();

            // Performance Settings
            builder.comment("=== PERFORMANCE SETTINGS ===",
                    "Configure performance-related options").push("performance");

            targetingUpdateRate = builder
                    .comment("How often to update targeting (ticks between updates, lower = more responsive)")
                    .defineInRange("targetingUpdateRate", 1, 1, 20);

            renderUpdateRate = builder
                    .comment("How often to update rendering (ticks between updates)")
                    .defineInRange("renderUpdateRate", 1, 1, 10);

            enableEntityCulling = builder
                    .comment("Skip targeting entities outside of view")
                    .define("enableEntityCulling", true);

            maxTargetsToConsider = builder
                    .comment("Maximum number of entities to consider for targeting")
                    .defineInRange("maxTargetsToConsider", 50, 10, 200);

            builder.pop();

            // Keybinding Behavior
            builder.comment("=== KEYBINDING BEHAVIOR ===",
                    "Configure how keybindings work").push("keybinds");

            toggleMode = builder
                    .comment("Use toggle mode for lock-on (press once to lock, press again to unlock)")
                    .define("toggleMode", true);

            requireHoldToMaintain = builder
                    .comment("Require holding the key to maintain lock-on (overrides toggle mode)")
                    .define("requireHoldToMaintain", false);

            enableQuickCycle = builder
                    .comment("Enable quick cycling through targets with repeated presses")
                    .define("enableQuickCycle", true);

            cycleCooldownMs = builder
                    .comment("Cooldown between target cycles in milliseconds")
                    .defineInRange("cycleCooldownMs", 250, 50, 2000);

            builder.pop(); // End keybinds
            builder.pop(); // End general
        }
    }

    /**
     * Enum for different indicator styles
     */
    public enum IndicatorStyle {
        CIRCLE("Circle"),
        CROSSHAIR("Crosshair"),
        SQUARE("Square"),
        DIAMOND("Diamond"),
        TRIANGLE("Triangle"),
        HEXAGON("Hexagon"),
        STAR("Star"),
        PLUS("Plus");

        private final String displayName;

        IndicatorStyle(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Helper methods to easily access config values

    // Targeting Settings
    public static float getMaxLockOnDistance() {
        return CLIENT.maxLockOnDistance.get().floatValue();
    }

    public static float getSearchRadius() {
        return CLIENT.searchRadius.get().floatValue();
    }

    public static boolean requireLineOfSight() {
        return CLIENT.requireLineOfSight.get();
    }

    public static boolean prioritizeClosestTarget() {
        return CLIENT.prioritizeClosestTarget.get();
    }

    public static boolean prioritizeLookDirection() {
        return CLIENT.prioritizeLookDirection.get();
    }

    public static float getLookDirectionWeight() {
        return CLIENT.lookDirectionWeight.get().floatValue();
    }

    public static float getDistanceTargetingWeight() {
        return CLIENT.distanceTargetingWeight.get().floatValue();
    }

    public static boolean autoSwitchOnTargetDeath() {
        return CLIENT.autoSwitchOnTargetDeath.get();
    }

    public static boolean autoLockOnCombat() {
        return CLIENT.autoLockOnCombat.get();
    }

    // Camera Settings
    public static float getRotationSpeed() {
        return CLIENT.rotationSpeed.get().floatValue();
    }

    public static float getMinRotationSpeed() {
        return CLIENT.minRotationSpeed.get().floatValue();
    }

    public static float getMaxRotationSpeed() {
        return CLIENT.maxRotationSpeed.get().floatValue();
    }

    public static float getDistanceWeight() {
        return CLIENT.distanceWeight.get().floatValue();
    }

    public static boolean isSmoothCameraEnabled() {
        return CLIENT.enableSmoothCamera.get();
    }

    public static float getCameraSmoothing() {
        return CLIENT.cameraSmoothing.get().floatValue();
    }

    public static boolean isPredictiveTargetingEnabled() {
        return CLIENT.enablePredictiveTargeting.get();
    }

    public static float getPredictionStrength() {
        return CLIENT.predictionStrength.get().floatValue();
    }

    public static boolean isAdaptiveSpeedEnabled() {
        return CLIENT.enableAdaptiveSpeed.get();
    }

    public static float getCameraDeadzone() {
        return CLIENT.cameraDeadzone.get().floatValue();
    }

    // Visual Settings - Indicator
    public static float getIndicatorSize() {
        return CLIENT.indicatorSize.get().floatValue();
    }

    public static float getIndicatorThickness() {
        return CLIENT.indicatorThickness.get().floatValue();
    }

    public static IndicatorStyle getIndicatorStyle() {
        return CLIENT.indicatorStyle.get();
    }

    public static boolean isIndicatorPulseEnabled() {
        return CLIENT.enableIndicatorPulse.get();
    }

    public static float getPulseSpeed() {
        return CLIENT.pulseSpeed.get().floatValue();
    }

    public static float getPulseAmplitude() {
        return CLIENT.pulseAmplitude.get().floatValue();
    }

    public static boolean isIndicatorRotationEnabled() {
        return CLIENT.enableIndicatorRotation.get();
    }

    public static float getIndicatorRotationSpeed() {
        return CLIENT.rotationSpeed.get().floatValue();
    }

    public static int getIndicatorSegments() {
        return CLIENT.indicatorSegments.get();
    }

    // Color Settings
    public static Color getIndicatorColor() {
        return new Color(
                CLIENT.indicatorColorRed.get(),
                CLIENT.indicatorColorGreen.get(),
                CLIENT.indicatorColorBlue.get(),
                CLIENT.indicatorColorAlpha.get()
        );
    }

    public static Color getOutlineColor() {
        return new Color(
                CLIENT.outlineColorRed.get(),
                CLIENT.outlineColorGreen.get(),
                CLIENT.outlineColorBlue.get(),
                CLIENT.outlineColorAlpha.get()
        );
    }

    public static Color getLowHealthColor() {
        return new Color(
                CLIENT.lowHealthColorRed.get(),
                CLIENT.lowHealthColorGreen.get(),
                CLIENT.lowHealthColorBlue.get(),
                CLIENT.lowHealthColorAlpha.get()
        );
    }

    // Effect Settings
    public static boolean isDistanceFadingEnabled() {
        return CLIENT.enableDistanceFading.get();
    }

    public static float getFadeStartDistance() {
        return CLIENT.fadeStartDistance.get().floatValue();
    }

    public static float getFadeEndDistance() {
        return CLIENT.fadeEndDistance.get().floatValue();
    }

    public static boolean isHealthColorChangeEnabled() {
        return CLIENT.enableHealthColorChange.get();
    }

    public static float getLowHealthThreshold() {
        return CLIENT.lowHealthThreshold.get().floatValue();
    }

    public static boolean isTargetHealthBarEnabled() {
        return CLIENT.enableTargetHealthBar.get();
    }

    public static boolean isTargetNameDisplayEnabled() {
        return CLIENT.enableTargetNameDisplay.get();
    }

    public static boolean isCrosshairHidingEnabled() {
        return CLIENT.enableCrosshairHiding.get();
    }

    // Basic Filter Settings
    public static boolean canTargetPlayers() {
        return CLIENT.targetPlayers.get();
    }

    public static boolean canTargetHostileMobs() {
        return CLIENT.targetHostileMobs.get();
    }

    public static boolean canTargetPassiveMobs() {
        return CLIENT.targetPassiveMobs.get();
    }

    public static boolean canTargetNeutrals() {
        return CLIENT.targetNeutrals.get();
    }

    public static boolean canTargetBosses() {
        return CLIENT.targetBosses.get();
    }

    public static boolean canTargetTameable() {
        return CLIENT.targetTameable.get();
    }

    public static boolean canTargetVehicles() {
        return CLIENT.targetVehicles.get();
    }

    // Advanced Filter Settings
    public static boolean onlyTargetHostileToPlayer() {
        return CLIENT.onlyTargetHostileToPlayer.get();
    }

    public static boolean excludeCreative() {
        return CLIENT.excludeCreative.get();
    }

    public static boolean excludeSpectator() {
        return CLIENT.excludeSpectator.get();
    }

    public static boolean excludeInvisible() {
        return CLIENT.excludeInvisible.get();
    }

    public static boolean excludeBabies() {
        return CLIENT.excludeBabies.get();
    }

    public static List<? extends String> getEntityBlacklist() {
        return CLIENT.entityBlacklist.get();
    }

    public static List<? extends String> getEntityWhitelist() {
        return CLIENT.entityWhitelist.get();
    }

    public static boolean useWhitelistMode() {
        return CLIENT.useWhitelistMode.get();
    }

    // Audio Settings
    public static boolean isLockOnSoundEnabled() {
        return CLIENT.enableLockOnSound.get();
    }

    public static boolean isLockOffSoundEnabled() {
        return CLIENT.enableLockOffSound.get();
    }

    public static boolean isTargetSwitchSoundEnabled() {
        return CLIENT.enableTargetSwitchSound.get();
    }

    public static float getSoundVolume() {
        return CLIENT.soundVolume.get().floatValue();
    }

    // Performance Settings
    public static int getTargetingUpdateRate() {
        return CLIENT.targetingUpdateRate.get();
    }

    public static int getRenderUpdateRate() {
        return CLIENT.renderUpdateRate.get();
    }

    public static boolean isEntityCullingEnabled() {
        return CLIENT.enableEntityCulling.get();
    }

    public static int getMaxTargetsToConsider() {
        return CLIENT.maxTargetsToConsider.get();
    }

    // Keybinding Settings
    public static boolean isToggleMode() {
        return CLIENT.toggleMode.get();
    }

    public static boolean requireHoldToMaintain() {
        return CLIENT.requireHoldToMaintain.get();
    }

    public static boolean isQuickCycleEnabled() {
        return CLIENT.enableQuickCycle.get();
    }

    public static int getCycleCooldownMs() {
        return CLIENT.cycleCooldownMs.get();
    }

    /**
     * Handle the config loading event
     */
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        LockOnMod.LOGGER.debug("Loaded Lock-On Mod config file {}", configEvent.getConfig().getFileName());
    }

    /**
     * Handle the config reloading event
     */
    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        LockOnMod.LOGGER.debug("Lock-On Mod config reloaded");
    }
}