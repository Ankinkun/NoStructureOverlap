package com.ankin.nostructureoverlap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;

import javax.annotation.Nullable;

/**
 * Thread-local dimension for structure generation so bounding-box tracking
 * stays per-world during concurrent chunk gen.
 */
public final class StructureOverlapContext {
    private static final ThreadLocal<ResourceKey<Level>> DIMENSION = new ThreadLocal<>();

    private StructureOverlapContext() {}

    public static void push(StructureManager structureManager) {
        DIMENSION.set(resolveDimension(structureManager));
    }

    public static void pop() {
        DIMENSION.remove();
    }

    @Nullable
    public static ResourceKey<Level> dimension() {
        return DIMENSION.get();
    }

    @Nullable
    private static ResourceKey<Level> resolveDimension(StructureManager structureManager) {
        LevelAccessor level = StructureManagerAccess.getLevel(structureManager);
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.dimension();
        }
        if (level instanceof WorldGenRegion region) {
            return region.getLevel().dimension();
        }
        return null;
    }
}
