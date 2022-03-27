package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedChunkBuilder;
import me.luna.fastmc.shared.util.ParallelUtils;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChunkBuilder.class)
public class MixinPatchChunkBuilder implements IPatchedChunkBuilder {
    @Mutable
    @Shadow
    @Final
    private BlockBufferBuilderStorage buffers;

    @Shadow @Final private Queue<Runnable> uploadQueue;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(World world, WorldRenderer worldRenderer, Executor executor, boolean is64Bits, BlockBufferBuilderStorage buffers, CallbackInfo ci) {
        this.buffers = null;
    }

    @Override
    public boolean upload(boolean @NotNull [] running) {
        boolean uploaded = false;
        int count = Math.max(ParallelUtils.CPU_THREADS, this.uploadQueue.size() / 2);
        int min = count / 2;
        Runnable runnable;

        while (count-- > 0 && (runnable = this.uploadQueue.poll()) != null) {
            runnable.run();
            uploaded = true;
            if (count <= min && !running[0]) break;
        }

        return uploaded;
    }
}
