package me.luna.fastmc.mixin.patch.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.luna.fastmc.mixin.IPatchedChunkRenderContainer;
import me.luna.fastmc.mixin.IPatchedRenderGlobal;
import me.luna.fastmc.mixin.IPatchedVisGraph;
import me.luna.fastmc.shared.util.DoubleBuffered;
import me.luna.fastmc.shared.util.DoubleBufferedCollection;
import me.luna.fastmc.shared.util.MathUtils;
import me.luna.fastmc.shared.util.collection.FastObjectArrayList;
import me.luna.fastmc.util.ExtensionsKt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal implements IPatchedRenderGlobal {

    @Shadow
    private WorldClient world;

    @Shadow
    private List<RenderGlobal.ContainerLocalRenderInformation> renderInfos;

    @Shadow
    @Final
    private RenderManager renderManager;

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private int countEntitiesRendered;

    @Shadow
    private ChunkRenderContainer renderContainer;

    @Shadow
    private double prevRenderSortX;

    @Shadow
    private double prevRenderSortY;

    @Shadow
    private double prevRenderSortZ;

    @Shadow
    private ChunkRenderDispatcher renderDispatcher;

    @Shadow
    private Set<RenderChunk> chunksToUpdate;

    @Shadow
    private boolean displayListEntitiesDirty;

    @Shadow
    protected abstract boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera);

    private final DoubleBufferedCollection<FastObjectArrayList<TileEntity>> renderTileEntityList = new DoubleBufferedCollection<>(new FastObjectArrayList<>(), FastObjectArrayList::clearFast);
    private final DoubleBufferedCollection<FastObjectArrayList<Entity>> renderEntityList = new DoubleBufferedCollection<>(new FastObjectArrayList<>(), FastObjectArrayList::clearFast);
    private final DoubleBuffered<FastObjectArrayList<RenderChunk>[]> filteredRenderInfos = new DoubleBuffered<>(getArray(), getArray(), it -> {
        for (int i = 0; i < BlockRenderLayer.values().length; i++) {
            it[i].clearFast();
        }
    });

    private static FastObjectArrayList<RenderChunk>[] getArray() {
        int size = BlockRenderLayer.values().length;
        ArrayList<FastObjectArrayList<RenderChunk>> list = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            list.add(new FastObjectArrayList<>());
        }

        //noinspection unchecked
        return (FastObjectArrayList<RenderChunk>[]) list.toArray(new FastObjectArrayList[size]);
    }

    /**
     * @author Luna
     * @reason Optimization
     */
    @Overwrite
    private Set<EnumFacing> getVisibleFacings(BlockPos pos) {
        VisGraph visgraph = new VisGraph();
        IPatchedVisGraph patchedVisGraph = (IPatchedVisGraph) visgraph;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        Chunk chunk = this.world.getChunk(chunkX, chunkZ);

        int x1 = chunkX << 4;
        int y1 = pos.getY() >> 4 << 4;
        int z1 = chunkZ << 4;

        int x2 = x1 + 16;
        int y2 = y1 + 16;
        int z2 = z1 + 16;

        for (; x1 < x2; x1++) {
            for (; y1 < y2; y1++) {
                for (; z1 < z2; z1++) {
                    IBlockState blockState = chunk.getBlockState(x1, y1, z1);
                    if (blockState.isOpaqueCube()) {
                        //noinspection ConstantConditions
                        patchedVisGraph.setOpaqueCube(x1 & 15, y1 & 15, z1 & 15);
                    }
                }
            }
        }

        return visgraph.getVisibleFacings(pos);
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z", ordinal = 0, remap = false))
    private boolean setupTerrain$Redirect$INVOKE$isEmpty$0(Set<RenderChunk> instance) {
        return true;
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", shift = At.Shift.AFTER, ordinal = 5), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void setupTerrain$Inject$INVOKE$endStartSection$5(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci, double d0, double d1, double d2, double d3, double d4, double d5, BlockPos blockpos1, RenderChunk renderchunk, BlockPos blockpos, boolean flag) {
        ci.cancel();

        Set<RenderChunk> oldSet = this.chunksToUpdate;
        Set<RenderChunk> newSet = new LinkedHashSet<>();

        for (RenderGlobal.ContainerLocalRenderInformation renderInfo : this.renderInfos) {
            RenderChunk renderChunk = ExtensionsKt.getRenderChunk(renderInfo);

            if (renderChunk.needsUpdate() || oldSet.contains(renderChunk)) {
                BlockPos blockPos = renderChunk.getPosition();
                boolean flag3 = blockpos1.distanceSq(blockPos.getX() + 8, blockPos.getY() + 8, blockPos.getZ() + 8) < 768.0D;

                if (net.minecraftforge.common.ForgeModContainer.alwaysSetupTerrainOffThread || (!renderChunk.needsImmediateUpdate() && !flag3)) {
                    newSet.add(renderChunk);
                } else {
                    this.mc.profiler.startSection("build near");
                    this.renderDispatcher.updateChunkNow(renderChunk);
                    renderChunk.clearNeedsUpdate();
                    this.displayListEntitiesDirty = true;
                    this.mc.profiler.endSection();
                }
            }
        }

        newSet.addAll(oldSet);
        this.chunksToUpdate = newSet;
        this.mc.profiler.endSection();
    }

    @Inject(method = "updateChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;updateChunkNow(Lnet/minecraft/client/renderer/chunk/RenderChunk;)Z", shift = At.Shift.AFTER))
    private void updateChunks$Inject$INVOKE$updateChunkNow(long finishTimeNano, CallbackInfo ci) {
        this.displayListEntitiesDirty = true;
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;drawBatch(I)V", remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
    private void renderEntities$Inject$INVOKE$drawBatch(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci, int pass) {
        for (TileEntity tileEntity : renderTileEntityList.get()) {
            if (!tileEntity.shouldRenderInPass(pass) || !camera.isBoundingBoxInFrustum(tileEntity.getRenderBoundingBox()))
                continue;
            TileEntityRendererDispatcher.instance.render(tileEntity, partialTicks, -1);
        }
    }

    @Redirect(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;", opcode = Opcodes.GETFIELD, ordinal = 1))
    private List<RenderGlobal.ContainerLocalRenderInformation> renderEntities$Redirect$INVOKE$FIELD$renderInfos$GETFIELD$1(RenderGlobal instance) {
        return Collections.emptyList();
    }

    @Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false))
    private void setupTerrain$Inject$HEAD(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        renderTileEntityList.swap();
        int entityCapacity = world.loadedTileEntityList.size();
        entityCapacity = MathUtils.ceilToPOT(entityCapacity + entityCapacity / 2);

        FastObjectArrayList<TileEntity> renderTileEntityList = this.renderTileEntityList.get();

        if (renderTileEntityList.getCapacity() > entityCapacity << 1) {
            renderTileEntityList.trim(entityCapacity);
        } else {
            renderTileEntityList.ensureCapacity(entityCapacity);
        }

        filteredRenderInfos.swap();
        FastObjectArrayList<RenderChunk>[] array = filteredRenderInfos.get();
        int renderInfoSize = this.renderInfos.size();

        for (int i = 0; i < BlockRenderLayer.values().length; i++) {
            FastObjectArrayList<RenderChunk> list = array[i];

            if (list.getCapacity() > renderInfoSize + renderInfoSize / 2) {
                renderTileEntityList.trim(renderInfoSize);
            } else {
                list.ensureCapacity(renderInfoSize);
            }
        }
    }

    @ModifyArg(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false), index = 0)
    private Object setupTerrain$Inject$INVOKE$add$1(Object value) {
        RenderGlobal.ContainerLocalRenderInformation renderInfo = (RenderGlobal.ContainerLocalRenderInformation) value;
        RenderChunk renderChunk = ExtensionsKt.getRenderChunk(renderInfo);
        CompiledChunk compiledChunk = renderChunk.getCompiledChunk();
        List<TileEntity> list = compiledChunk.getTileEntities();

        if (!list.isEmpty()) {
            FastObjectArrayList<TileEntity> mainList = renderTileEntityList.get();

            if (list instanceof ObjectArrayList<?>) {
                mainList.addAll(((ObjectArrayList<TileEntity>) list));
            } else {
                mainList.addAll(list);
            }
        }

        for (int i = 0; i < BlockRenderLayer.values().length; i++) {
            BlockRenderLayer layer = BlockRenderLayer.values()[i];
            if (!compiledChunk.isLayerEmpty(layer)) {
                filteredRenderInfos.get()[i].add(renderChunk);
            }
        }

        return value;
    }

    @Redirect(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private List<RenderGlobal.ContainerLocalRenderInformation> renderEntities$Redirect$INVOKE$FIELD$renderInfos$GETFIELD$0
        (RenderGlobal instance) {
        return Collections.emptyList();
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "renderEntities", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;retain()Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void renderEntities$Inject$INVOKE$retain(Entity renderViewEntity, ICamera camera,
                                                     float partialTicks, CallbackInfo ci, int pass, double d0, double d1, double d2, Entity entity, double d3,
                                                     double d4, double d5, List<
        Entity> list, List<Entity> list1, List<Entity> list2, BlockPos.PooledMutableBlockPos mutableBlockPos) {
        for (Entity it : this.getRenderEntityList().get()) {
            if (!it.shouldRenderInPass(pass)) continue;
            if (!it.isRidingOrBeingRiddenBy(renderViewEntity)
                && !this.renderManager.shouldRender(it, camera, d0, d1, d2))
                continue;

            ++this.countEntitiesRendered;
            this.renderManager.renderEntityStatic(it, partialTicks, false);

            if (this.isOutlineActive(it, it, camera)) {
                list1.add(it);
            }

            if (this.renderManager.isRenderMultipass(it)) {
                list2.add(it);
            }
        }
    }

    /**
     * @author Luna
     * @reason Lambda optimization
     */
    @Overwrite
    public int renderBlockLayer(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn) {
        RenderHelper.disableStandardItemLighting();

        if (blockLayerIn == BlockRenderLayer.TRANSLUCENT) {
            this.mc.profiler.startSection("translucent_sort");
            double d0 = entityIn.posX - this.prevRenderSortX;
            double d1 = entityIn.posY - this.prevRenderSortY;
            double d2 = entityIn.posZ - this.prevRenderSortZ;

            if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0D) {
                this.prevRenderSortX = entityIn.posX;
                this.prevRenderSortY = entityIn.posY;
                this.prevRenderSortZ = entityIn.posZ;
                int k = 0;

                for (RenderGlobal.ContainerLocalRenderInformation info : this.renderInfos) {
                    RenderChunk renderChunk = ExtensionsKt.getRenderChunk(info);
                    if (renderChunk.compiledChunk.isLayerStarted(blockLayerIn) && k++ < 15) {
                        this.renderDispatcher.updateTransparencyLater(renderChunk);
                    }
                }
            }

            this.mc.profiler.endSection();
        }

        FastObjectArrayList<RenderChunk> list = filteredRenderInfos.get()[blockLayerIn.ordinal()];

        this.mc.profiler.startSection(blockLayerIn.toString());
        this.renderBlockLayer(list, blockLayerIn);
        this.mc.profiler.endSection();

        return list.size();
    }

    private void renderBlockLayer(FastObjectArrayList<RenderChunk> list, BlockRenderLayer blockLayerIn) {
        this.mc.entityRenderer.enableLightmap();

        if (OpenGlHelper.useVbo()) {
            GlStateManager.glEnableClientState(32884);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.glEnableClientState(32888);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glEnableClientState(32888);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.glEnableClientState(32886);
        }

        ((IPatchedChunkRenderContainer) this.renderContainer).renderChunkLayer(list, blockLayerIn);

        if (OpenGlHelper.useVbo()) {
            for (VertexFormatElement vertexformatelement : DefaultVertexFormats.BLOCK.getElements()) {
                VertexFormatElement.EnumUsage usage = vertexformatelement.getUsage();
                int index = vertexformatelement.getIndex();

                switch (usage) {
                    case POSITION:
                        GlStateManager.glDisableClientState(32884);
                        break;
                    case UV:
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + index);
                        GlStateManager.glDisableClientState(32888);
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                        break;
                    case COLOR:
                        GlStateManager.glDisableClientState(32886);
                        GlStateManager.resetColor();
                }
            }
        }

        this.mc.entityRenderer.disableLightmap();
    }

    @NotNull
    @Override
    public DoubleBufferedCollection<FastObjectArrayList<Entity>> getRenderEntityList() {
        return renderEntityList;
    }
}
