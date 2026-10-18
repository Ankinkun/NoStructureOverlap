package com.ankin.nostructureoverlap;

import com.mojang.logging.LogUtils;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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
 * Tracks accepted {@link StructureStart} bounding boxes and rejects new starts
 * whose boxes intersect a different structure registry id.
 */
public final class StructureBoundingBoxTracker {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** dimension id -> chunk key -> placements indexed under that chunk */
    private static final Map<String, Map<Long, Set<TrackedStructure>>> BY_DIMENSION = new ConcurrentHashMap<>();

    private StructureBoundingBoxTracker() {}

    public record TrackedStructure(
            ResourceLocation structureId,
            BoundingBox boundingBox,
            ChunkPos startChunk
    ) {
        public TrackedStructure {
            Objects.requireNonNull(structureId, "structureId");
            Objects.requireNonNull(boundingBox, "boundingBox");
            Objects.requireNonNull(startChunk, "startChunk");
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
     * @return {@code true} if vanilla may store this start
     */
    public static boolean tryAccept(
            @Nullable ResourceKey<Level> dimension,
            ResourceLocation structureId,
            StructureStart start
    ) {
        if (!Config.enableOverlapPrevention || !start.isValid()) {
            return true;
        }
        if (!Config.isStructureEnabled(structureId.toString())) {
            return true;
        }

        TrackedStructure candidate = new TrackedStructure(
                structureId,
                start.getBoundingBox(),
                start.getChunkPos()
        );
        String dimKey = dimensionKey(dimension);

        synchronized (dimKey.intern()) {
            TrackedStructure conflict = findConflict(dimKey, candidate);
            if (conflict != null) {
                if (Config.logBlockedStructures) {
                    LOGGER.info(
                            "Blocked {} at ({}, {}) {} — overlaps {} at ({}, {}) {}",
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
            register(dimKey, candidate);
            return true;
        }
    }

    @Nullable
    private static TrackedStructure findConflict(String dimKey, TrackedStructure candidate) {
        boolean use3D = Config.use3DOverlapDetection;
        for (TrackedStructure existing : collectNearby(dimKey, candidate.boundingBox)) {
            if (existing.structureId.equals(candidate.structureId)) {
                continue;
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

    private static List<TrackedStructure> collectNearby(String dimKey, BoundingBox box) {
        Map<Long, Set<TrackedStructure>> byChunk = BY_DIMENSION.get(dimKey);
        if (byChunk == null || byChunk.isEmpty()) {
            return List.of();
        }

        List<TrackedStructure> result = new ArrayList<>();
        forEachIntersectingChunk(box, chunkPos -> {
            Set<TrackedStructure> set = byChunk.get(chunkPos.toLong());
            if (set != null) {
                result.addAll(set);
            }
        });
        return result;
    }

    private static void register(String dimKey, TrackedStructure placement) {
        Map<Long, Set<TrackedStructure>> byChunk =
                BY_DIMENSION.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());

        forEachIntersectingChunk(placement.boundingBox, chunkPos ->
                byChunk.computeIfAbsent(chunkPos.toLong(), k -> ConcurrentHashMap.newKeySet()).add(placement));

        if (Config.logBlockedStructures) {
            LOGGER.debug(
                    "Accepted {} at ({}, {}) {}",
                    placement.structureId,
                    placement.startChunk.x,
                    placement.startChunk.z,
                    formatBox(placement.boundingBox)
            );
        }
    }

    public static void clearAll() {
        BY_DIMENSION.clear();
    }

    public static void clearDimension(@Nullable ResourceKey<Level> dimension) {
        BY_DIMENSION.remove(dimensionKey(dimension));
    }

    public static int getTrackedCount() {
        return (int) BY_DIMENSION.values().stream()
                .flatMap(map -> map.values().stream())
                .flatMap(Set::stream)
                .distinct()
                .count();
    }

    private static String dimensionKey(@Nullable ResourceKey<Level> dimension) {
        return dimension != null ? dimension.location().toString() : "unknown";
    }

    /**
     * 1.20.1 BoundingBox has no intersectingChunks(); iterate section coords manually.
     */
    private static void forEachIntersectingChunk(BoundingBox box, java.util.function.Consumer<ChunkPos> consumer) {
        int minChunkX = SectionPos.blockToSectionCoord(box.minX());
        int minChunkZ = SectionPos.blockToSectionCoord(box.minZ());
        int maxChunkX = SectionPos.blockToSectionCoord(box.maxX());
        int maxChunkZ = SectionPos.blockToSectionCoord(box.maxZ());
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                consumer.accept(new ChunkPos(x, z));
            }
        }
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
