package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.IPatchedChunkData;
import me.luna.fastmc.renderer.TileEntityRenderer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
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

    @Inject(method = "run", at = @At("RETURN"))
    private void run$Inject$RETURN(BlockBufferBuilderStorage buffers, CallbackInfoReturnable<CompletableFuture<ChunkBuilder.Result>> cir) {
        cir.getReturnValue().whenComplete((result, throwable) -> {
            if (result == ChunkBuilder.Result.SUCCESSFUL) {
                ((IPatchedChunkData) field_20839.getData()).onComplete();
            }
        });
    }
}
