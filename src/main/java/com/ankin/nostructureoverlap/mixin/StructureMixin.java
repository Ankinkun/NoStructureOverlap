package com.ankin.nostructureoverlap.mixin;

import com.ankin.nostructureoverlap.Config;
import com.ankin.nostructureoverlap.StructureOverlapContext;
import com.ankin.nostructureoverlap.StructurePlacementValidator;
import com.mojang.logging.LogUtils;
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
import net.minecraft.core.Holder;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * After piece layout finishes (MiniHUD-equivalent BB available), reject starts that
 * intersect a different structure registry id. Returning INVALID_START prevents
 * setStartForStructure and later block placement.
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

        StructureOverlapContext.Active context = StructureOverlapContext.current();
        boolean accepted = StructurePlacementValidator.tryAccept(
                context != null ? context.dimension() : null,
                structureId,
                start,
                context != null ? context.structureManager() : null
        );

        if (!accepted) {
            cir.setReturnValue(StructureStart.INVALID_START);
        }
    }
}
