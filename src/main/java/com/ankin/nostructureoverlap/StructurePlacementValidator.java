package com.ankin.nostructureoverlap;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks accepted structure starts by MiniHUD-equivalent bounding boxes and
 * rejects new starts whose boxes intersect a different structure registry id.
 */
public final class StructurePlacementValidator {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** dimension id string -> chunkKey -> placements anchored in that chunk */
    private static final Map<String, Map<Long, Set<TrackedStructure>>> BY_DIMENSION = new ConcurrentHashMap<>();

    private StructurePlacementValidator() {}

    public record TrackedStructure(
            ResourceLocation structureId,
            BoundingBox boundingBox,
            ChunkPos startChunk
    ) {
        public TrackedStructure {
            Objects.requireNonNull(structureId, "structureId");
            Objects.requireNonNull(boundingBox, "boundingBox");
            Objects.requireNonNull(startChunk, "startChunk");
            // Defensive copy: BoundingBox is mutable
            boundingBox = copyBox(boundingBox);
        }

        public boolean overlaps(TrackedStructure other, boolean use3D) {
            if (use3D) {
                return boundingBox.intersects(other.boundingBox);
            }
            return boundingBox.intersects(
                    other.boundingBox.minX(),
                    other.boundingBox.minZ(),
                    other.boundingBox.maxX(),
                    other.boundingBox.maxZ()
            );
        }
    }

    /**
     * Atomically checks for a conflicting different-id BB and registers on success.
     *
     * @return {@code true} if the start may be stored by vanilla
     */
    public static boolean tryAccept(
            @Nullable ResourceKey<Level> dimension,
            ResourceLocation structureId,
            StructureStart start,
            @Nullable StructureManager structureManager
    ) {
        if (!Config.enableOverlapPrevention) {
            return true;
        }
        if (!start.isValid()) {
            return true;
        }
        if (!Config.isStructureEnabled(structureId.toString())) {
            return true;
        }

        BoundingBox box = copyBox(start.getBoundingBox());
        TrackedStructure candidate = new TrackedStructure(structureId, box, start.getChunkPos());
        String dimKey = dimensionKey(dimension);

        // Per-dimension monitor: check+register must be atomic across worldgen threads.
        synchronized (lockFor(dimKey)) {
            // In-memory tracker is the source of truth during STRUCTURE_STARTS.
            // Optional non-blocking scan of already-loaded neighbor starts covers restarts
            // when neighbors are already present in the live chunk map.
            TrackedStructure conflict = findTrackedConflict(dimKey, candidate);
            if (conflict == null && structureManager != null) {
                conflict = findLoadedChunkConflict(structureManager, candidate);
            }
            if (conflict != null) {
                if (Config.logBlockedStructures) {
                    LOGGER.info(
                            "Blocked structure {} at chunk ({}, {}) — BB {} overlaps {} at chunk ({}, {}) BB {}",
                            structureId,
                            candidate.startChunk.x,
                            candidate.startChunk.z,
                            formatBox(candidate.boundingBox),
                            conflict.structureId,
                            conflict.startChunk.x,
                            conflict.startChunk.z,
                            formatBox(conflict.boundingBox)
                    );
                }
                return false;
            }
            registerUnlocked(dimKey, candidate);
            return true;
        }
    }

    @Nullable
    private static TrackedStructure findTrackedConflict(String dimKey, TrackedStructure candidate) {
        boolean use3D = Config.use3DOverlapDetection;

        for (TrackedStructure existing : collectNearbyTracked(dimKey, candidate.boundingBox)) {
            if (existing.structureId.equals(candidate.structureId)) {
                continue; // same registry id may overlap
            }
            if (!Config.isStructureEnabled(existing.structureId.toString())) {
                continue;
            }
            if (candidate.overlaps(existing, use3D)) {
                return existing;
            }
        }
        return null;
    }

