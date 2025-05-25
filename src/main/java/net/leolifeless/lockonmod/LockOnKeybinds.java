package net.leolifeless.lockonmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * Enhanced Keybinding handler for the Lock-On Mod
 * Manages all keybindings and their behavior based on configuration settings
 */
@OnlyIn(Dist.CLIENT)
public class LockOnKeybinds {
    // Key categories
    public static final String KEY_CATEGORY_LOCKON = "key.categories." + LockOnMod.MOD_ID;

    // Key translation keys
    public static final String KEY_LOCKON = "key." + LockOnMod.MOD_ID + ".lockon";
    public static final String KEY_CYCLE_TARGET = "key." + LockOnMod.MOD_ID + ".cycle_target";
    public static final String KEY_CYCLE_BACKWARD = "key." + LockOnMod.MOD_ID + ".cycle_backward";
    public static final String KEY_TOGGLE_MODE = "key." + LockOnMod.MOD_ID + ".toggle_mode";
    public static final String KEY_NEAREST_TARGET = "key." + LockOnMod.MOD_ID + ".nearest_target";
    public static final String KEY_HOSTILE_ONLY = "key." + LockOnMod.MOD_ID + ".hostile_only";

    // KeyMapping instances
    public static KeyMapping lockOnKey;
    public static KeyMapping cycleTargetKey;
    public static KeyMapping cycleBackwardKey;
    public static KeyMapping toggleModeKey;
    public static KeyMapping nearestTargetKey;
    public static KeyMapping hostileOnlyKey;

    // Timing and state management
    private static long lastCycleTime = 0;
    private static long lastLockOnPress = 0;
    private static boolean wasKeyPressed = false;
    private static boolean quickCycleActive = false;

    /**
     * Initialize all keybindings with their default values
     */
    public static void init() {
        // Primary lock-on key (R by default)
        lockOnKey = new KeyMapping(
                KEY_LOCKON,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KEY_CATEGORY_LOCKON
        );

        // Cycle to next target (T by default)
        cycleTargetKey = new KeyMapping(
                KEY_CYCLE_TARGET,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                KEY_CATEGORY_LOCKON
        );

        // Cycle to previous target (Shift+T by default)
        cycleBackwardKey = new KeyMapping(
                KEY_CYCLE_BACKWARD,
                KeyConflictContext.IN_GAME,
                KeyModifier.SHIFT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                KEY_CATEGORY_LOCKON
        );

        // Toggle between toggle/hold mode (F by default)
        toggleModeKey = new KeyMapping(
                KEY_TOGGLE_MODE,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                KEY_CATEGORY_LOCKON
        );

        // Lock onto nearest target regardless of look direction (G by default)
        nearestTargetKey = new KeyMapping(
                KEY_NEAREST_TARGET,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KEY_CATEGORY_LOCKON
        );

        // Lock onto nearest hostile entity (H by default)
        hostileOnlyKey = new KeyMapping(
                KEY_HOSTILE_ONLY,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY_LOCKON
        );
    }

