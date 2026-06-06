# LockOn Mod

A client-side lock-on targeting system for Minecraft 1.19.2 (Forge).  
Lock onto enemies, cycle targets, and keep your camera focused — with full third-person mod support.

---

## Requirements

- Minecraft **1.19.2**
- Forge (see `mods.toml` for the exact version range)
- **Client-side only** — does not need to be installed on the server

---

## Installation

1. Download the mod `.jar` file.
2. Place it in your `.minecraft/mods/` folder.
3. Launch the game — a config file is created automatically on first run.

---

## Default Keybinds

All binds can be rebound in **Options → Controls → LockOn Mod**.

### Core

| Key | Action |
|-----|--------|
| `R` | Lock onto nearest valid target |
| `T` | Cycle to next target |
| `Shift + T` | Cycle to previous target |
| `Esc` | Clear current target |

### Targeting Modes

| Key | Mode |
|-----|------|
| `Ctrl + 1` | Closest |
| `Ctrl + 2` | Most Damaged |
| `Ctrl + 3` | Threat Level |

### Quick Filters (toggle on/off mid-game)

| Key | Toggles |
|-----|---------|
| `Alt + P` | Target players |
| `Alt + H` | Target hostile mobs |
| `Alt + A` | Target passive mobs |

### Visual

| Key | Action |
|-----|--------|
| `Ctrl + I` | Toggle indicator visibility |
| `Ctrl + V` | Cycle indicator shape |

### Utility

| Key | Action |
|-----|--------|
| `Ctrl + F12` | Force desync check (multiplayer) |

---

## Configuration

Config file: `.minecraft/config/lockonmod-client.toml`

### Targeting
| Option | Default | Description |
|--------|---------|-------------|
| `maxLockOnDistance` | `50` | Max range in blocks |
| `targetingAngle` | `45` | Max degrees off-crosshair to search |
| `targetingMode` | `CLOSEST` | Priority mode: `CLOSEST`, `MOST_DAMAGED`, `CROSSHAIR_CENTERED`, `THREAT_LEVEL`, `SMART` |
| `requireLineOfSight` | `true` | Only lock onto visible targets |
| `smartTargeting` | `true` | Combine distance, angle, and health weights for selection |

### Third-Person Compatibility
| Option | Default | Description |
|--------|---------|-------------|
| `enableThirdPersonEnhancements` | `true` | Enable all third-person adjustments |
| `thirdPersonRangeMultiplier` | `1.2` | Range boost in third-person (1.2 = +20%) |
| `thirdPersonAngleMultiplier` | `1.3` | Angle boost in third-person |
| `thirdPersonRotationSpeedMultiplier` | `0.85` | Slightly slower rotation for third-person feel |
| `adjustForCameraOffset` | `true` | Correct targeting for camera position offset |

### Filters
| Option | Default | Description |
|--------|---------|-------------|
| `canTargetPlayers` | `true` | Allow targeting players |
| `canTargetHostileMobs` | `true` | Allow targeting hostile mobs |
| `canTargetPassiveMobs` | `false` | Allow targeting passive mobs |
| `canTargetBosses` | `true` | Allow targeting bosses |
| `entityBlacklist` | `[villager, cat]` | Entity IDs to never target |
| `useWhitelist` | `false` | If `true`, only targets in `entityWhitelist` are locked |

### Indicator
| Option | Default | Description |
|--------|---------|-------------|
| `indicatorType` | `CIRCLE` | Shape: `CIRCLE`, `CROSSHAIR`, `DIAMOND`, `SQUARE`, `CUSTOM` |
| `indicatorSize` | `1.0` | World-space size of the indicator |
| `pulseEnabled` | `true` | Pulsing animation |
| `glowEnabled` | `true` | Glow halo around the indicator |
| `customIndicatorName` | `default` | File name (without `.png`) to use when type is `CUSTOM` |
| `dynamicHealthColorEnabled` | `true` | Shifts color based on target health |
| `indicatorColorHex` | `#FF6600` | Indicator fill colour (hex) |
| `outlineColorHex` | `#FFFFFF` | Indicator outline colour (hex) |

### Keybind Behavior
| Option | Default | Description |
|--------|---------|-------------|
| `toggleMode` | `true` | One press to lock, one press to unlock |
| `holdToMaintainLock` | `false` | Hold the key to keep the lock active |

---

## Custom Indicator Textures

You can use your own PNG images as the lock-on indicator.

1. Open `.minecraft/config/lockonmod/custom_indicators/`  
   *(the folder is created automatically on first launch)*
2. Drop any `.png` file into that folder.
3. In-game, set `indicatorType = CUSTOM` in the config and set `customIndicatorName` to your file's name (without `.png`).
4. Press **F3+T** to reload — no restart needed.

**Tips:**
- 64×64 pixels, square, with a transparent background works best.
- Use white or light colours — the mod tints the image using your configured `indicatorColorHex`.

---

## Third-Party Mod Compatibility

The mod automatically detects and integrates with these mods if they are installed:

| Mod | What changes |
|-----|-------------|
| **Shoulder Surfing Reloaded** | Camera rotation is applied to the shoulder-surfing camera; mouse input is suppressed while locked |
| **Leawind's Third Person** (v2.0+) | Camera follow mode is activated on lock; rotation targets the correct third-person camera angle; range and angle scale automatically |

No extra configuration is needed — compatibility is handled automatically. The `[thirdPersonCompat]` config section lets you fine-tune the multipliers if the defaults don't feel right.

---

## Troubleshooting

**Target is locked but the camera doesn't rotate**  
Check that you don't have a conflicting mod overriding camera control. The Shoulder Surfing and Leawind integrations handle their own cameras automatically.

**Custom indicator not showing**  
- Confirm `indicatorType` is set to `CUSTOM` in the config.
- Confirm `customIndicatorName` matches the file name (no extension).
- Check `.minecraft/logs/latest.log` for lines containing `lockonmod` — loading errors are logged there.

**Target immediately drops on a multiplayer server**  
Press `Ctrl + F12` to run a manual desync check. The mod also auto-checks every 3 seconds.

**Poor performance**  
Lower `maxTargetsToSearch` (default 50) and increase `updateFrequency` (higher number = less frequent updates).

---

*Created by Leolifeless — Minecraft 1.19.2 — Client-Side Only*
