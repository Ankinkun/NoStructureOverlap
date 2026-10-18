# No Structure Overlap v3.0.0

![NoStructureOverlap](src/main/resources/nostructureoverlap.png)

A NeoForge mod for Minecraft 1.21.1 that prevents **different** structure types from generating inside each other by comparing vanilla `StructureStart` bounding boxes — the same boxes [MiniHUD](https://modrinth.com/project/UMxybHE8) displays.

## Features

- **MiniHUD-equivalent bounding boxes** — uses `StructureStart.getBoundingBox()` after jigsaw/piece layout
- **Clean cancel** — returns `INVALID_START` before the start is stored and before blocks place
- **Different IDs only** — same structure registry id may still overlap (e.g. two villages); pieces within one start are never treated as conflicts
- **3D or 2D checks** — full AABB by default; optional X/Z-only mode
- **Per-structure filters** — whitelist, blacklist, and enable/disable overrides
- **Modded structures** — works for anything that goes through vanilla `Structure.generate` / `StructureStart`

## How it works

Vanilla already splits layout from build:

1. Spacing / exclusion zones choose candidate chunks
2. `Structure.generate` builds pieces (jigsaw layout included) and creates a `StructureStart`
3. Bounding box is available immediately from that start
4. Blocks are placed much later in `placeInChunk`

This mod hooks **after** step 2. If the new start’s box intersects another accepted start with a **different** registry id, it returns `StructureStart.INVALID_START` so vanilla never stores or places it.

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

Use real registry ids such as `minecraft:village_plains`, not class names.

## Commands

- `/nostructureoverlap status` — enabled state and tracked structure count
- `/nostructureoverlap clear` — clear the in-memory BB tracker
- `/nostructureoverlap toggle` — toggle prevention
- `/nostructureoverlap toggle3D` — toggle 3D vs 2D
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
