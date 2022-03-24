package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.mixin.IPatchedTask;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChunkBuilder.BuiltChunk.Task.class)
public abstract class MixinCoreChunkBuilderBuiltChunkTask implements IPatchedTask {
    @Shadow @Final protected AtomicBoolean cancelled;
    private ChunkBuilder chunkBuilder;
    private ChunkBuilder.BuiltChunk builtChunk;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(ChunkBuilder.BuiltChunk builtChunk, double d, CallbackInfo ci) {
        this.chunkBuilder = ((IPatchedBuiltChunk) builtChunk).getChunkBuilder();
        this.builtChunk = builtChunk;
    }

    @NotNull
    @Override
    public ChunkBuilder getChunkBuilder() {
        return chunkBuilder;
    }

    @NotNull
    @Override
    public ChunkBuilder.BuiltChunk getBuiltChunk() {
        return builtChunk;
    }

    @NotNull
    @Override
    public AtomicBoolean getCancelled0() {
        return cancelled;
    }
}
