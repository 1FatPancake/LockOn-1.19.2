# LockOn Mod

A client-side lock-on targeting system for Minecraft 1.20.1 (Forge).  
Lock onto enemies, cycle targets, and keep your camera focused — with full third-person mod support.

---

## Requirements

- Minecraft **1.20.1**
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

The file is split into sections. The most useful options:

### Targeting
| Option | Default | Description |
|--------|---------|-------------|
| `maxLockOnDistance` | `32` | Max range in blocks |
| `targetingAngle` | `45` | Max degrees off-crosshair to search |
| `targetingMode` | `CROSSHAIR_CENTERED` | Priority mode: `CLOSEST`, `MOST_DAMAGED`, `CROSSHAIR_CENTERED`, `THREAT_LEVEL` |
| `requireLineOfSight` | `true` | Only lock onto visible targets |

### Filters
| Option | Default | Description |
|--------|---------|-------------|
| `targetPlayers` | `true` | Allow targeting players |
| `targetHostileMobs` | `true` | Allow targeting hostile mobs |
| `targetPassiveMobs` | `true` | Allow targeting passive mobs |
| `targetBosses` | `true` | Allow targeting bosses |
| `entityBlacklist` | `[villager, cat]` | Entity IDs to never target |
| `useWhitelist` | `false` | If `true`, only targets in `entityWhitelist` are locked |

### Indicator
| Option | Default | Description |
|--------|---------|-------------|
| `indicatorType` | `CIRCLE` | Shape: `CIRCLE`, `CROSSHAIR`, `DIAMOND`, `SQUARE`, `CUSTOM` |
| `indicatorSize` | `0.5` | World-space size of the indicator |
| `enablePulse` | `true` | Pulsing animation |
| `enableGlow` | `true` | Glow halo around the indicator |
| `dynamicColorBasedOnHealth` | `false` | Shifts color red→green with target health |

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
- Use white or light colours — the mod tints the image using your configured indicator colour.

---

## Third-Party Mod Compatibility

The mod automatically detects and integrates with these mods if they are installed:

| Mod | What changes |
|-----|-------------|
| **Shoulder Surfing Reloaded** | Camera rotation is applied to the shoulder-surfing camera; mouse input is suppressed while locked |
| **Leawind's Third Person** | Camera follow mode is activated on lock; rotation targets the correct third-person camera angle |

No extra configuration is needed — compatibility is handled automatically.

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
Lower `maxTargetsToSearch` (default 50) and increase `updateFrequency` (higher = less frequent).

---

*Created by Leolifeless — Minecraft 1.20.1 — Client-Side Only*
