package me.luna.fastmc.mixin.patch.render;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.*;
import me.luna.fastmc.mixin.accessor.*;
import me.luna.fastmc.renderer.TileEntityRenderer;
import me.luna.fastmc.terrain.ChunkBuilderContext;
import me.luna.fastmc.terrain.ChunkVertexData;
import me.luna.fastmc.terrain.RegionBuiltChunkStorage;
import me.luna.fastmc.terrain.VertexDataTransformer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL11.GL_QUADS;

@Mixin(ChunkBuilder.BuiltChunk.RebuildTask.class)
public abstract class MixinPatchChunkBuilderBuiltChunkRebuildTask implements IPatchedTask {

    @Shadow
    @Nullable
    protected ChunkRendererRegion region;

    @Shadow
    protected abstract <E extends BlockEntity> void addBlockEntity(ChunkBuilder.ChunkData data, Set<BlockEntity> blockEntities, E blockEntity);

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
                IPatchedBlockBufferBuilderStorage patchedBuffers = (IPatchedBlockBufferBuilderStorage) buffers;
                ChunkBuilder chunkBuilder = getChunkBuilder();
                Vec3d vec3d = chunkBuilder.getCameraPosition();

                ChunkBuilder.ChunkData newData = new ChunkBuilder.ChunkData();
                Set<BlockEntity> set = this.render(
                    patchedBuffers.getContext(),
                    buffers,
                    newData,
                    vec3d.x,
                    vec3d.y,
                    vec3d.z
                );

                ((AccessorBuiltChunk) builtChunk).callSetNoCullingBlockEntities(set);

