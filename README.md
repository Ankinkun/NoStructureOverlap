# No Structure Overlap v3.0.0

![NoStructureOverlap](src/main/resources/nostructureoverlap.png)

A NeoForge mod for Minecraft 1.21.1 that prevents **different** structure types from generating inside each other by comparing vanilla `StructureStart` bounding boxes after piece layout and before blocks are placed.

## Features

- **Vanilla bounding boxes** — uses `StructureStart.getBoundingBox()` after jigsaw/piece layout
- **Clean cancel** — returns `INVALID_START` before the start is stored and before blocks place
- **Different IDs only** — same structure registry id may still overlap (e.g. two villages); pieces within one start are never treated as conflicts
- **3D or 2D checks** — full AABB by default; optional X/Z-only mode via config
- **Per-structure filters** — whitelist, blacklist, and enable/disable overrides
- **Modded structures** — works for anything that goes through vanilla `Structure.generate` / `StructureStart`

## How it works

1. Vanilla spacing / structure sets choose candidate chunks
2. `Structure.generate` builds pieces and creates a `StructureStart` (bounding box available)
3. This mod rejects the start if its box intersects another accepted start with a **different** registry id
4. Blocks are only placed later in `placeInChunk` for starts that remain valid

## Configuration

File: `config/nostructureoverlap-common.toml`

| Option | Default | Meaning |
|--------|---------|---------|
| `enableOverlapPrevention` | `true` | Master toggle |
| `use3DOverlapDetection` | `true` | `true` = full 3D `BoundingBox.intersects`; `false` = X/Z only |
| `logBlockedStructures` | `true` | Log blocked placements |
| `structureWhitelist` | `[]` | If non-empty, only these ids participate |
| `structureBlacklist` | `[]` | These ids always skip checks |
| `structureSpecificEnabled` | `[]` | Per-id overrides, e.g. `["minecraft:mineshaft=false"]` |

Use real registry ids such as `minecraft:village_plains`, not class names. Change settings in the config file (not via runtime toggles).

## Commands

- `/nostructureoverlap status` — enabled state and tracked structure count
- `/nostructureoverlap info` — detailed status
- `/nostructureoverlap structures` — show participation filters

## Installation

1. Install NeoForge for Minecraft **1.21.1**
2. Drop `nostructureoverlap-3.0.0.jar` into `mods/`
3. Start the game or dedicated server

## Compatibility

- Minecraft **1.21.1**
- NeoForge **21.1.208** (project pin; range `[21,)`)
- Vanilla and modded structures using the standard structure pipeline
- Not covered: custom worldgen that places blocks without `StructureStart`

## Building

```powershell
.\gradlew.bat build
```

JAR output: `build/libs/nostructureoverlap-3.0.0.jar`

## License

See [LICENSE](LICENSE).
