package net.leolifeless.lockonmod;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LockOnMod.MOD_ID)
public class LockOnMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "lockonmod";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public LockOnMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the key mapping registration method
        modEventBus.addListener(this::onRegisterKeyMappings);  // Add this line
        // Initialize keybindings
        LockOnKeybinds.init();

        // Register configuration
        context.registerConfig(ModConfig.Type.CLIENT, LockOnConfig.CLIENT_SPEC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

    }

    private void commonSetup(final FMLCommonSetupEvent event){
        LOGGER.info("Lock-On Mod initializing...");
    }

    // This is the correct way to register key mappings in 1.19.2
    private void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        LockOnKeybinds.register(event);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Server starting logic
    }

    // Helper method to create resource locations for this mod
    public static ResourceLocation location(String path) {
        return new ResourceLocation(MOD_ID, path); // Fixed constructor usage
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Initialize custom indicator manager on client setup
            event.enqueueWork(() -> {
                CustomIndicatorManager.initialize();
                LOGGER.info("Custom Indicator Manager initialized");
            });

            LOGGER.info("Lock-On Mod client setup complete");
        }
    }

    // Event handler for resource pack reloads
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEventHandler {

        @SubscribeEvent
        public static void onResourceReload(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
            // Refresh custom indicators when resource packs change
            CustomIndicatorManager.refresh();
            LOGGER.debug("Refreshed custom indicators after resource pack reload");
        }
    }
}