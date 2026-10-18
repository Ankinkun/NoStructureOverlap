# NoStructureOverlap 3.0.1 (Minecraft 1.20.1)

![NoStructureOverlap](src/main/resources/nostructureoverlap.png)

NeoForge/Forge-compatible mod for **Minecraft 1.20.1** that stops **different** structures from generating inside each other.

It compares vanilla `StructureStart` bounding boxes after piece layout (including jigsaw) and before blocks are placed. Same structure id may still overlap; pieces inside one start are never treated as conflicts.

> **Note:** This branch targets **NeoForge 1.20.1** (`1.20.1-47.1.106`). NeoForged considers 1.20.1 NeoForge legacy; most 1.20.1 packs use MinecraftForge instead. The code uses the Forge-compatible API surface NeoForge 1.20.1 provides.

## How it works

1. Vanilla chooses a candidate chunk and runs `Structure.generate`
2. Pieces are built → `StructureStart.getBoundingBox()` is available
3. If that box intersects another tracked start with a **different** registry id, the mod returns `StructureStart.INVALID_START`
4. Vanilla never stores or places the blocked start

## Config

`config/nostructureoverlap-common.toml`

| Option | Default | Description |
|--------|---------|-------------|
| `enableOverlapPrevention` | `true` | Master switch |
| `use3DOverlapDetection` | `true` | Full 3D AABB; `false` = X/Z only |
| `logBlockedStructures` | `true` | Log when a start is blocked |
| `structureWhitelist` | `[]` | If set, only these ids are checked |
| `structureBlacklist` | `[]` | These ids skip checks |
| `structureSpecificEnabled` | `[]` | Overrides like `["minecraft:mineshaft=false"]` |

## Commands

| Command | Description |
|---------|-------------|
| `/nostructureoverlap status` | Enabled state, tracked count, 3D/2D mode |
| `/nostructureoverlap info` | Full status summary |
| `/nostructureoverlap structures` | Active whitelist / blacklist / overrides |

## Install

1. NeoForge **1.20.1** (or a compatible Forge 1.20.1 loader that accepts this jar — verify in your pack)
2. Put `nostructureoverlap-3.0.1.jar` in `mods/`
3. Launch client or dedicated server

Server-only install is supported (`displayTest = IGNORE_SERVER_VERSION`).

## Build

```powershell
.\gradlew.bat build
```

Output: `build/libs/nostructureoverlap-3.0.1.jar`

## License

See [LICENSE](LICENSE).
