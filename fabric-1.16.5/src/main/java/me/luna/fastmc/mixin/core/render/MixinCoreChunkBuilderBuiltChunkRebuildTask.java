package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.IPatchedChunkData;
import me.luna.fastmc.mixin.IPatchedTask;
import me.luna.fastmc.renderer.TileEntityRenderer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ChunkBuilder.BuiltChunk.RebuildTask.class)
public abstract class MixinCoreChunkBuilderBuiltChunkRebuildTask implements IPatchedTask {
    @Inject(method = "addBlockEntity", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void addBlockEntity$Inject$HEAD(ChunkBuilder.ChunkData data, Set<BlockEntity> blockEntities, E blockEntity, CallbackInfo ci) {
        if (((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer()).hasRenderer(blockEntity)) {
            ((IPatchedChunkData) data).getInstancingRenderTileEntities().add(blockEntity);
            ci.cancel();
        }
    }
}
