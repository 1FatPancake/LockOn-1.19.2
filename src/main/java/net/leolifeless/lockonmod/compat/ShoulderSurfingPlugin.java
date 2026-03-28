package net.leolifeless.lockonmod.compat;

import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingRegistrar;
import com.github.exopandora.shouldersurfing.api.callback.ICameraCouplingCallback;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * Registers Lock-On Mod's callbacks with Shoulder Surfing Reloaded.
 * Registered in LockOnMod during FMLClientSetupEvent when SS is present.
 */
public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void register(IShoulderSurfingRegistrar registry) {
        // Force camera coupling while lock-on is active.
        // This is the proper fix for the decoupled camera / body rotation bug —
        // no reflection, no config mutation, just a clean API callback.
        registry.registerCameraCouplingCallback(new ICameraCouplingCallback() {
            @Override
            public boolean isForcingCameraCoupling(Minecraft minecraft) {
                return ShoulderSurfingCompat.shouldForceCoupling();
            }
        });

        LOGGER.info("Lock-On Mod: Shoulder Surfing camera coupling callback registered");
    }
}