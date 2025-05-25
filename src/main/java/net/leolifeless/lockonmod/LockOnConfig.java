package net.leolifeless.lockonmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;

/**
 * Configuration handler for the Lock-On Mod
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

        // Camera Settings
        public final ForgeConfigSpec.DoubleValue rotationSpeed;
        public final ForgeConfigSpec.DoubleValue minRotationSpeed;
        public final ForgeConfigSpec.DoubleValue distanceWeight;
        public final ForgeConfigSpec.BooleanValue enableSmoothCamera;

        // Visual Settings
        public final ForgeConfigSpec.DoubleValue indicatorSize;
        public final ForgeConfigSpec.DoubleValue pulseSpeed;
        public final ForgeConfigSpec.DoubleValue pulseAmplitude;
        public final ForgeConfigSpec.IntValue indicatorColorRed;
        public final ForgeConfigSpec.IntValue indicatorColorGreen;
        public final ForgeConfigSpec.IntValue indicatorColorBlue;
        public final ForgeConfigSpec.IntValue indicatorColorAlpha;
        public final ForgeConfigSpec.IntValue outlineColorRed;
        public final ForgeConfigSpec.IntValue outlineColorGreen;
        public final ForgeConfigSpec.IntValue outlineColorBlue;
        public final ForgeConfigSpec.IntValue outlineColorAlpha;

        // Target Filter Settings
        public final ForgeConfigSpec.BooleanValue targetPlayers;
        public final ForgeConfigSpec.BooleanValue targetHostileMobs;
        public final ForgeConfigSpec.BooleanValue targetPassiveMobs;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Lock-On Mod Client Configuration")
                    .push("general");

            // Targeting Settings Section
            builder.comment("Targeting Settings").push("targeting");

            maxLockOnDistance = builder
                    .comment("Maximum distance to lock onto entities")
                    .defineInRange("maxLockOnDistance", 32.0, 5.0, 100.0);

            searchRadius = builder
                    .comment("Radius around player to search for entities")
                    .defineInRange("searchRadius", 10.0, 2.0, 50.0);

            builder.pop();

            // Camera Settings Section
            builder.comment("Camera Settings").push("camera");

            rotationSpeed = builder
                    .comment("Camera rotation speed (lower value = smoother but slower camera)")
                    .defineInRange("rotationSpeed", 0.25, 0.01, 1.0);

            minRotationSpeed = builder
                    .comment("Minimum rotation speed")
                    .defineInRange("minRotationSpeed", 0.3, 0.01, 1.0);

            distanceWeight = builder
                    .comment("How much distance affects rotation speed")
                    .defineInRange("distanceWeight", 0.5, 0.0, 1.0);

            enableSmoothCamera = builder
                    .comment("Enable smooth camera interpolation")
                    .define("enableSmoothCamera", true);

            builder.pop();

            // Visual Settings Section
            builder.comment("Visual Settings").push("visual");

            indicatorSize = builder
                    .comment("Size of the lock-on indicator")
                    .defineInRange("indicatorSize", 0.5, 0.1, 2.0);

            pulseSpeed = builder
                    .comment("Speed of the pulse animation")
                    .defineInRange("pulseSpeed", 1.5, 0.1, 5.0);

            pulseAmplitude = builder
                    .comment("Size variation in pulse")
                    .defineInRange("pulseAmplitude", 0.15, 0.0, 1.0);

            // Indicator Color
            builder.comment("Indicator Color").push("indicatorColor");

            indicatorColorRed = builder
                    .comment("Red component (0-255)")
                    .defineInRange("red", 255, 0, 255);

            indicatorColorGreen = builder
                    .comment("Green component (0-255)")
                    .defineInRange("green", 50, 0, 255);

            indicatorColorBlue = builder
                    .comment("Blue component (0-255)")
                    .defineInRange("blue", 50, 0, 255);

            indicatorColorAlpha = builder
                    .comment("Alpha component (0-255)")
                    .defineInRange("alpha", 180, 0, 255);

            builder.pop();

            // Outline Color
            builder.comment("Outline Color").push("outlineColor");

            outlineColorRed = builder
                    .comment("Red component (0-255)")
                    .defineInRange("red", 255, 0, 255);

            outlineColorGreen = builder
                    .comment("Green component (0-255)")
                    .defineInRange("green", 255, 0, 255);

            outlineColorBlue = builder
                    .comment("Blue component (0-255)")
                    .defineInRange("blue", 255, 0, 255);

            outlineColorAlpha = builder
                    .comment("Alpha component (0-255)")
                    .defineInRange("alpha", 220, 0, 255);

            builder.pop(); // End outline color
            builder.pop(); // End visual settings

            // Target Filter Settings
            builder.comment("Target Filter Settings").push("filters");

            targetPlayers = builder
                    .comment("Allow targeting players")
                    .define("targetPlayers", true);

            targetHostileMobs = builder
                    .comment("Allow targeting hostile mobs")
                    .define("targetHostileMobs", true);

            targetPassiveMobs = builder
                    .comment("Allow targeting passive mobs")
                    .define("targetPassiveMobs", true);

            builder.pop(); // End target filters
            builder.pop(); // End general
        }
    }

    /**
     * Helper methods to easily access config values
     */
    public static float getMaxLockOnDistance() {
        return CLIENT.maxLockOnDistance.get().floatValue();
    }

    public static float getSearchRadius() {
        return CLIENT.searchRadius.get().floatValue();
    }

    public static float getRotationSpeed() {
        return CLIENT.rotationSpeed.get().floatValue();
    }

    public static float getMinRotationSpeed() {
        return CLIENT.minRotationSpeed.get().floatValue();
    }

    public static float getDistanceWeight() {
        return CLIENT.distanceWeight.get().floatValue();
    }

    public static boolean isSmoothCameraEnabled() {
        return CLIENT.enableSmoothCamera.get();
    }

    public static float getIndicatorSize() {
        return CLIENT.indicatorSize.get().floatValue();
    }

    public static float getPulseSpeed() {
        return CLIENT.pulseSpeed.get().floatValue();
    }

    public static float getPulseAmplitude() {
        return CLIENT.pulseAmplitude.get().floatValue();
    }

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

    public static boolean canTargetPlayers() {
        return CLIENT.targetPlayers.get();
    }

    public static boolean canTargetHostileMobs() {
        return CLIENT.targetHostileMobs.get();
    }

    public static boolean canTargetPassiveMobs() {
        return CLIENT.targetPassiveMobs.get();
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