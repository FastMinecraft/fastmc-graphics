package me.luna.fastmc.mixin.core.render;

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

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class MixinChunkBuilderBuiltChunk {
    @Shadow public abstract ChunkBuilder.ChunkData getData();

    @Inject(method = "clear", at = @At("HEAD"))
    private void clear$Inject$HEAD(CallbackInfo ci) {
        ((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer())
            .updateEntities(Collections.emptyList(), ((IPatchedChunkData) this.getData()).getInstancingRenderTileEntities());
    }
}
