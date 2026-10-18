package com.ankin.nostructureoverlap.mixin;

import com.ankin.nostructureoverlap.Config;
import com.ankin.nostructureoverlap.StructureBoundingBoxTracker;
import com.ankin.nostructureoverlap.StructureOverlapContext;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * After piece layout, reject starts whose bounding boxes intersect a different
 * structure registry id by returning {@link StructureStart#INVALID_START}.
 */
@Mixin(Structure.class)
public class StructureMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "generate", at = @At("RETURN"), cancellable = true)
    private void nostructureoverlap$onGenerateReturn(
            RegistryAccess registryAccess,
            ChunkGenerator chunkGenerator,
            BiomeSource biomeSource,
            RandomState randomState,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkPos chunkPos,
            int references,
            LevelHeightAccessor heightAccessor,
            Predicate<Holder<Biome>> validBiome,
            CallbackInfoReturnable<StructureStart> cir
    ) {
        if (!Config.enableOverlapPrevention) {
            return;
        }

        StructureStart start = cir.getReturnValue();
        if (start == null || !start.isValid()) {
            return;
        }

        Structure structure = (Structure) (Object) this;
        ResourceLocation structureId = registryAccess.registryOrThrow(Registries.STRUCTURE).getKey(structure);
        if (structureId == null) {
            LOGGER.warn("Structure {} has no registry key; skipping overlap check", structure.getClass().getName());
            return;
        }

        if (!Config.isStructureEnabled(structureId.toString())) {
            return;
        }

        if (!StructureBoundingBoxTracker.tryAccept(StructureOverlapContext.dimension(), structureId, start)) {
            cir.setReturnValue(StructureStart.INVALID_START);
        }
    }
}
