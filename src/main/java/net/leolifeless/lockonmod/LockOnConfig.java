package net.leolifeless.lockonmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LockOnConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static class ClientConfig {

        // === TARGETING ===
        public final ForgeConfigSpec.DoubleValue  maxLockOnDistance;
        public final ForgeConfigSpec.DoubleValue  targetingAngle;
        public final ForgeConfigSpec.EnumValue<TargetingMode> targetingMode;
        public final ForgeConfigSpec.BooleanValue requireLineOfSight;
        public final ForgeConfigSpec.DoubleValue  distancePriorityWeight;
        public final ForgeConfigSpec.DoubleValue  anglePriorityWeight;
        public final ForgeConfigSpec.IntValue     maxTargetsToSearch;
        public final ForgeConfigSpec.IntValue     updateFrequency;

        // === CAMERA ===
        public final ForgeConfigSpec.DoubleValue  rotationSpeed;
        public final ForgeConfigSpec.BooleanValue enableSmoothCamera;
        public final ForgeConfigSpec.BooleanValue autoBreakOnObstruction;

        // === INDICATOR ===
        public final ForgeConfigSpec.EnumValue<IndicatorType> indicatorType;
        public final ForgeConfigSpec.DoubleValue  indicatorSize;
        public final ForgeConfigSpec.BooleanValue enablePulse;
        public final ForgeConfigSpec.DoubleValue  pulseSpeed;
        public final ForgeConfigSpec.DoubleValue  pulseAmplitude;
        public final ForgeConfigSpec.BooleanValue enableGlow;
        public final ForgeConfigSpec.DoubleValue  glowIntensity;
        public final ForgeConfigSpec.BooleanValue dynamicColorBasedOnHealth;
        public final ForgeConfigSpec.BooleanValue dynamicColorBasedOnDistance;
        public final ForgeConfigSpec.ConfigValue<String> customIndicatorName;

        // === INDICATOR COLOR ===
        public final ForgeConfigSpec.IntValue indicatorColorRed;
        public final ForgeConfigSpec.IntValue indicatorColorGreen;
        public final ForgeConfigSpec.IntValue indicatorColorBlue;
        public final ForgeConfigSpec.IntValue indicatorColorAlpha;

        // === OUTLINE COLOR ===
        public final ForgeConfigSpec.IntValue outlineColorRed;
        public final ForgeConfigSpec.IntValue outlineColorGreen;
        public final ForgeConfigSpec.IntValue outlineColorBlue;
        public final ForgeConfigSpec.IntValue outlineColorAlpha;

        // === HUD ===
        public final ForgeConfigSpec.EnumValue<HudVariant> hudVariant;
        public final ForgeConfigSpec.BooleanValue showTargetName;
        public final ForgeConfigSpec.BooleanValue showHealthBar;
        public final ForgeConfigSpec.BooleanValue showDistance;
        public final ForgeConfigSpec.EnumValue<DistanceUnit> distanceUnit;

        // === FILTERS ===
        public final ForgeConfigSpec.BooleanValue targetPlayers;
        public final ForgeConfigSpec.BooleanValue targetHostileMobs;
        public final ForgeConfigSpec.BooleanValue targetPassiveMobs;
        public final ForgeConfigSpec.BooleanValue targetBosses;
        public final ForgeConfigSpec.DoubleValue  minTargetHealth;
        public final ForgeConfigSpec.DoubleValue  maxTargetHealth;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityBlacklist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityWhitelist;
        public final ForgeConfigSpec.BooleanValue useWhitelist;

        // === AUDIO ===
        public final ForgeConfigSpec.BooleanValue enableSounds;
        public final ForgeConfigSpec.DoubleValue  soundVolume;
        public final ForgeConfigSpec.BooleanValue lockOnSound;
        public final ForgeConfigSpec.BooleanValue targetSwitchSound;
        public final ForgeConfigSpec.BooleanValue targetLostSound;

        // === KEYBINDS BEHAVIOR ===
        public final ForgeConfigSpec.BooleanValue toggleMode;
        public final ForgeConfigSpec.BooleanValue holdToMaintainLock;
        public final ForgeConfigSpec.BooleanValue cycleThroughTargets;
        public final ForgeConfigSpec.BooleanValue reverseScrollCycling;

        // === GAME MODE ===
        public final ForgeConfigSpec.BooleanValue disableInCreative;
        public final ForgeConfigSpec.BooleanValue disableInSpectator;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("lockon");

            // --- Targeting ---
            builder.comment("Targeting").push("targeting");

            maxLockOnDistance = builder
                    .comment("Maximum distance to lock onto entities")
                    .defineInRange("maxLockOnDistance", 32.0, 5.0, 100.0);

            targetingAngle = builder
                    .comment("Maximum angle from crosshair to consider entities (degrees)")
                    .defineInRange("targetingAngle", 45.0, 10.0, 180.0);

            targetingMode = builder
                    .comment("Targeting priority mode")
                    .defineEnum("targetingMode", TargetingMode.CROSSHAIR_CENTERED);

            requireLineOfSight = builder
                    .comment("Require line of sight to target entities")
                    .define("requireLineOfSight", true);

            distancePriorityWeight = builder
                    .comment("Weight for distance in targeting priority")
                    .defineInRange("distancePriorityWeight", 0.4, 0.0, 1.0);

            anglePriorityWeight = builder
                    .comment("Weight for crosshair angle in targeting priority")
                    .defineInRange("anglePriorityWeight", 0.3, 0.0, 1.0);

            maxTargetsToSearch = builder
                    .comment("Maximum entities to evaluate when finding a target")
                    .defineInRange("maxTargetsToSearch", 50, 10, 200);

            updateFrequency = builder
                    .comment("How often to update targeting in ticks (1 = every tick)")
                    .defineInRange("updateFrequency", 1, 1, 20);

            builder.pop();

            // --- Camera ---
            builder.comment("Camera").push("camera");

            rotationSpeed = builder
                    .comment("Base camera rotation speed when locked on")
                    .defineInRange("rotationSpeed", 0.15, 0.01, 2.0);

            enableSmoothCamera = builder
                    .comment("Enable smooth camera interpolation")
                    .define("enableSmoothCamera", true);

            autoBreakOnObstruction = builder
                    .comment("Automatically break lock when target is obstructed")
                    .define("autoBreakOnObstruction", false);

            builder.pop();

            // --- Indicator ---
            builder.comment("World-space lock-on indicator").push("indicator");

            indicatorType = builder
                    .comment("Indicator shape: CIRCLE, CROSSHAIR, DIAMOND, SQUARE, CUSTOM")
                    .defineEnum("indicatorType", IndicatorType.CIRCLE);

            indicatorSize = builder
                    .comment("Size of the lock-on indicator")
                    .defineInRange("indicatorSize", 0.5, 0.1, 3.0);

            enablePulse = builder
                    .comment("Enable pulsing animation")
                    .define("enablePulse", true);

            pulseSpeed = builder
                    .comment("Speed of the pulse animation")
                    .defineInRange("pulseSpeed", 1.5, 0.1, 10.0);

            pulseAmplitude = builder
                    .comment("Size variation in pulse")
                    .defineInRange("pulseAmplitude", 0.15, 0.0, 1.0);

            enableGlow = builder
                    .comment("Enable glow effect around indicator")
                    .define("enableGlow", true);

            glowIntensity = builder
                    .comment("Intensity of the glow effect")
                    .defineInRange("glowIntensity", 0.5, 0.0, 2.0);

            dynamicColorBasedOnHealth = builder
                    .comment("Shift indicator color based on target health")
                    .define("dynamicColorBasedOnHealth", false);

            dynamicColorBasedOnDistance = builder
                    .comment("Shift indicator color based on distance")
                    .define("dynamicColorBasedOnDistance", false);

            customIndicatorName = builder
                    .comment("Custom indicator texture name (used when type is CUSTOM)")
                    .define("customIndicatorName", "default");

            builder.comment("Indicator fill color").push("color");
            indicatorColorRed   = builder.defineInRange("red",   255, 0, 255);
            indicatorColorGreen = builder.defineInRange("green",  50, 0, 255);
            indicatorColorBlue  = builder.defineInRange("blue",   50, 0, 255);
            indicatorColorAlpha = builder.defineInRange("alpha", 180, 0, 255);
            builder.pop();

            builder.comment("Indicator outline color").push("outlineColor");
            outlineColorRed   = builder.defineInRange("red",   255, 0, 255);
            outlineColorGreen = builder.defineInRange("green", 255, 0, 255);
            outlineColorBlue  = builder.defineInRange("blue",  255, 0, 255);
            outlineColorAlpha = builder.defineInRange("alpha", 220, 0, 255);
            builder.pop();

            builder.pop(); // indicator

            // --- HUD ---
            builder.comment("HUD display options").push("hud");

            hudVariant = builder
                    .comment("HUD display style: CLASSIC, MINIMAL, COMPACT")
                    .defineEnum("hudVariant", HudVariant.CLASSIC);

            showTargetName = builder.define("showTargetName", true);
            showHealthBar  = builder.define("showHealthBar",  true);
            showDistance   = builder.define("showDistance",   true);

            distanceUnit = builder
                    .comment("Distance display unit: BLOCKS or METERS")
                    .defineEnum("distanceUnit", DistanceUnit.BLOCKS);

            builder.pop();

            // --- Filters ---
            builder.comment("Entity targeting filters").push("filters");

            targetPlayers     = builder.define("targetPlayers",     true);
            targetHostileMobs = builder.define("targetHostileMobs", true);
            targetPassiveMobs = builder.define("targetPassiveMobs", true);
            targetBosses      = builder.define("targetBosses",      true);

            minTargetHealth = builder
                    .comment("Minimum health required to target an entity")
                    .defineInRange("minTargetHealth", 0.0, 0.0, 1000.0);

            maxTargetHealth = builder
                    .comment("Maximum health to target (0 = no limit)")
                    .defineInRange("maxTargetHealth", 0.0, 0.0, 1000.0);

            entityBlacklist = builder
                    .comment("Entity IDs to never target")
                    .defineListAllowEmpty("entityBlacklist",
                            Arrays.asList("minecraft:villager", "minecraft:cat"),
                            obj -> obj instanceof String);

            entityWhitelist = builder
                    .comment("Entity IDs to exclusively target (requires useWhitelist = true)")
                    .defineListAllowEmpty("entityWhitelist",
                            Arrays.asList(),
                            obj -> obj instanceof String);

            useWhitelist = builder
                    .comment("Use whitelist instead of blacklist")
                    .define("useWhitelist", false);

            builder.pop();

            // --- Audio ---
            builder.comment("Sound settings").push("audio");

            enableSounds      = builder.define("enableSounds",      true);
            soundVolume       = builder.defineInRange("soundVolume", 1.0, 0.0, 2.0);
            lockOnSound       = builder.define("lockOnSound",        true);
            targetSwitchSound = builder.define("targetSwitchSound",  true);
            targetLostSound   = builder.define("targetLostSound",    true);

            builder.pop();

            // --- Keybind behavior ---
            builder.comment("Keybind behavior").push("keybinds");

            toggleMode = builder
                    .comment("Toggle lock-on with one press instead of holding")
                    .define("toggleMode", true);

            holdToMaintainLock = builder
                    .comment("Require holding the key to maintain lock")
                    .define("holdToMaintainLock", false);

            cycleThroughTargets = builder
                    .comment("Allow cycling through multiple nearby targets")
                    .define("cycleThroughTargets", true);

            reverseScrollCycling = builder
                    .comment("Reverse scroll direction when cycling targets")
                    .define("reverseScrollCycling", false);

            builder.pop();

            // --- Game mode ---
            builder.comment("Game mode restrictions").push("gamemode");

            disableInCreative  = builder.define("disableInCreative",  false);
            disableInSpectator = builder.define("disableInSpectator",  true);

            builder.pop();

            builder.pop(); // lockon
        }
    }

    // === ENUMS ===

    public enum TargetingMode {
        CLOSEST, MOST_DAMAGED, CROSSHAIR_CENTERED, THREAT_LEVEL
    }

    public enum IndicatorType {
        CIRCLE, CROSSHAIR, DIAMOND, SQUARE, CUSTOM
    }

    public enum HudVariant {
        CLASSIC, MINIMAL, COMPACT
    }

    public enum DistanceUnit {
        BLOCKS, METERS
    }

    // === ACCESSORS ===

    public static float  getMaxLockOnDistance()    { return CLIENT.maxLockOnDistance.get().floatValue(); }
    public static float  getTargetingAngle()       { return CLIENT.targetingAngle.get().floatValue(); }
    public static TargetingMode getTargetingMode() { return CLIENT.targetingMode.get(); }
    public static boolean requireLineOfSight()     { return CLIENT.requireLineOfSight.get(); }
    public static float  getDistancePriorityWeight() { return CLIENT.distancePriorityWeight.get().floatValue(); }
    public static float  getAnglePriorityWeight()  { return CLIENT.anglePriorityWeight.get().floatValue(); }
    public static int    getMaxTargetsToSearch()   { return CLIENT.maxTargetsToSearch.get(); }
    public static int    getUpdateFrequency()      { return CLIENT.updateFrequency.get(); }

    public static float  getRotationSpeed()        { return CLIENT.rotationSpeed.get().floatValue(); }
    public static boolean isSmoothCameraEnabled()  { return CLIENT.enableSmoothCamera.get(); }
    public static boolean autoBreakOnObstruction() { return CLIENT.autoBreakOnObstruction.get(); }

    public static IndicatorType getIndicatorType() { return CLIENT.indicatorType.get(); }
    public static float  getIndicatorSize()        { return CLIENT.indicatorSize.get().floatValue(); }
    public static boolean isPulseEnabled()         { return CLIENT.enablePulse.get(); }
    public static float  getPulseSpeed()           { return CLIENT.pulseSpeed.get().floatValue(); }
    public static float  getPulseAmplitude()       { return CLIENT.pulseAmplitude.get().floatValue(); }
    public static boolean isGlowEnabled()          { return CLIENT.enableGlow.get(); }
    public static float  getGlowIntensity()        { return CLIENT.glowIntensity.get().floatValue(); }
    public static boolean isDynamicColorBasedOnHealth()    { return CLIENT.dynamicColorBasedOnHealth.get(); }
    public static boolean isDynamicColorBasedOnDistance()  { return CLIENT.dynamicColorBasedOnDistance.get(); }
    public static String getCustomIndicatorName()  { return CLIENT.customIndicatorName.get(); }

    public static Color getIndicatorColor() {
        return new Color(CLIENT.indicatorColorRed.get(), CLIENT.indicatorColorGreen.get(),
                CLIENT.indicatorColorBlue.get(), CLIENT.indicatorColorAlpha.get());
    }

    public static Color getOutlineColor() {
        return new Color(CLIENT.outlineColorRed.get(), CLIENT.outlineColorGreen.get(),
                CLIENT.outlineColorBlue.get(), CLIENT.outlineColorAlpha.get());
    }

    public static boolean showTargetName()  { return CLIENT.showTargetName.get(); }
    public static boolean showHealthBar()   { return CLIENT.showHealthBar.get(); }
    public static boolean showDistance()    { return CLIENT.showDistance.get(); }
    public static DistanceUnit getDistanceUnit() { return CLIENT.distanceUnit.get(); }
    public static HudVariant getHudVariant() { return CLIENT.hudVariant.get(); }

    public static boolean canTargetPlayers()     { return CLIENT.targetPlayers.get(); }
    public static boolean canTargetHostileMobs() { return CLIENT.targetHostileMobs.get(); }
    public static boolean canTargetPassiveMobs() { return CLIENT.targetPassiveMobs.get(); }
    public static boolean canTargetBosses()      { return CLIENT.targetBosses.get(); }
    public static float  getMinTargetHealth()    { return CLIENT.minTargetHealth.get().floatValue(); }
    public static float  getMaxTargetHealth()    { return CLIENT.maxTargetHealth.get().floatValue(); }
    public static boolean useWhitelist()         { return CLIENT.useWhitelist.get(); }

    public static List<String> getEntityBlacklist() {
        List<? extends String> raw = CLIENT.entityBlacklist.get();
        return raw == null ? new ArrayList<>() : new ArrayList<>(raw);
    }

    public static List<String> getEntityWhitelist() {
        List<? extends String> raw = CLIENT.entityWhitelist.get();
        return raw == null ? new ArrayList<>() : new ArrayList<>(raw);
    }

    public static boolean areSoundsEnabled()      { return CLIENT.enableSounds.get(); }
    public static float  getSoundVolume()         { return CLIENT.soundVolume.get().floatValue(); }
    public static boolean playLockOnSound()       { return CLIENT.lockOnSound.get(); }
    public static boolean playTargetSwitchSound() { return CLIENT.targetSwitchSound.get(); }
    public static boolean playTargetLostSound()   { return CLIENT.targetLostSound.get(); }

    public static boolean isToggleMode()          { return CLIENT.toggleMode.get(); }
    public static boolean holdToMaintainLock()    { return CLIENT.holdToMaintainLock.get(); }
    public static boolean canCycleThroughTargets(){ return CLIENT.cycleThroughTargets.get(); }
    public static boolean reverseScrollCycling()  { return CLIENT.reverseScrollCycling.get(); }

    public static boolean disableInCreative()     { return CLIENT.disableInCreative.get(); }
    public static boolean disableInSpectator()    { return CLIENT.disableInSpectator.get(); }

    // Kept for backward compat with LockOnSystem references
    public static double getSearchRadius()        { return CLIENT.maxLockOnDistance.get() * 1.2; }
    public static double getMinRotationSpeed()    { return 0.05; }
    public static double getMaxRotationSpeed()    { return 0.8; }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        LockOnMod.LOGGER.debug("Lock-On config loaded: {}", configEvent.getConfig().getFileName());
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        LockOnMod.LOGGER.debug("Lock-On config reloaded");
    }
}