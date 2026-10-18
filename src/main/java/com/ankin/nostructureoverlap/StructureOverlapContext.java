package com.ankin.nostructureoverlap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;

import javax.annotation.Nullable;

/**
 * Thread-local worldgen context so Structure.generate can resolve dimension and
 * optionally query already-persisted structure starts via StructureManager.
 */
public final class StructureOverlapContext {
    private static final ThreadLocal<Active> ACTIVE = new ThreadLocal<>();

    private StructureOverlapContext() {}

    public static void push(StructureManager structureManager) {
        ResourceKey<Level> dimension = resolveDimension(structureManager);
        ACTIVE.set(new Active(dimension, structureManager));
    }

    public static void pop() {
        ACTIVE.remove();
    }

    @Nullable
    public static Active current() {
        return ACTIVE.get();
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

    public record Active(@Nullable ResourceKey<Level> dimension, StructureManager structureManager) {}
}
