# Changelog

All notable changes to the NoStructureOverlap mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - 2026-10-18

### Added
- Bounding-box overlap engine using vanilla `StructureStart.getBoundingBox()` (MiniHUD-equivalent)
- `ChunkGenerator` mixin to publish dimension / `StructureManager` context during `createStructures`
- Safe loaded-chunk scan via `getChunkNow` (no worldgen status upgrades)
- Dimension unload and server-stop tracker cleanup

### Changed
- Intercept at `Structure.generate` **RETURN** so jigsaw/piece layout BBs exist before cancel
- Overlap rule: block only when **different** structure registry ids have intersecting boxes
- Same registry id may overlap; pieces inside one start are never compared
- Config retargeted to participation filters (whitelist / blacklist / per-id enable)
- Commands updated for BB tracking (`cleanup` and center-distance settings removed)

### Removed
- Estimated structure radii / center-distance primary overlap rule
- Broken default blacklist entries using class-like names (`minecraft:jigsawstructure`, etc.)
- `minOverlapDistance` and `structureSpecificDistances` config options
- HEAD-of-generate interception and reflection-based structure id resolution

### Breaking Changes
- Config keys removed: `minOverlapDistance`, `structureSpecificDistances`
- Default blacklist is now empty (was incorrect class-name style ids)
- Overlap semantics are AABB intersection of different ids, not estimated exclusion spheres
- Existing `nostructureoverlap-common.toml` may need a one-time refresh for removed keys

## [2.0.1] - 2025-01-07

### Fixed
- **Method Signature Issue** - Fixed incorrect method signature for Structure.generate() in NeoForge 21.1.208
- **Mixin Compatibility** - Updated to use the correct parameter list for structure generation interception
- **Crash Resolution** - Resolved all remaining mixin injection failures

## [2.0.0] - 2025-01-07

### 🚀 MAJOR ARCHITECTURAL OVERHAUL

This is a complete rewrite of the overlap prevention system with significant performance and functionality improvements.

### Fixed (Dev Version Fix)
- **Mixin Target Issue** - Fixed crash caused by targeting non-existent `findStructurePositions` method
- **ChunkGenerator Mixin** - Updated to use `generateStructure` method instead for proper NeoForge compatibility
- **Structure Mixin** - Switched back to Structure.generate() method for reliable NeoForge compatibility
- **API Compatibility** - Resolved all mixin target issues with proper method signatures

### Added
- **Early Interception System** - Now intercepts structure placement at `ChunkGenerator.findStructurePositions()` instead of `Structure.generate()`
- **3D Overlap Detection** - Properly handles structures at different Y levels (underground vs surface structures)
- **2D/3D Toggle** - Configurable overlap detection mode (`use3DOverlapDetection` setting)
- **New StructurePlacementValidator** - Completely new, more efficient overlap validation system
- **Smart Exclusion Zones** - More intelligent structure placement validation
- **Enhanced Commands**:
  - `/nostructureoverlap toggle3D` - Toggle between 3D and 2D detection modes
  - Simplified command structure with cleaner output

### Changed
- **Performance Revolution** - Prevents overlaps before any generation work is done, eliminating wasted computation
- **Memory Efficiency** - Significantly reduced memory usage with optimized data structures
- **Architecture** - Complete rewrite from late interception to early interception approach
- **3D Detection** - Underground structures no longer incorrectly block surface structures and vice versa
- **Command System** - Streamlined commands with better organization and clearer output

### Removed
- **Old StructureMixin** - Removed the old `Structure.generate()` interception system
- **Old StructureOverlapManager** - Replaced with more efficient `StructurePlacementValidator`
- **Redundant Commands** - Removed commands that were specific to the old system
- **PreventRepeatedAttempts** - No longer needed with the new early interception approach

### Technical Details
- **Early Interception**: Structures are blocked at the placement decision level, not after generation starts
- **3D Distance Calculation**: Uses full 3D coordinates for accurate overlap detection
- **2D Fallback**: Option to use 2D detection for compatibility or specific use cases
- **ChunkGenerator Mixin**: Single, efficient mixin that handles all structure placement decisions
- **Automatic Cleanup**: Built-in memory management with configurable cleanup intervals

### Configuration
- **New config option in `nostructureoverlap-common.toml`:**
  - `use3DOverlapDetection` - Enable/disable 3D overlap detection (default: true)

### Breaking Changes
- **Command Changes**: Some commands have been renamed or removed
- **Configuration**: `preventRepeatedAttempts` setting removed (no longer needed)
- **Architecture**: Complete internal rewrite - may affect compatibility with other mods

### Migration Notes
- **Automatic**: No manual migration required
- **Configuration**: Existing configs will work with new defaults
- **Performance**: Expect significantly better performance, especially in areas with many structures

## [1.3.1] - 2025-01-07

### Fixed
- **Structure Name Configuration** - Fixed structure names in default blacklist to use correct Minecraft registry names
- **Configuration Comments** - Added helpful comments to all structure-related config options explaining the correct naming format

