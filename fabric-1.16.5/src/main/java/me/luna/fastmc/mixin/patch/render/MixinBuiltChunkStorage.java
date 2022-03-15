package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.mixin.IPatchedBuiltChunkStorage;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltChunkStorage.class)
public abstract class MixinBuiltChunkStorage implements IPatchedBuiltChunkStorage {
    @Shadow
    public ChunkBuilder.BuiltChunk[] chunks;

    @Shadow protected int sizeY;

    @Shadow protected int sizeX;

    @Shadow protected int sizeZ;

    @Inject(method = "createChunks", at = @At("RETURN"))
    private void createChunks$Inject$RETURN(ChunkBuilder chunkBuilder, CallbackInfo ci) {
        for (int i = 0; i < this.chunks.length; i++) {
            ((IPatchedBuiltChunk) this.chunks[i]).setIndex(i);
        }
    }

    @Nullable
    @Override
    public ChunkBuilder.BuiltChunk getRenderedChunk(@NotNull BlockPos pos) {
        return getRenderedChunk(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    @Nullable
    @Override
    public ChunkBuilder.BuiltChunk getRenderedChunk(int chunkX, int chunkY, int chunkZ) {
        if (chunkY >= 0 && chunkY < this.sizeY) {
            chunkX = MathHelper.floorMod(chunkX, this.sizeX);
            chunkZ = MathHelper.floorMod(chunkZ, this.sizeZ);
            return this.chunks[(chunkZ * this.sizeY + chunkY) * this.sizeX + chunkX];
        } else {
            return null;
        }
    }
}
