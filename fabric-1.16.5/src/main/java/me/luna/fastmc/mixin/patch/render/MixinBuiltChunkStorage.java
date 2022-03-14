package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltChunkStorage.class)
public class MixinBuiltChunkStorage {
    @Shadow
    public ChunkBuilder.BuiltChunk[] chunks;

    @Inject(method = "createChunks", at = @At("RETURN"))
    private void createChunks$Inject$RETURN(ChunkBuilder chunkBuilder, CallbackInfo ci) {
        for (int i = 0; i < this.chunks.length; i++) {
            ((IPatchedBuiltChunk) this.chunks[i]).setIndex(i);
        }
    }
}
