# Changelog

All notable changes to the NoStructureOverlap mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.1] - 2026-10-18

### Changed
- Renamed tracker to `StructureBoundingBoxTracker` (pure bounding-box path only)
- Removed leftover StructureManager scan path and legacy placement-validator naming
- Commands reduced to read-only `status` / `info` / `structures`
- Docs and metadata cleaned up (no MiniHUD references)

### Notes
- Same core behavior as 3.0.0: vanilla `StructureStart` AABB overlap for different registry ids
- **1.21.1:** branch `3.0.0-for-1.21.1` / tag `v3.0.1`
- **1.20.1:** branch `3.0.1-for-1.20.1` (NeoForge `1.20.1-47.1.106`, Java 17, Forge-compatible API)

## [3.0.0] - 2026-10-18

### Added
- Structure overlap prevention using vanilla `StructureStart.getBoundingBox()` after piece layout
- Cancel via `StructureStart.INVALID_START` before storage and block placement
- Overlap rule: different structure registry ids only (same id may overlap)
- Optional 3D or 2D AABB checks and per-structure participation filters
- Dimension-scoped in-memory bounding-box tracker

### Notes
- Replaces earlier experimental approaches (estimated radii, early HEAD interception, etc.)
- Configure via `config/nostructureoverlap-common.toml`
- Commands: `/nostructureoverlap status|info|structures`
