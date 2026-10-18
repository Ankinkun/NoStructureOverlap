package com.ankin.nostructureoverlap;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;

/**
 * Bridge to the StructureManager level accessor mixin. Kept in a separate class
 * so production code does not import mixin interfaces directly in hot paths beyond this call.
 */
public final class StructureManagerAccess {
    private StructureManagerAccess() {}

    public static LevelAccessor getLevel(StructureManager structureManager) {
        return ((com.ankin.nostructureoverlap.mixin.StructureManagerAccessor) structureManager).nostructureoverlap$getLevel();
    }
}
