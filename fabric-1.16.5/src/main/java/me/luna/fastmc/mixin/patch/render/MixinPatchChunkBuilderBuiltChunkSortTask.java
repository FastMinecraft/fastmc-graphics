package me.luna.fastmc.mixin.patch.render;

import com.mojang.datafixers.util.Pair;
import me.luna.fastmc.mixin.IPatchedBlockBufferBuilderStorage;
import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.mixin.IPatchedRenderLayer;
import me.luna.fastmc.mixin.IPatchedTask;
import me.luna.fastmc.mixin.accessor.AccessorChunkData;
import me.luna.fastmc.terrain.VertexDataTransformer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChunkBuilder.BuiltChunk.SortTask.class)
public abstract class MixinPatchChunkBuilderBuiltChunkSortTask implements IPatchedTask {
    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public CompletableFuture<ChunkBuilder.Result> run(BlockBufferBuilderStorage buffers) {
        AtomicBoolean cancelled = getCancelled0();
        if (cancelled.get()) {
            return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
        } else {
            ChunkBuilder.BuiltChunk builtChunk = getBuiltChunk();
            IPatchedBuiltChunk patchedBuiltChunk = (IPatchedBuiltChunk) builtChunk;
            AccessorChunkData accessorChunkData = (AccessorChunkData) builtChunk.getData();

            if (cancelled.get()) {
                return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
            } else {
                ChunkBuilder chunkBuilder = getChunkBuilder();

                Vec3d vec3d = chunkBuilder.getCameraPosition();
                float f = (float) vec3d.x;
                float g = (float) vec3d.y;
                float h = (float) vec3d.z;
                BufferBuilder.State state = accessorChunkData.getBufferState();

                RenderLayer layer = RenderLayer.getTranslucent();
                if (state != null && !builtChunk.getData().isEmpty(layer)) {
                    BufferBuilder bufferBuilder = buffers.get(layer);
                    bufferBuilder.begin(7, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                    bufferBuilder.restoreState(state);
                    bufferBuilder.sortQuads(f - (float) builtChunk.getOrigin().getX(), g - (float) builtChunk.getOrigin().getY(), h - (float) builtChunk.getOrigin().getZ());
                    accessorChunkData.setBufferState(bufferBuilder.popState());
                    bufferBuilder.end();

                    if (cancelled.get()) {
                        return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
                    } else {
                        Pair<BufferBuilder.DrawArrayParameters, ByteBuffer> bufferData = buffers.get(layer).popData();
                        int vertexCount = bufferData.getFirst().getCount();
                        if (vertexCount == 0) return CompletableFuture.completedFuture(ChunkBuilder.Result.SUCCESSFUL);
                        long builtOrigin = builtChunk.getOrigin().asLong();

                        BlockPos regionOrigin = patchedBuiltChunk.getRegion().getOrigin();
                        BlockPos chunkOrigin = builtChunk.getOrigin();
                        float offsetX = (float) (chunkOrigin.getX() - regionOrigin.getX());
                        float offsetY = (float) (chunkOrigin.getY() - regionOrigin.getY());
                        float offsetZ = (float) (chunkOrigin.getZ() - regionOrigin.getZ());

                        int minCapacity = VertexDataTransformer.INSTANCE.transformedSize(vertexCount);
                        int newCapacity = (minCapacity + 2097151) >> 20 << 20;

                        ByteBuffer swapBuffer = ((IPatchedBlockBufferBuilderStorage) buffers).getCachedByteBuffer().getWithCapacity(minCapacity, newCapacity);
                        ByteBuffer buffer = bufferData.getSecond();

                        VertexDataTransformer.INSTANCE.transform(offsetX, offsetY, offsetZ, vertexCount, buffer, swapBuffer);
                        buffer.clear();
                        buffer.put(swapBuffer);
                        buffer.flip();

                        return scheduleUpload(() -> updateVertexData(
                            patchedBuiltChunk.getIndex(),
                            patchedBuiltChunk.getChunkVertexDataArray(),
                            ((IPatchedRenderLayer) layer).getIndex(),
                            builtOrigin,
                            buffer,
                            vertexCount
                        ));
                    }
                } else {
                    return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
                }
            }
        }
    }
}
