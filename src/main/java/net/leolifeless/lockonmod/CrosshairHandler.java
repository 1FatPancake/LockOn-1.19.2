package net.leolifeless.lockonmod;

import net.leolifeless.lockonmod.compat.LeawindCompat;
import net.leolifeless.lockonmod.compat.ShoulderSurfingCompat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CrosshairHandler {

    @SubscribeEvent
    public static void onRenderCrosshair(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            if (LockOnSystem.hasTarget()) {
                if (ShoulderSurfingCompat.isActive() ||
                        (LeawindCompat.isLoaded() && LeawindCompat.isInitialized())) {
                    event.setCanceled(true);
                }
            }
        }
    }
}