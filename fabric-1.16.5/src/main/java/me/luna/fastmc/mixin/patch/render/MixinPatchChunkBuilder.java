package me.luna.fastmc.mixin.patch.render;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(ChunkBuilder.class)
public class MixinPatchChunkBuilder {
    @Mutable
    @Shadow
    @Final
    private BlockBufferBuilderStorage buffers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(World world, WorldRenderer worldRenderer, Executor executor, boolean is64Bits, BlockBufferBuilderStorage buffers, CallbackInfo ci) {
        this.buffers = null;
    }
}
