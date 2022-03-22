package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldRenderer.class)
public interface AccessorWorldRenderer {
    @Nullable
    @Invoker
    ChunkBuilder.BuiltChunk invokeGetAdjacentChunk(BlockPos pos, ChunkBuilder.BuiltChunk chunk, Direction direction);

    @Accessor
    BuiltChunkStorage getChunks();
}
