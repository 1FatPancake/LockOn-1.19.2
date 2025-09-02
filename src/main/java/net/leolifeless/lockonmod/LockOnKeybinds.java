package net.leolifeless.lockonmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class LockOnKeybinds {
    public static final String KEY_CATEGORY_LOCKON = "key.categories." + LockOnMod.MOD_ID;

    // Main keybinds
    public static final String KEY_LOCKON = "key." + LockOnMod.MOD_ID + ".lockon";
    public static final String KEY_CYCLE_TARGET = "key." + LockOnMod.MOD_ID + ".cycle_target";
    public static final String KEY_CYCLE_TARGET_REVERSE = "key." + LockOnMod.MOD_ID + ".cycle_target_reverse";
    public static final String KEY_CLEAR_TARGET = "key." + LockOnMod.MOD_ID + ".clear_target";

    // Targeting mode keybinds
    public static final String KEY_TARGET_CLOSEST = "key." + LockOnMod.MOD_ID + ".target_closest";
    public static final String KEY_TARGET_MOST_DAMAGED = "key." + LockOnMod.MOD_ID + ".target_most_damaged";
    public static final String KEY_TARGET_THREAT = "key." + LockOnMod.MOD_ID + ".target_threat";

    // Quick filter toggles
    public static final String KEY_TOGGLE_PLAYERS = "key." + LockOnMod.MOD_ID + ".toggle_players";
    public static final String KEY_TOGGLE_HOSTILES = "key." + LockOnMod.MOD_ID + ".toggle_hostiles";
    public static final String KEY_TOGGLE_PASSIVES = "key." + LockOnMod.MOD_ID + ".toggle_passives";

    // Visual toggles
    public static final String KEY_TOGGLE_INDICATOR = "key." + LockOnMod.MOD_ID + ".toggle_indicator";
    public static final String KEY_CYCLE_INDICATOR_TYPE = "key." + LockOnMod.MOD_ID + ".cycle_indicator_type";

    // Force Sync Check
    public static final String KEY_FORCE_SYNC_CHECK = "key." + LockOnMod.MOD_ID + ".force_sync_check";

    // KeyMapping instances
    public static KeyMapping lockOnKey;
    public static KeyMapping cycleTargetKey;
    public static KeyMapping cycleTargetReverseKey;
    public static KeyMapping clearTargetKey;

    public static KeyMapping targetClosestKey;
    public static KeyMapping targetMostDamagedKey;
    public static KeyMapping targetThreatKey;

    public static KeyMapping togglePlayersKey;
    public static KeyMapping toggleHostilesKey;
    public static KeyMapping togglePassivesKey;

    public static KeyMapping toggleIndicatorKey;
    public static KeyMapping cycleIndicatorTypeKey;

    public static KeyMapping forceSyncCheckKey;

    public static void init() {
        // Main controls
        lockOnKey = new KeyMapping(
                KEY_LOCKON,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KEY_CATEGORY_LOCKON
        );

        cycleTargetKey = new KeyMapping(
                KEY_CYCLE_TARGET,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                KEY_CATEGORY_LOCKON
        );

        cycleTargetReverseKey = new KeyMapping(
                KEY_CYCLE_TARGET_REVERSE,
                KeyConflictContext.IN_GAME,
                KeyModifier.SHIFT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                KEY_CATEGORY_LOCKON
        );

        clearTargetKey = new KeyMapping(
                KEY_CLEAR_TARGET,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_ESCAPE,
                KEY_CATEGORY_LOCKON
        );

        // Targeting mode shortcuts
        targetClosestKey = new KeyMapping(
                KEY_TARGET_CLOSEST,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_1,
                KEY_CATEGORY_LOCKON
        );

        targetMostDamagedKey = new KeyMapping(
                KEY_TARGET_MOST_DAMAGED,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_2,
                KEY_CATEGORY_LOCKON
        );

        targetThreatKey = new KeyMapping(
                KEY_TARGET_THREAT,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_3,
                KEY_CATEGORY_LOCKON
        );

        // Quick filter toggles
        togglePlayersKey = new KeyMapping(
                KEY_TOGGLE_PLAYERS,
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KEY_CATEGORY_LOCKON
        );

        toggleHostilesKey = new KeyMapping(
                KEY_TOGGLE_HOSTILES,
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY_LOCKON
        );

        togglePassivesKey = new KeyMapping(
                KEY_TOGGLE_PASSIVES,
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_A,
                KEY_CATEGORY_LOCKON
        );

        // Visual controls
        toggleIndicatorKey = new KeyMapping(
                KEY_TOGGLE_INDICATOR,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                KEY_CATEGORY_LOCKON
        );

        cycleIndicatorTypeKey = new KeyMapping(
                KEY_CYCLE_INDICATOR_TYPE,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KEY_CATEGORY_LOCKON
        );

        forceSyncCheckKey = new KeyMapping(
                KEY_FORCE_SYNC_CHECK,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F12,
                KEY_CATEGORY_LOCKON
        );
    }

    public static void register(RegisterKeyMappingsEvent event) {
        // Register main controls
        event.register(lockOnKey);
        event.register(cycleTargetKey);
        event.register(cycleTargetReverseKey);
        event.register(clearTargetKey);

        // Register targeting mode shortcuts
        event.register(targetClosestKey);
        event.register(targetMostDamagedKey);
        event.register(targetThreatKey);

        // Register filter toggles
        event.register(togglePlayersKey);
        event.register(toggleHostilesKey);
        event.register(togglePassivesKey);

        // Register visual controls
        event.register(toggleIndicatorKey);
        event.register(cycleIndicatorTypeKey);

        // Register force check
        event.register(forceSyncCheckKey);
    }
}