    /**
     * Only inspect chunks already resident in the server chunk map.
     * Never request status upgrades — that deadlocks during STRUCTURE_STARTS.
     */
    @Nullable
    private static TrackedStructure findLoadedChunkConflict(
            StructureManager structureManager,
            TrackedStructure candidate
    ) {
        LevelAccessor level = StructureManagerAccess.getLevel(structureManager);
        if (!(level instanceof ServerLevel serverLevel)) {
            // WorldGenRegion / other accessors: skip — in-memory tracker covers concurrent gen.
            return null;
        }

        Registry<Structure> registry = structureManager.registryAccess().registryOrThrow(Registries.STRUCTURE);
        boolean use3D = Config.use3DOverlapDetection;
        var chunkSource = serverLevel.getChunkSource();

        for (ChunkPos chunkPos : candidate.boundingBox.intersectingChunks().toList()) {
            ChunkAccess chunk = chunkSource.getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk == null || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.STRUCTURE_STARTS)) {
                continue;
            }

            for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                StructureStart existingStart = entry.getValue();
                if (existingStart == null || !existingStart.isValid()) {
                    continue;
                }

                ResourceLocation existingId = registry.getKey(entry.getKey());
                if (existingId == null || existingId.equals(candidate.structureId)) {
                    continue;
                }
                if (!Config.isStructureEnabled(existingId.toString())) {
                    continue;
                }

                TrackedStructure existing = new TrackedStructure(
                        existingId,
                        existingStart.getBoundingBox(),
                        existingStart.getChunkPos()
                );
                if (candidate.overlaps(existing, use3D)) {
                    return existing;
                }
            }
        }
        return null;
    }

    private static List<TrackedStructure> collectNearbyTracked(String dimKey, BoundingBox box) {
        Map<Long, Set<TrackedStructure>> byChunk = BY_DIMENSION.get(dimKey);
        if (byChunk == null || byChunk.isEmpty()) {
            return List.of();
        }

        List<TrackedStructure> result = new ArrayList<>();
        for (ChunkPos chunkPos : box.intersectingChunks().toList()) {
            Set<TrackedStructure> set = byChunk.get(chunkPos.toLong());
            if (set != null) {
                result.addAll(set);
            }
        }
        return result;
    }

    private static void registerUnlocked(String dimKey, TrackedStructure placement) {
        Map<Long, Set<TrackedStructure>> byChunk =
                BY_DIMENSION.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());

        // Index under every chunk the BB touches so lookups by intersectingChunks are complete.
        for (ChunkPos chunkPos : placement.boundingBox.intersectingChunks().toList()) {
            byChunk.computeIfAbsent(chunkPos.toLong(), k -> ConcurrentHashMap.newKeySet()).add(placement);
        }

        if (Config.logBlockedStructures) {
            LOGGER.debug(
                    "Accepted structure {} at chunk ({}, {}) BB {}",
                    placement.structureId,
                    placement.startChunk.x,
                    placement.startChunk.z,
                    formatBox(placement.boundingBox)
            );
        }
    }

    public static void clearStructures() {
        BY_DIMENSION.clear();
        LOGGER.info("Cleared all tracked structure bounding boxes");
    }

    public static void clearDimension(@Nullable ResourceKey<Level> dimension) {
        String key = dimensionKey(dimension);
        if (BY_DIMENSION.remove(key) != null) {
            LOGGER.info("Cleared tracked structures for dimension {}", key);
        }
    }

    public static int getStructureCount() {
        // Each placement is indexed in multiple chunks; count unique instances by identity.
        return (int) BY_DIMENSION.values().stream()
                .flatMap(map -> map.values().stream())
                .flatMap(Set::stream)
                .distinct()
                .count();
    }

    public static int getStructureCount(ResourceLocation structureId) {
        return (int) BY_DIMENSION.values().stream()
                .flatMap(map -> map.values().stream())
                .flatMap(Set::stream)
                .filter(tracked -> tracked.structureId.equals(structureId))
                .distinct()
                .count();
    }

    private static String dimensionKey(@Nullable ResourceKey<Level> dimension) {
        return dimension != null ? dimension.location().toString() : "unknown";
    }

    private static Object lockFor(String dimKey) {
        // Intern so all threads sharing a dimension share one monitor.
        return dimKey.intern();
    }

    private static BoundingBox copyBox(BoundingBox box) {
        return new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    private static String formatBox(BoundingBox box) {
        return String.format(
                "[%d,%d,%d -> %d,%d,%d]",
                box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()
        );
    }
}