    /**
     * Register all keybindings with Forge
     */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(lockOnKey);
        event.register(cycleTargetKey);
        event.register(cycleBackwardKey);
        event.register(toggleModeKey);
        event.register(nearestTargetKey);
        event.register(hostileOnlyKey);
    }

    /**
     * Check if the lock-on key should trigger based on configuration
     * Handles both toggle and hold modes, as well as quick cycling
     */
    public static boolean shouldLockOnActivate() {
        long currentTime = System.currentTimeMillis();
        boolean isPressed = lockOnKey.isDown();
        boolean wasJustPressed = lockOnKey.consumeClick();

        // Handle hold-to-maintain mode (overrides toggle mode)
        if (LockOnConfig.requireHoldToMaintain()) {
            return isPressed;
        }

        // Handle toggle mode
        if (LockOnConfig.isToggleMode()) {
            if (wasJustPressed) {
                // Check for quick cycle functionality
                if (LockOnConfig.isQuickCycleEnabled() &&
                        (currentTime - lastLockOnPress) < LockOnConfig.getCycleCooldownMs()) {
                    quickCycleActive = true;
                    return false; // Don't toggle, just cycle
                }

                lastLockOnPress = currentTime;
                return true; // Toggle lock-on state
            }
            return false;
        }

        // Default behavior - press to lock, release to unlock
        return wasJustPressed;
    }

    /**
     * Check if quick cycle should be triggered
     */
    public static boolean shouldQuickCycle() {
        if (!LockOnConfig.isQuickCycleEnabled()) return false;

        if (quickCycleActive && lockOnKey.consumeClick()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCycleTime >= LockOnConfig.getCycleCooldownMs()) {
                lastCycleTime = currentTime;
                return true;
            }
        }

        // Reset quick cycle if too much time has passed
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLockOnPress > LockOnConfig.getCycleCooldownMs() * 2) {
            quickCycleActive = false;
        }

        return false;
    }

    /**
     * Check if cycle target key was pressed (with cooldown)
     */
    public static boolean shouldCycleTarget() {
        if (!cycleTargetKey.consumeClick()) return false;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCycleTime >= LockOnConfig.getCycleCooldownMs()) {
            lastCycleTime = currentTime;
            return true;
        }
        return false;
    }

    /**
     * Check if cycle backward key was pressed (with cooldown)
     */
    public static boolean shouldCycleBackward() {
        if (!cycleBackwardKey.consumeClick()) return false;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCycleTime >= LockOnConfig.getCycleCooldownMs()) {
            lastCycleTime = currentTime;
            return true;
        }
        return false;
    }

    /**
     * Check if toggle mode key was pressed
     */
    public static boolean shouldToggleMode() {
        return toggleModeKey.consumeClick();
    }

    /**
     * Check if nearest target key was pressed
     */
    public static boolean shouldTargetNearest() {
        return nearestTargetKey.consumeClick();
    }

    /**
     * Check if hostile-only key was pressed
     */
    public static boolean shouldTargetHostileOnly() {
        return hostileOnlyKey.consumeClick();
    }

    /**
     * Reset all timing-related state (useful when lock-on is disabled)
     */
    public static void resetState() {
        lastCycleTime = 0;
        lastLockOnPress = 0;
        wasKeyPressed = false;
        quickCycleActive = false;
    }

    /**
     * Check if any lock-on related key is currently being held
     * Useful for determining if player is actively using the system
     */
    public static boolean isAnyKeyHeld() {
        return lockOnKey.isDown() ||
                cycleTargetKey.isDown() ||
                cycleBackwardKey.isDown() ||
                toggleModeKey.isDown() ||
                nearestTargetKey.isDown() ||
                hostileOnlyKey.isDown();
    }

    /**
     * Get the display name for a keybinding mode
     */
    public static String getKeybindingModeDescription() {
        if (LockOnConfig.requireHoldToMaintain()) {
            return "Hold to Lock-On";
        } else if (LockOnConfig.isToggleMode()) {
            return LockOnConfig.isQuickCycleEnabled() ?
                    "Toggle Mode (Quick Cycle Enabled)" :
                    "Toggle Mode";
        } else {
            return "Press to Lock-On";
        }
    }

    /**
     * Check if the system should maintain lock when key is released
     * Used by the main system to determine behavior
     */
    public static boolean shouldMaintainLock() {
        // If require hold is enabled, only maintain while key is held
        if (LockOnConfig.requireHoldToMaintain()) {
            return lockOnKey.isDown();
        }

        // If toggle mode, maintain until toggled off
        if (LockOnConfig.isToggleMode()) {
            return true; // Let the main system handle toggle logic
        }

        // Default: maintain for a short time after key release for responsiveness
        return System.currentTimeMillis() - lastLockOnPress < 100; // 100ms grace period
    }

    /**
     * Update internal state - should be called each tick
     */
    public static void tick() {
        // Update key state tracking
        boolean currentlyPressed = lockOnKey.isDown();

        // Handle state transitions
        if (currentlyPressed && !wasKeyPressed) {
            // Key just pressed
            onKeyPressed();
        } else if (!currentlyPressed && wasKeyPressed) {
            // Key just released
            onKeyReleased();
        }

        wasKeyPressed = currentlyPressed;

        // Clean up old state
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLockOnPress > 5000) { // 5 second timeout
            quickCycleActive = false;
        }
    }

    /**
     * Called when lock-on key is first pressed
     */
    private static void onKeyPressed() {
        lastLockOnPress = System.currentTimeMillis();
    }

    /**
     * Called when lock-on key is released
     */
    private static void onKeyReleased() {
        // Handle any release-specific logic here
        // Currently just used for state tracking
    }

    /**
     * Get the current key state for debugging/display purposes
     */
    public static String getKeyStateDebug() {
        return String.format(
                "LockOn: %s, Cycle: %s, Mode: %s, QuickCycle: %s, LastPress: %dms ago",
                lockOnKey.isDown() ? "DOWN" : "UP",
                cycleTargetKey.isDown() ? "DOWN" : "UP",
                getKeybindingModeDescription(),
                quickCycleActive ? "ACTIVE" : "INACTIVE",
                System.currentTimeMillis() - lastLockOnPress
        );
    }
}