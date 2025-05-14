package net.leolifeless.lockonmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class LockOnKeybinds {
    public static final String KEY_CATEGORY_LOCKON = "key.categories." + LockOnMod.MOD_ID;
    public static final String KEY_LOCKON = "key." + LockOnMod.MOD_ID + ".lockon";
    public static final String KEY_CYCLE_TARGET = "key." + LockOnMod.MOD_ID + ".cycle_target";

    public static KeyMapping lockOnKey;
    public static KeyMapping cycleTargetKey;

    public static void init() {
        lockOnKey = new KeyMapping(
                KEY_LOCKON,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,  // Default to R key
                KEY_CATEGORY_LOCKON
        );

        cycleTargetKey = new KeyMapping(
                KEY_CYCLE_TARGET,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_T,  // Default to T key
                KEY_CATEGORY_LOCKON
        );
    }

    // This method should be called during the ClientSetupEvent
    public static void register(final FMLClientSetupEvent event) {
        // Register the keybindings using the new method in 1.19.2
        event.enqueueWork(() -> {
            net.minecraftforge.client.RegisterKeyMappingsEvent registrationEvent = new net.minecraftforge.client.RegisterKeyMappingsEvent();
            registrationEvent.register(lockOnKey);
            registrationEvent.register(cycleTargetKey);
        });
    }
}