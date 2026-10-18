package com.ankin.nostructureoverlap.mixin;

import com.ankin.nostructureoverlap.StructureOverlapContext;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Publishes StructureManager (and thus dimension) for Structure.generate overlap checks.
 */
@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {
    @Inject(method = "createStructures", at = @At("HEAD"))
    private void nostructureoverlap$pushContext(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager,
            CallbackInfo ci
    ) {
        StructureOverlapContext.push(structureManager);
    }

    @Inject(method = "createStructures", at = @At("RETURN"))
    private void nostructureoverlap$popContext(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager,
            CallbackInfo ci
    ) {
        StructureOverlapContext.pop();
    }
}
