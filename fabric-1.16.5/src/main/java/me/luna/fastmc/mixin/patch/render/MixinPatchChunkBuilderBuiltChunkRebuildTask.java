package me.luna.fastmc.mixin.patch.render;

import com.mojang.datafixers.util.Pair;
import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.mixin.IPatchedChunkData;
import me.luna.fastmc.mixin.IPatchedTask;
import me.luna.fastmc.mixin.accessor.*;
import me.luna.fastmc.renderer.TileEntityRenderer;
import me.luna.fastmc.shared.util.BufferUtils;
import me.luna.fastmc.terrain.ChunkVertexData;
import me.luna.fastmc.terrain.RegionBuiltChunkStorage;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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

            if (cancelled0.get()) {
                return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
            } else {
                ChunkBuilder chunkBuilder = getChunkBuilder();
                Vec3d vec3d = chunkBuilder.getCameraPosition();
                float f = (float) vec3d.x;
                float g = (float) vec3d.y;
                float h = (float) vec3d.z;
                ChunkBuilder.ChunkData newData = new ChunkBuilder.ChunkData();
                Set<BlockEntity> set = this.render(f, g, h, newData, buffers);
                ((AccessorBuiltChunk) builtChunk).callSetNoCullingBlockEntities(set);
                if (cancelled0.get()) {
                    return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
                } else {
                    List<RenderLayer> layers = RenderLayer.getBlockLayers();
                    Set<RenderLayer> initializedLayers = ((AccessorChunkData) newData).getInitializedLayers();

                    long builtOrigin = builtChunk.getOrigin().asLong();
                    ByteBuffer[] bufferArray = new ByteBuffer[layers.size()];
                    int[] vertexCountArray = new int[layers.size()];

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
                            vertexCountArray[i] = vertexCount;
                        }
                    }

                    if (!cancelled0.get()) {
                        AccessorChunkBuilder accessorChunkBuilder = (AccessorChunkBuilder) chunkBuilder;
                        List<BlockEntity> oldList = ((IPatchedChunkData) builtChunk.data.get()).getInstancingRenderTileEntities();
                        List<BlockEntity> newList = ((IPatchedChunkData) newData).getInstancingRenderTileEntities();

                        TileEntityRenderer renderer = ((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer());
                        boolean oldEmpty = oldList.isEmpty();
                        boolean newEmpty = newList.isEmpty();
                        List<BlockEntity> adding = Collections.emptyList();
                        List<BlockEntity> removing = Collections.emptyList();

                        if (!oldEmpty || !newEmpty) {

                            if (oldEmpty) {
                                adding = newList;
                            } else if (newEmpty) {
                                removing = oldList;
                            } else {
                                Set<BlockEntity> oldSet = new HashSet<>(oldList);
                                Set<BlockEntity> newSet = new HashSet<>(newList);

                                adding = new ArrayList<>();
                                removing = new ArrayList<>();

                                for (BlockEntity e : newList) {
                                    if (!oldSet.contains(e)) {
                                        adding.add(e);
                                    }
                                }

                                for (BlockEntity e : oldList) {
                                    if (!newSet.contains(e)) {
                                        removing.add(e);
                                    }
                                }
                            }
                        }

                        ChunkBuilder.ChunkData oldData = builtChunk.getData();
                        boolean cullingDirty = oldData.isEmpty()
                            || !((AccessorChunkOcclusionData) ((AccessorChunkData) oldData).getOcclusionGraph()).getVisibility()
                            .equals(((AccessorChunkOcclusionData) ((AccessorChunkData) newData).getOcclusionGraph()).getVisibility());
                        List<BlockEntity> finalAdding = adding;
                        List<BlockEntity> finalRemoving = removing;

                        accessorChunkBuilder.getUploadQueue().add(() -> {
                            if (!cancelled0.get()) {
                                ChunkVertexData[] chunkVertexDataArray = patchedBuiltChunk.getChunkVertexDataArray();

                                for (int i = 0; i < bufferArray.length; i++) {
                                    ByteBuffer newBuffer = bufferArray[i];
                                    if (newBuffer != null) {
                                        updateVertexData(
                                            patchedBuiltChunk.getIndex(),
                                            chunkVertexDataArray,
                                            i,
                                            builtOrigin,
                                            newBuffer,
                                            vertexCountArray[i]
                                        );
                                    }
                                }

                                renderer.updateEntities(finalAdding, finalRemoving);
                                builtChunk.data.set(newData);
                                ((IPatchedChunkData) newData).onComplete();
                                patchedBuiltChunk.getRegion().getDirty().set(true);
                                if (cullingDirty) {
                                    ((RegionBuiltChunkStorage) ((AccessorWorldRenderer) ((AccessorChunkBuilder) getChunkBuilder()).getWorldRenderer()).getChunks()).markCaveCullingDirty();
                                }
                            }
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
