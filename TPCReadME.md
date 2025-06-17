# Target Lock-On Mod - Third Person Compatibility

## Overview

The Target Lock-On mod now includes comprehensive compatibility with third-person camera mods, specifically enhanced for **Leawind's Third Person** mod. This integration provides a seamless targeting experience whether you're in first-person or third-person perspective.

## Supported Third Person Mods

### Primary Support
- **Leawind's Third Person** (v2.0+) - Full compatibility with all features
    - Dynamic camera offset detection
    - Enhanced indicator rendering
    - Perspective-aware targeting adjustments

### Basic Support
- **Shoulder Surfing Reloaded** - Basic compatibility
- **Better Third Person** - Basic compatibility

## Features

### Automatic Detection
The mod automatically detects when you're using a supported third-person mod and enables enhanced features:

- ✅ **Dynamic Range Adjustment** - Targeting range automatically adjusts based on camera distance
- ✅ **Enhanced Angle Detection** - Wider targeting angles for easier targeting in third-person
- ✅ **Smooth Camera Integration** - Improved camera rotation that works with third-person smoothing
- ✅ **Optimized Indicators** - Indicators are sized and positioned optimally for third-person view
- ✅ **Camera Offset Compensation** - Targeting calculations account for camera position

### Configuration Options

All third-person features can be customized in the mod's configuration:

```toml
[thirdPersonCompat]
    # Enable enhanced features when third-person mods are detected
    enableThirdPersonEnhancements = true
    
    # Multiplier for targeting range in third person mode (1.2 = 20% increase)
    thirdPersonRangeMultiplier = 1.2
    
    # Multiplier for targeting angle in third person mode (1.3 = 30% increase)
    thirdPersonAngleMultiplier = 1.3
    
    # Additional smoothing factor for third person camera
    thirdPersonSmoothingFactor = 1.15
    
    # Rotation speed multiplier for third person mode (0.85 = 15% slower)
    thirdPersonRotationSpeedMultiplier = 0.85
    
    # Indicator size multiplier for third person mode
    thirdPersonIndicatorSizeMultiplier = 1.0
    
    # Adjust targeting calculations for third person camera offset
    adjustForCameraOffset = true
    
    # Use enhanced smoothing algorithms in third person
    enhancedThirdPersonSmoothing = true
    
    # Automatically detect and adjust for third person perspective