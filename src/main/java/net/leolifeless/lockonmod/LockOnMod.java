package net.leolifeless.lockonmod;

import com.mojang.logging.LogUtils;
import net.leolifeless.lockonmod.compat.ThirdPersonCompatibility;
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

@Mod(LockOnMod.MOD_ID)
public class LockOnMod {
    public static final String MOD_ID = "lockonmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LockOnMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the key mapping registration method
        modEventBus.addListener(this::onRegisterKeyMappings);
        // Initialize keybindings
        LockOnKeybinds.init();

        // Register configuration
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LockOnConfig.CLIENT_SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event){
        LOGGER.info("Lock-On Mod initializing...");
    }

    private void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        LockOnKeybinds.register(event);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
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
                ThirdPersonCompatibility.initialize();
                CustomIndicatorManager.initialize();
                LOGGER.info("Lock-On Mod client setup complete");
                LOGGER.info("Third Person Compatibility: {}", ThirdPersonCompatibility.getCompatibilityStatus());
            });
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEventHandler {
        @SubscribeEvent
        public static void onResourceReload(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
            CustomIndicatorManager.refresh();
            LOGGER.debug("Refreshed custom indicators after resource pack reload");
        }
    }
}