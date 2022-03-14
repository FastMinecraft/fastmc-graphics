package me.luna.fastmc.mixin.patch.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.mixin.IPatchedRenderLayer;
import me.luna.fastmc.mixin.IPatchedWorldRenderer;
import me.luna.fastmc.mixin.accessor.AccessorVertexBuffer;
import me.luna.fastmc.shared.util.*;
import me.luna.fastmc.shared.util.collection.ExtendedBitSet;
import me.luna.fastmc.shared.util.collection.FastObjectArrayList;
import me.luna.fastmc.util.AdaptersKt;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_QUADS;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements IPatchedWorldRenderer {
    @Shadow
    private ChunkBuilder chunkBuilder;

    @Shadow
    private boolean needsTerrainUpdate;

    @Shadow
    private Set<ChunkBuilder.BuiltChunk> chunksToRebuild;

    @Shadow
    @Final
    private ObjectList<WorldRenderer.ChunkInfo> visibleChunks;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private ClientWorld world;

    @Shadow
    private double lastTranslucentSortX;

    @Shadow
    private double lastTranslucentSortY;

    @Shadow
    private double lastTranslucentSortZ;

    @Shadow
    @Final
    private VertexFormat vertexFormat;

    private final DoubleBufferedCollection<FastObjectArrayList<BlockEntity>> renderTileEntityList = new DoubleBufferedCollection<>(new FastObjectArrayList<>(), FastObjectArrayList::clearFast);
    private final DoubleBufferedCollection<ExtendedBitSet> chunksToUpdateBitSet = new DoubleBufferedCollection<>(new ExtendedBitSet(), it -> {});
    private final DoubleBuffered<FastObjectArrayList<ChunkBuilder.BuiltChunk>[]> filteredRenderInfos = new DoubleBuffered<>(getArray(), getArray(), MixinWorldRenderer::clearArray);

    private final Matrix4f original = new Matrix4f();
    private final Matrix4f translated = new Matrix4f();

    private static FastObjectArrayList<ChunkBuilder.BuiltChunk>[] getArray() {
        int size = RenderLayer.getBlockLayers().size();
        ArrayList<FastObjectArrayList<ChunkBuilder.BuiltChunk>> list = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            list.add(new FastObjectArrayList<>());
        }

        //noinspection unchecked
        return (FastObjectArrayList<ChunkBuilder.BuiltChunk>[]) list.toArray(new FastObjectArrayList[size]);
    }

    private static void clearArray(FastObjectArrayList<ChunkBuilder.BuiltChunk>[] array) {
        for (int i = 0; i < RenderLayer.getBlockLayers().size(); i++) {
            array[i].clearFast();
        }
    }

    private static void clearAndTrimArray(FastObjectArrayList<ChunkBuilder.BuiltChunk>[] array) {
        for (int i = 0; i < RenderLayer.getBlockLayers().size(); i++) {
            array[i].clearAndTrim();
        }
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    public void setWorld$Inject$HEAD(@Nullable ClientWorld world, CallbackInfo ci) {
        if (world == null) {
            renderTileEntityList.get().clearAndTrim();
            renderTileEntityList.swapAndGet().clearAndTrim();

            clearAndTrimArray(filteredRenderInfos.get());
            clearAndTrimArray(filteredRenderInfos.swapAndGet());

            chunksToUpdateBitSet.get().clearFast();
            chunksToUpdateBitSet.swapAndGet().clearFast();
        }
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z", ordinal = 0, remap = false))
    private boolean setupTerrain$Redirect$INVOKE$isEmpty$0(Set<ChunkBuilder.BuiltChunk> instance) {
        return true;
    }

    @Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", shift = At.Shift.AFTER, ordinal = 3), cancellable = true)
    private void setupTerrain$Inject$INVOKE$swap$3(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator, CallbackInfo ci) {
        ci.cancel();

        ExtendedBitSet oldSet = chunksToUpdateBitSet.get();
        ExtendedBitSet newSet = chunksToUpdateBitSet.swapAndGet();
        FastObjectArrayList<ChunkBuilder.BuiltChunk> list = new FastObjectArrayList<>();

        for (WorldRenderer.ChunkInfo renderInfo : this.visibleChunks) {
            ChunkBuilder.BuiltChunk builtChunk = renderInfo.chunk;
            int index = ((IPatchedBuiltChunk) builtChunk).getIndex();

            if (builtChunk.needsRebuild() || oldSet.contains(index)) {
                if (newSet.add(index)) {
                    list.add(builtChunk);
                }
            }
        }

        for (ChunkBuilder.BuiltChunk builtChunk : this.chunksToRebuild) {
            int index = ((IPatchedBuiltChunk) builtChunk).getIndex();
            if (newSet.add(index)) {
                list.add(builtChunk);
            }
        }

        oldSet.clear();
        list.trim();

        this.chunksToRebuild = new ObjectArraySet<>(list.elements());
        this.client.getProfiler().pop();
    }

    /**
     * @author Luna
     * @reason Chunk update optimization
     */
    @Overwrite
    private void updateChunks(long limitTime) {
        this.needsTerrainUpdate |= this.chunkBuilder.upload();

        long start = System.nanoTime();
        int count = 0;

        if (!this.chunksToRebuild.isEmpty()) {
            int countAsync = ParallelUtils.CPU_THREADS;
            boolean finish = false;

            Iterator<ChunkBuilder.BuiltChunk> iterator = this.chunksToRebuild.iterator();
            ExtendedBitSet bitSet = chunksToUpdateBitSet.get();

            while (iterator.hasNext() && (countAsync > 0 || !finish)) {
                ChunkBuilder.BuiltChunk builtChunk = iterator.next();
                boolean updated = false;

                if (builtChunk.needsImportantRebuild()) {
                    if (!finish) {
                        updated = true;
                        this.chunkBuilder.rebuild(builtChunk);
                        long current = System.nanoTime();
                        long durationPerChunk = (current - start) / (long) ++count;
                        long remaining = limitTime - current;
                        finish = remaining < durationPerChunk;
                    }
                } else if (--countAsync > 0) {
                    updated = true;
                    builtChunk.scheduleRebuild(this.chunkBuilder);
                }

                if (!updated) {
                    continue;
                }

                builtChunk.cancelRebuild();
                iterator.remove();
                bitSet.remove(((IPatchedBuiltChunk) builtChunk).getIndex());
            }
        }
    }

    @ModifyArg(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectList;add(Ljava/lang/Object;)Z", remap = false), index = 0)
    private Object setupTerrain$Inject$INVOKE$add$1(Object value) {
        WorldRenderer.ChunkInfo renderInfo = (WorldRenderer.ChunkInfo) value;
        ChunkBuilder.BuiltChunk builtChunk = renderInfo.chunk;
        ChunkBuilder.ChunkData chunkData = builtChunk.getData();
        List<BlockEntity> list = chunkData.getBlockEntities();

        if (!list.isEmpty()) {
            FastObjectArrayList<BlockEntity> mainList = renderTileEntityList.get();

            if (list instanceof ObjectArrayList<?>) {
                mainList.addAll(((FastObjectArrayList<BlockEntity>) list));
            } else {
                mainList.addAll(list);
            }
        }

        FastObjectArrayList<ChunkBuilder.BuiltChunk>[] array = filteredRenderInfos.get();

        for (int i = 0; i < RenderLayer.getBlockLayers().size(); i++) {
            RenderLayer layer = RenderLayer.getBlockLayers().get(i);
            FastObjectArrayList<ChunkBuilder.BuiltChunk> renderInfoList = array[i];
            if (!chunkData.isEmpty(layer)) {
                renderInfoList.add(builtChunk);
            }
        }

        return value;
    }

    @Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectList;clear()V", remap = false))
    private void setupTerrain$Inject$HEAD(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator, CallbackInfo ci) {
        renderTileEntityList.getAndSwap();
        int entityCapacity = world.blockEntities.size();
        entityCapacity = MathUtils.ceilToPOT(entityCapacity + entityCapacity / 2);

        FastObjectArrayList<BlockEntity> renderTileEntityList = this.renderTileEntityList.get();

        if (renderTileEntityList.getCapacity() > entityCapacity << 1) {
            renderTileEntityList.trim(entityCapacity);
        } else {
            renderTileEntityList.ensureCapacity(entityCapacity);
        }

        filteredRenderInfos.getAndSwap();
        FastObjectArrayList<ChunkBuilder.BuiltChunk>[] array = filteredRenderInfos.get();
        int renderInfoSize = this.visibleChunks.size();

        for (int i = 0; i < RenderLayer.getBlockLayers().size(); i++) {
            FastObjectArrayList<ChunkBuilder.BuiltChunk> list = array[i];

            if (list.getCapacity() > renderInfoSize + renderInfoSize / 2) {
                renderTileEntityList.trim(renderInfoSize);
            } else {
                list.ensureCapacity(renderInfoSize);
            }
        }
    }

    @NotNull
    @Override
    public List<BlockEntity> getRenderTileEntityList() {
        return renderTileEntityList.get();
    }

    /**
     * @author Luna
     * @reason Batch optimization
     */
    @SuppressWarnings("deprecation")
    @Overwrite
    private void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double renderPosX, double renderPosY, double renderPosZ) {
        renderLayer.startDrawing();
        if (renderLayer == RenderLayer.getTranslucent()) {
            this.client.getProfiler().push("translucentSort");
            double xDiff = renderPosX - this.lastTranslucentSortX;
            double yDiff = renderPosY - this.lastTranslucentSortY;
            double zDiff = renderPosZ - this.lastTranslucentSortZ;
            if (xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 1.0D) {
                this.lastTranslucentSortX = renderPosX;
                this.lastTranslucentSortY = renderPosY;
                this.lastTranslucentSortZ = renderPosZ;
                int count = 0;

                for (WorldRenderer.ChunkInfo chunkInfo : this.visibleChunks) {
                    if (count >= 16) break;
                    if (chunkInfo.chunk.scheduleSort(renderLayer, this.chunkBuilder)) {
                        ++count;
                    }
                }
            }

            this.client.getProfiler().pop();
        }

        this.client.getProfiler().push(renderLayer.toString());
        FastObjectArrayList<ChunkBuilder.BuiltChunk> list = filteredRenderInfos.get()[((IPatchedRenderLayer) renderLayer).getIndex()];

        AdaptersKt.toJoml(matrixStack.peek().getModel(), original);
        RenderSystem.pushMatrix();

        if (renderLayer != RenderLayer.getTranslucent()) {
            for (int i = 0; i < list.size(); i++) {
                ChunkBuilder.BuiltChunk builtChunk = list.get(i);
                VertexBuffer vertexBuffer = builtChunk.getBuffer(renderLayer);

                RenderSystem.loadIdentity();
                BlockPos blockPos = builtChunk.getOrigin();
                original.translate((float) (blockPos.getX() - renderPosX), (float) (blockPos.getY() - renderPosY), (float) (blockPos.getZ() - renderPosZ), translated);
                MatrixUtils.INSTANCE.putMatrix(translated);
                GlStateManager.multMatrix(MatrixUtils.INSTANCE.getMatrixBuffer());

                vertexBuffer.bind();
                this.vertexFormat.startDrawing(0L);
                RenderSystem.drawArrays(GL_QUADS, 0, ((AccessorVertexBuffer) vertexBuffer).getVertexCount());
            }
        } else {
            for (int i = list.size() - 1; i >= 0; i--) {
                ChunkBuilder.BuiltChunk builtChunk = list.get(i);
                VertexBuffer vertexBuffer = builtChunk.getBuffer(renderLayer);

                RenderSystem.loadIdentity();
                BlockPos blockPos = builtChunk.getOrigin();
                original.translate((float) (blockPos.getX() - renderPosX), (float) (blockPos.getY() - renderPosY), (float) (blockPos.getZ() - renderPosZ), translated);
                MatrixUtils.INSTANCE.putMatrix(translated);
                GlStateManager.multMatrix(MatrixUtils.INSTANCE.getMatrixBuffer());

                vertexBuffer.bind();
                this.vertexFormat.startDrawing(0L);
                RenderSystem.drawArrays(GL_QUADS, 0, ((AccessorVertexBuffer) vertexBuffer).getVertexCount());
            }
        }

        RenderSystem.popMatrix();

        VertexBuffer.unbind();
        RenderSystem.clearCurrentColor();
        this.vertexFormat.endDrawing();
        this.client.getProfiler().pop();
        renderLayer.endDrawing();
    }
}
