package net.leolifeless.lockonmod;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(LockOnMod.MOD_ID)
public class LockOnMod {
    public static final String MOD_ID = "lockonmod";
    public static final Logger LOGGER = LogManager.getLogger();

    public LockOnMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        LockOnKeybinds.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LockOnConfig.CLIENT_SPEC);

        // CRITICAL: Register the LockOnSystem for FORGE events (input handling)
        MinecraftForge.EVENT_BUS.register(LockOnSystem.class);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Lock-On Mod initializing...");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Register all keybindings
            for (net.minecraft.client.settings.KeyBinding keyBinding : LockOnKeybinds.getAllKeyBindings()) {
                ClientRegistry.registerKeyBinding(keyBinding);
            }
            LOGGER.info("Registered {} keybindings", LockOnKeybinds.getAllKeyBindings().length);
        });
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // Server starting logic
    }

    public static ResourceLocation location(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                CustomIndicatorManager.initialize();
                LOGGER.info("Custom Indicator Manager initialized");
            });
            LOGGER.info("Lock-On Mod client setup complete");
        }
    }
}