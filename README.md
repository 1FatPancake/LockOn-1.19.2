# TargetLock Mod

A comprehensive lock-on targeting system for Minecraft 1.19.2 that provides smooth camera control, intelligent target selection, and customizable visual indicators.

## 🎯 Features

### Core Functionality
- **Advanced Targeting System**: Lock onto entities with intelligent prioritization
- **Smooth Camera Control**: Fluid camera movement with predictive targeting
- **Multiple Targeting Modes**: Closest, Most Damaged, Cross-hair Centered, Threat Level
- **Target Cycling**: Seamlessly switch between multiple targets
- **HUD Information Display**: Clean interface showing target name, distance, and health

### Visual Indicators
- **Multiple Indicator Types**: Circle, Cross-hair, Diamond, Square, Custom animated star
- **Dynamic Effects**: Pulsing animations, glow effects, and rotating indicators
- **Color Customization**: Configurable colors with dynamic health/distance-based coloring
- **Professional HUD**: Top-right corner display with health bars and target information

### Smart Filtering
- **Entity Type Filters**: Target players, hostile mobs, passive mobs, bosses, etc.
- **Health-Based Filtering**: Set minimum/maximum health thresholds
- **Distance Controls**: Configurable lock-on range and search radius
- **Line of Sight**: Optional requirement with glass penetration support
- **Blacklist/Whitelist**: Exclude or exclusively target specific entity types

### Configuration Options
- **Extensive Customization**: Over 50 configuration options
- **Keybinding Modes**: Hold-to-maintain, toggle, or hybrid modes
- **Performance Settings**: Adjustable update frequency and search limits
- **Audio Feedback**: Configurable sound effects for targeting events
- **Game Mode Support**: Optional disable in creative/spectator modes

## 🎮 Default Controls

| Key | Action |
|-----|--------|
| `R` | Lock-On Target |
| `T` | Cycle Target |
| `Shift + T` | Cycle Target (Reverse) |
| `ESC` | Clear Target |
| `Ctrl + 1` | Target Closest |
| `Ctrl + 2` | Target Most Damaged |
| `Ctrl + 3` | Target by Threat Level |
| `Alt + P` | Toggle Player Targeting |
| `Alt + H` | Toggle Hostile Mob Targeting |
| `Alt + A` | Toggle Passive Mob Targeting |
| `Ctrl + I` | Toggle Indicator Visibility |
| `Ctrl + V` | Cycle Indicator Type |


🐛 Troubleshooting
Common Issues
Mod not working:

Verify Minecraft 1.19.2 with Forge 43.5.0+
Check mod is in the correct mods folder
Ensure no conflicting mods

Poor performance:

Reduce updateFrequency in the config
Lower maxTargetsToSearch
Disable visual effects if needed

Targeting is not working:

Check entity filters in the configuration
Verify line of sight settings
Ensure you're within max distance
