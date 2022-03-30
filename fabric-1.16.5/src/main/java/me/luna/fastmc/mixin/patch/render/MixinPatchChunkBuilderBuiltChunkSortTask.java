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

import static org.lwjgl.opengl.GL11.GL_QUADS;

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
                BufferBuilder.State state = accessorChunkData.getBufferState();

                RenderLayer layer = RenderLayer.getTranslucent();
                if (state != null && !builtChunk.getData().isEmpty(layer)) {
                    BufferBuilder bufferBuilder = buffers.get(layer);
                    bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                    bufferBuilder.restoreState(state);
                    bufferBuilder.sortQuads(
                        (float) (vec3d.x - builtChunk.getOrigin().getX()),
                        (float) (vec3d.y - builtChunk.getOrigin().getY()),
                        (float) (vec3d.z - builtChunk.getOrigin().getZ())
                    );
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

                        ByteBuffer swapBuffer = ((IPatchedBlockBufferBuilderStorage) buffers).getContext().cachedByteBuffer.getWithCapacity(minCapacity, newCapacity);
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
