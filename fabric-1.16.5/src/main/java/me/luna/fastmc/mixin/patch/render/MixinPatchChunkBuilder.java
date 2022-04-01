package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedChunkBuilder;
import me.luna.fastmc.shared.FpsDisplay;
import me.luna.fastmc.shared.util.ParallelUtils;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.Executor;

@Mixin(ChunkBuilder.class)
public class MixinPatchChunkBuilder implements IPatchedChunkBuilder {
    @Mutable
    @Shadow
    @Final
    private BlockBufferBuilderStorage buffers;

    @Shadow
    @Final
    private Queue<Runnable> uploadQueue;

    @Shadow
    private volatile int bufferCount;

    @Shadow
    @Final
    private Queue<BlockBufferBuilderStorage> threadBuffers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(World world, WorldRenderer worldRenderer, Executor executor, boolean is64Bits, BlockBufferBuilderStorage buffers, CallbackInfo ci) {
        this.buffers = null;
        int newSize = ParallelUtils.CPU_THREADS * 2;
        int remaining = newSize - this.bufferCount;
        for (int i = 0; i < remaining; i++) {
            this.threadBuffers.add(new BlockBufferBuilderStorage());
        }
        this.bufferCount = newSize;
    }

    @Override
    public int upload(boolean @NotNull [] running) {
        int count = 0;
        int max = (ParallelUtils.CPU_THREADS * 500) / (FpsDisplay.INSTANCE.getFpsValue());
        int min = Math.max(max / 4, 2);
        Runnable runnable;

        while (count < max && (runnable = this.uploadQueue.poll()) != null) {
            runnable.run();
            count++;
            if (count >= min && !running[0]) break;
        }

        return count;
    }
}
