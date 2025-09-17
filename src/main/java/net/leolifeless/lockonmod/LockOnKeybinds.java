package net.leolifeless.lockonmod;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
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

    // KeyBinding instances (1.16.5 style)
    public static KeyBinding lockOnKey;
    public static KeyBinding cycleTargetKey;
    public static KeyBinding cycleTargetReverseKey;
    public static KeyBinding clearTargetKey;

    public static KeyBinding targetClosestKey;
    public static KeyBinding targetMostDamagedKey;
    public static KeyBinding targetThreatKey;

    public static KeyBinding togglePlayersKey;
    public static KeyBinding toggleHostilesKey;
    public static KeyBinding togglePassivesKey;

    public static KeyBinding toggleIndicatorKey;
    public static KeyBinding cycleIndicatorTypeKey;

    public static void init() {
        // Main controls (1.16.5 KeyBinding constructor)
        lockOnKey = new KeyBinding(
                KEY_LOCKON,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KEY_CATEGORY_LOCKON
        );

        cycleTargetKey = new KeyBinding(
                KEY_CYCLE_TARGET,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                KEY_CATEGORY_LOCKON
        );

        cycleTargetReverseKey = new KeyBinding(
                KEY_CYCLE_TARGET_REVERSE,
                KeyConflictContext.IN_GAME,
                KeyModifier.SHIFT,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                KEY_CATEGORY_LOCKON
        );

        clearTargetKey = new KeyBinding(
                KEY_CLEAR_TARGET,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_ESCAPE,
                KEY_CATEGORY_LOCKON
        );

        // Targeting mode shortcuts
        targetClosestKey = new KeyBinding(
                KEY_TARGET_CLOSEST,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_1,
                KEY_CATEGORY_LOCKON
        );

        targetMostDamagedKey = new KeyBinding(
                KEY_TARGET_MOST_DAMAGED,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_2,
                KEY_CATEGORY_LOCKON
        );

        targetThreatKey = new KeyBinding(
                KEY_TARGET_THREAT,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_3,
                KEY_CATEGORY_LOCKON
        );

        // Quick filter toggles
        togglePlayersKey = new KeyBinding(
                KEY_TOGGLE_PLAYERS,
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KEY_CATEGORY_LOCKON
        );

        toggleHostilesKey = new KeyBinding(
                KEY_TOGGLE_HOSTILES,
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY_LOCKON
        );

        togglePassivesKey = new KeyBinding(
                KEY_TOGGLE_PASSIVES,
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_A,
                KEY_CATEGORY_LOCKON
        );

        // Visual controls
        toggleIndicatorKey = new KeyBinding(
                KEY_TOGGLE_INDICATOR,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                KEY_CATEGORY_LOCKON
        );

        cycleIndicatorTypeKey = new KeyBinding(
                KEY_CYCLE_INDICATOR_TYPE,
                KeyConflictContext.IN_GAME,
                KeyModifier.CONTROL,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KEY_CATEGORY_LOCKON
        );
    }

    // 1.16.5 uses different key registration - this would be called from a KeyBindingRegistry event
    public static KeyBinding[] getAllKeyBindings() {
        return new KeyBinding[]{
                lockOnKey,
                cycleTargetKey,
                cycleTargetReverseKey,
                clearTargetKey,
                targetClosestKey,
                targetMostDamagedKey,
                targetThreatKey,
                togglePlayersKey,
                toggleHostilesKey,
                togglePassivesKey,
                toggleIndicatorKey,
                cycleIndicatorTypeKey
        };
    }
}