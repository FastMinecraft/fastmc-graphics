package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.IPatchedChunkData;
import me.luna.fastmc.renderer.TileEntityRenderer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Mixin(ChunkBuilder.BuiltChunk.RebuildTask.class)
public abstract class MixinChunkBuilderBuiltChunkRebuildTask {
    @Shadow(remap = false, aliases = { "this$0", "a" })
    @Final
    ChunkBuilder.BuiltChunk field_20839;

    @Inject(method = "addBlockEntity", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void addBlockEntity$Inject$HEAD(ChunkBuilder.ChunkData data, Set<BlockEntity> blockEntities, E blockEntity, CallbackInfo ci) {
        if (((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer()).hasRenderer(blockEntity)) {
            ((IPatchedChunkData) data).getInstancingRenderTileEntities().add(blockEntity);
            ci.cancel();
        }
    }

    @Inject(method = "run", at = @At("RETURN"))
    private void run$Inject$RETURN(BlockBufferBuilderStorage buffers, CallbackInfoReturnable<CompletableFuture<ChunkBuilder.Result>> cir) {
        ChunkBuilder.ChunkData oldData = field_20839.getData();

        cir.getReturnValue().whenComplete((result, throwable) -> {
            if (result == ChunkBuilder.Result.SUCCESSFUL) {
                ChunkBuilder.ChunkData newData = field_20839.getData();
                if (oldData == newData) return;

                List<BlockEntity> oldList = ((IPatchedChunkData) oldData).getInstancingRenderTileEntities();
                List<BlockEntity> newList = ((IPatchedChunkData) newData).getInstancingRenderTileEntities();

                boolean oldEmpty = oldList.isEmpty();
                boolean newEmpty = newList.isEmpty();

                if (!oldEmpty || !newEmpty) {
                    TileEntityRenderer renderer = ((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer());

                    if (oldEmpty) {
                        renderer.updateEntities(newList, Collections.emptyList());
                    } else if (newEmpty) {
                        renderer.updateEntities(Collections.emptyList(), oldList);
                    } else {
                        Set<BlockEntity> oldSet = new HashSet<>(oldList);
                        Set<BlockEntity> newSet = new HashSet<>(newList);

                        List<BlockEntity> adding = new ArrayList<>();
                        List<BlockEntity> removing = new ArrayList<>();

                        for (BlockEntity e : newList) {
                            if (!oldSet.contains(e)) {
                                adding.add(e);
                            }
                        }

                        for (BlockEntity e : oldList) {
                            if (!newSet.contains(e)) {
                                removing.add(e);
                            }
                        }

                        renderer.updateEntities(adding, removing);
                    }
                }
            }
        });
    }
}