                if (cancelled0.get()) {
                    return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
                } else {
                    List<RenderLayer> layers = RenderLayer.getBlockLayers();
                    Set<RenderLayer> initializedLayers = ((AccessorChunkData) newData).getInitializedLayers();

                    BlockPos chunkOrigin = builtChunk.getOrigin();
                    long builtOrigin = chunkOrigin.asLong();
                    ByteBuffer[] bufferArray = new ByteBuffer[layers.size()];
                    int[] vertexCountArray = new int[layers.size()];

                    for (int i = 0; i < layers.size(); i++) {
                        RenderLayer layer = layers.get(i);
                        if (initializedLayers.contains(layer)) {
                            Pair<BufferBuilder.DrawArrayParameters, ByteBuffer> bufferData = buffers.get(layer).popData();
                            int vertexCount = bufferData.getFirst().getCount();
                            if (vertexCount == 0) continue;

                            BlockPos regionOrigin = patchedBuiltChunk.getRegion().getOrigin();
                            float offsetX = (float) (chunkOrigin.getX() - regionOrigin.getX());
                            float offsetY = (float) (chunkOrigin.getY() - regionOrigin.getY());
                            float offsetZ = (float) (chunkOrigin.getZ() - regionOrigin.getZ());

                            int minCapacity = VertexDataTransformer.INSTANCE.transformedSize(vertexCount);
                            int newCapacity = (minCapacity + 2097151) >> 20 << 20;

                            ByteBuffer swapBuffer = patchedBuffers.getContext().cachedByteBuffer.getWithCapacity(minCapacity, newCapacity);
                            ByteBuffer buffer = bufferData.getSecond();

                            VertexDataTransformer.INSTANCE.transform(offsetX, offsetY, offsetZ, vertexCount, buffer, swapBuffer);
                            buffer.clear();
                            buffer.put(swapBuffer);
                            buffer.flip();

                            bufferArray[i] = buffer;
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

                        return scheduleUpload(() -> {
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
                                } else {
                                    clearVertexData(
                                        patchedBuiltChunk.getIndex(),
                                        chunkVertexDataArray,
                                        i,
                                        builtOrigin
                                    );
                                }
                            }

                            renderer.updateEntities(finalAdding, finalRemoving);
                            builtChunk.data.set(newData);
                            patchedBuiltChunk.getRegion().getDirty().set(true);
                            if (cullingDirty) {
                                ((RegionBuiltChunkStorage) ((AccessorWorldRenderer) (accessorChunkBuilder).getWorldRenderer()).getChunks()).markCaveCullingDirty();
                            }
                        });
                    } else {
                        return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
                    }
                }
            }
        }
    }

    private Set<BlockEntity> render(ChunkBuilderContext context, BlockBufferBuilderStorage buffers, ChunkBuilder.ChunkData data, double cameraX, double cameraY, double cameraZ) {
        AccessorChunkData accessorData = (AccessorChunkData) data;
        IPatchedChunkData patchedData = (IPatchedChunkData) data;

        ChunkBuilder.BuiltChunk builtChunk = getBuiltChunk();
        ChunkOcclusionDataBuilder chunkOcclusionDataBuilder = new ChunkOcclusionDataBuilder();
        Set<BlockEntity> set = Sets.newHashSet();
        ChunkRendererRegion chunkRendererRegion = this.region;
        this.region = null;

        if (chunkRendererRegion != null) {
            MatrixStack matrixStack = context.matrixStack;
            while (!matrixStack.isEmpty()) {
                matrixStack.pop();
            }

            context.brightnessCache.enable();

            Set<RenderLayer> initializedLayers = accessorData.getInitializedLayers();
            Set<RenderLayer> nonEmptyLayers = accessorData.getNonEmptyLayers();

            Random random = new Random();
            BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
            IPatchedBlockRenderManager patchedBlockRenderManager = (IPatchedBlockRenderManager) blockRenderManager;

            BlockPos origin = builtChunk.getOrigin();
            int startX = origin.getX();
            int startY = origin.getY();
            int startZ = origin.getZ();
            int endX = startX + 16;
            int endY = startY + 16;
            int endZ = startZ + 16;

            BlockPos.Mutable blockPos = context.mutableBlockPosPool.get();
            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    for (int z = startZ; z < endZ; z++) {
                        blockPos.set(x, y, z);
                        BlockState blockState = chunkRendererRegion.getBlockState(blockPos);
                        Block block = blockState.getBlock();

                        if (blockState.isOpaqueFullCube(chunkRendererRegion, blockPos)) {
                            chunkOcclusionDataBuilder.markClosed(blockPos);
                        }

                        if (block.hasBlockEntity()) {
                            BlockEntity blockEntity = chunkRendererRegion.getBlockEntity(blockPos, WorldChunk.CreationType.CHECK);
                            if (blockEntity != null) {
                                this.addBlockEntity(data, set, blockEntity);
                            }
                        }

                        FluidState fluidState = chunkRendererRegion.getFluidState(blockPos);

                        if (!fluidState.isEmpty()) {
                            RenderLayer layer = RenderLayers.getFluidLayer(fluidState);
                            BufferBuilder bufferBuilder = buffers.get(layer);
                            if (initializedLayers.add(layer)) {
                                bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                            }

                            if (blockRenderManager.renderFluid(blockPos, chunkRendererRegion, bufferBuilder, fluidState)) {
                                accessorData.setEmpty(false);
                                nonEmptyLayers.add(layer);
                            }
                        }

                        if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
                            RenderLayer layer = RenderLayers.getBlockLayer(blockState);
                            BufferBuilder bufferBuilder = buffers.get(layer);
                            if (initializedLayers.add(layer)) {
                                bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                            }

                            matrixStack.push();
                            matrixStack.translate(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
                            if (patchedBlockRenderManager.renderBlock0(context, blockState, blockPos, chunkRendererRegion, matrixStack, bufferBuilder, true, random)) {
                                accessorData.setEmpty(false);
                                nonEmptyLayers.add(layer);
                            }

                            matrixStack.pop();
                        }
                    }
                }
            }
            context.mutableBlockPosPool.put(blockPos);
            patchedData.onComplete();

            if (!data.isEmpty(RenderLayer.getTranslucent())) {
                BufferBuilder bufferBuilder = buffers.get(RenderLayer.getTranslucent());
                bufferBuilder.sortQuads(
                    (float) (cameraX - startX),
                    (float) (cameraY - startY),
                    (float) (cameraZ - startZ)
                );
                accessorData.setBufferState(bufferBuilder.popState());
            }

            for (RenderLayer layer : initializedLayers) {
                buffers.get(layer).end();
            }

            context.brightnessCache.disable();
        }

        accessorData.setOcclusionGraph(chunkOcclusionDataBuilder.build());

        return set;
    }
}