### Changed
- **Default Blacklist** - Updated to use correct structure names:
  - `minecraft:jigsawstructure` (was `minecraft:jigsaw_structure`)
  - `minecraft:netherfossilstructure` (was `minecraft:nether_fossil`)
  - `minecraft:fossilstructure` (was `minecraft:desert_pyramid`)
  - `minecraft:jungletemplestructure` (was `minecraft:jungle_pyramid`)
  - `minecraft:swamphutstructure` (was `minecraft:swamp_hut`)
  - `minecraft:igloostructure` (was `minecraft:igloo`)
  - `minecraft:alternatejigsawstructure` (new addition)

## [1.3.0] - 2025-01-07

### Added
- **Blocked Attempts Tracking** - Prevents repeated attempts to place structures in the same area after blocking
- **Smart Retry Prevention** - World generation no longer wastes time repeatedly trying to place blocked structures
- **Automatic Cleanup** - Expired blocked attempts are automatically cleaned up to prevent memory leaks
- **New Configuration Option** - `preventRepeatedAttempts` setting to control blocked attempts behavior (enabled by default)
- **New Commands**:
  - `/nostructureoverlap clearBlocked` - Clear all blocked attempts
  - `/nostructureoverlap cleanup` - Manually clean up expired blocked attempts
- **Enhanced Status Commands** - `/nostructureoverlap info` now shows blocked attempts count

### Changed
- **Performance Optimization** - Significantly reduced unnecessary overlap calculations for repeatedly blocked structures
- **Memory Management** - Blocked attempts expire after 5 minutes to prevent memory accumulation
- **World Generation Efficiency** - Game no longer repeatedly attempts to place structures in known blocked areas

### Technical Details
- Added `BlockedAttempt` class to track failed structure placement attempts
- Implemented proximity-based blocked attempt detection with configurable tolerance
- Enhanced `StructureOverlapManager` with blocked attempts tracking and cleanup
- Added automatic cleanup during blocked attempt checks to maintain performance
- Improved logging to show blocked attempt reasons and cleanup statistics

### Configuration
- **New config option in `nostructureoverlap-common.toml`:**
  - `preventRepeatedAttempts` - Enable/disable blocked attempts tracking (default: true)

## [1.2.2] - 2025-09-07

### Fixed
- **Configuration serialization crash** - Fixed TOML serialization issues by changing structure-specific configuration from Map format to List format for better compatibility with NeoForge's configuration system

## [1.2.1] - 2025-09-07

### Fixed
- **Configuration serialization crash** - Fixed `Unsupported value type: class java.util.HashMap` error by using `LinkedHashMap` instead of `HashMap` for structure-specific configuration maps

## [1.2] - 2025-09-07

### Added
- **Structure-specific configuration options** - Users can now configure overlap prevention on a per-structure basis
- **Structure whitelist** - List of structure IDs that should have overlap prevention enabled (if empty, all structures are included)
- **Structure blacklist** - List of structure IDs that should have overlap prevention disabled
- **Per-structure distance settings** - Custom minimum overlap distances for specific structures
- **Per-structure enable/disable settings** - Override enable/disable settings for specific structures
- **New command `/nostructureoverlap structures`** - View all structure-specific configuration settings
- **Enhanced `/nostructureoverlap info` command** - Now shows counts of configured structures
- **Default blacklist for small structures** - Fossil structures and other small structures are ignored by default

### Changed
- **Configuration system** - Expanded from basic global settings to comprehensive structure-specific configuration
- **StructureOverlapManager logic** - Now checks structure-specific configurations before applying overlap prevention
- **Default behavior** - Small structures like fossils, ruins, and temples are now ignored by default

### Configuration
- **New config options in `nostructureoverlap-common.toml`:**
  - `structureWhitelist` - Array of structure IDs to enable overlap prevention for
  - `structureBlacklist` - Array of structure IDs to disable overlap prevention for  
  - `structureSpecificDistances` - Map of structure ID to custom distance settings
  - `structureSpecificEnabled` - Map of structure ID to enable/disable overrides

### Default Blacklisted Structures
The following structures are now ignored by default (can be overridden in config):
- `minecraft:jigsawstructure`
- `minecraft:netherfossilstructure`
- `minecraft:fossilstructure`
- `minecraft:jungletemplestructure`
- `minecraft:swamphutstructure`
- `minecraft:igloostructure`
- `minecraft:alternatejigsawstructure`

### Technical Details
- Added validation methods for configuration values
- Implemented priority system for structure configuration (per-structure settings > blacklist > whitelist > defaults)
- Enhanced logging to show structure-specific distance settings
- Improved command output formatting and information display

## [1.1] - Previous Version

### Features
- Basic structure overlap prevention system
- Mixin-based structure generation interception
- Global configuration options (enable/disable, distance, logging)
- Basic commands for status and control

### Commands
- `/nostructureoverlap status` - Check mod status and tracked structures
- `/nostructureoverlap clear` - Clear all tracked structures
- `/nostructureoverlap toggle` - Toggle overlap prevention on/off
- `/nostructureoverlap info` - Display mod information and settings
