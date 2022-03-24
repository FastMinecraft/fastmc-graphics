package me.luna.fastmc.mixin.patch.render;

import com.mojang.datafixers.util.Pair;
import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.mixin.IPatchedChunkData;
import me.luna.fastmc.mixin.IPatchedTask;
import me.luna.fastmc.mixin.accessor.AccessorBuiltChunk;
import me.luna.fastmc.mixin.accessor.AccessorChunkBuilder;
import me.luna.fastmc.mixin.accessor.AccessorChunkData;
import me.luna.fastmc.shared.opengl.VertexBufferObject;
import me.luna.fastmc.shared.util.BufferUtils;
import me.luna.fastmc.terrain.ChunkVertexData;
import me.luna.fastmc.terrain.RenderRegion;
import me.luna.fastmc.terrain.VertexDataTransformer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL15.GL_STATIC_COPY;
import static org.lwjgl.opengl.GL45.glNamedBufferData;

@Mixin(ChunkBuilder.BuiltChunk.RebuildTask.class)
public abstract class MixinPatchChunkBuilderBuiltChunkRebuildTask implements IPatchedTask {
    @Shadow
    @Nullable
    protected ChunkRendererRegion region;

    @Shadow
    protected abstract Set<BlockEntity> render(float cameraX, float cameraY, float cameraZ, ChunkBuilder.ChunkData data, BlockBufferBuilderStorage buffers);

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public CompletableFuture<ChunkBuilder.Result> run(BlockBufferBuilderStorage buffers) {
        AtomicBoolean cancelled0 = getCancelled0();

        if (cancelled0.get()) {
            return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
        } else {
            ChunkBuilder.BuiltChunk builtChunk = getBuiltChunk();
            IPatchedBuiltChunk patchedBuiltChunk = (IPatchedBuiltChunk) builtChunk;

            if (!builtChunk.shouldBuild()) {
                this.region = null;
                builtChunk.scheduleRebuild(false);
                cancelled0.set(true);
                return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
            } else if (cancelled0.get()) {
                return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
            } else {
                ChunkBuilder chunkBuilder = getChunkBuilder();
                Vec3d vec3d = chunkBuilder.getCameraPosition();
                float f = (float) vec3d.x;
                float g = (float) vec3d.y;
                float h = (float) vec3d.z;
                ChunkBuilder.ChunkData chunkData = new ChunkBuilder.ChunkData();
                Set<BlockEntity> set = this.render(f, g, h, chunkData, buffers);
                ((AccessorBuiltChunk) builtChunk).callSetNoCullingBlockEntities(set);
                if (cancelled0.get()) {
                    return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
                } else {
                    List<RenderLayer> layers = RenderLayer.getBlockLayers();
                    Set<RenderLayer> initializedLayers = ((AccessorChunkData) chunkData).getInitializedLayers();
                    ByteBuffer[] bufferArray = new ByteBuffer[layers.size()];
                    long builtOrigin = builtChunk.getOrigin().asLong();

                    for (int i = 0; i < layers.size(); i++) {
                        RenderLayer layer = layers.get(i);
                        if (initializedLayers.contains(layer)) {
                            Pair<BufferBuilder.DrawArrayParameters, ByteBuffer> bufferData = buffers.get(layer).popData();
                            int vertexCount = bufferData.getFirst().getCount();
                            if (vertexCount == 0) continue;
                            ByteBuffer newBuffer = BufferUtils.allocateByte(VertexDataTransformer.INSTANCE.transformedSize(vertexCount));

                            BlockPos regionOrigin = patchedBuiltChunk.getRegion().getOrigin();
                            BlockPos chunkOrigin = builtChunk.getOrigin();
                            float offsetX = (float) (chunkOrigin.getX() - regionOrigin.getX());
                            float offsetY = (float) (chunkOrigin.getY() - regionOrigin.getY());
                            float offsetZ = (float) (chunkOrigin.getZ() - regionOrigin.getZ());

                            VertexDataTransformer.INSTANCE.transform(offsetX, offsetY, offsetZ, vertexCount, bufferData.getSecond(), newBuffer);
                            bufferArray[i] = newBuffer;
                        }
                    }

                    if (!cancelled0.get()) {
                        ((AccessorChunkBuilder) chunkBuilder).getUploadQueue().add(() -> {
                            ChunkVertexData[] chunkVertexDataArray = patchedBuiltChunk.getChunkVertexDataArray();

                            for (int i = 0; i < bufferArray.length; i++) {
                                ChunkVertexData data = chunkVertexDataArray[i];
                                ByteBuffer newBuffer = bufferArray[i];
                                if (newBuffer != null) {
                                    VertexBufferObject vbo = data != null ? data.getVbo() : new VertexBufferObject(RenderRegion.VERTEX_ATTRIBUTE);
                                    int size = newBuffer.remaining();
                                    glNamedBufferData(vbo.getId(), newBuffer, GL_STATIC_COPY);
                                    chunkVertexDataArray[i] = new ChunkVertexData(builtOrigin, size / VertexDataTransformer.INSTANCE.getVertexSize(), size, vbo);
                                }
                            }

                            patchedBuiltChunk.getRegion().setDirty(true);
                            builtChunk.data.set(chunkData);
                            ((IPatchedChunkData) builtChunk.getData()).onComplete();
                        });
                        return CompletableFuture.completedFuture(ChunkBuilder.Result.SUCCESSFUL);
                    } else {
                        return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
                    }
                }
            }
        }
    }
}
