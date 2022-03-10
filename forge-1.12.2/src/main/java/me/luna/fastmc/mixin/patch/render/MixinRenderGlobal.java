package me.luna.fastmc.mixin.patch.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.luna.fastmc.mixin.IPatchedRenderGlobal;
import me.luna.fastmc.mixin.IPatchedVisGraph;
import me.luna.fastmc.shared.util.DoubleBufferedCollection;
import me.luna.fastmc.shared.util.MathUtils;
import me.luna.fastmc.shared.util.collection.FastObjectArrayList;
import me.luna.fastmc.util.ExtensionsKt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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
    @Nullable
    protected abstract RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing);

    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    private int countEntitiesRendered;

    @Shadow
    protected abstract boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera);

    private final DoubleBufferedCollection<FastObjectArrayList<TileEntity>> renderTileEntityList = new DoubleBufferedCollection<>(new FastObjectArrayList<>(), FastObjectArrayList::clearFast);
    private final DoubleBufferedCollection<FastObjectArrayList<Entity>> renderEntityList = new DoubleBufferedCollection<>(new FastObjectArrayList<>(), FastObjectArrayList::clearFast);

    @Nullable
    @Override
    public RenderChunk getRenderChunkOffset0(@Nullable BlockPos playerPos, @Nullable RenderChunk renderChunkBase, @Nullable EnumFacing facing) {
        return getRenderChunkOffset(playerPos, renderChunkBase, facing);
    }

    @NotNull
    @Override
    public DoubleBufferedCollection<FastObjectArrayList<Entity>> getRenderEntityList() {
        return renderEntityList;
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

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", shift = At.Shift.AFTER, ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    private void setupTerrain$Redirect$INVOKE$newArrayDeque(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci, double d0, double d1, double d2, double d3, double d4, double d5, BlockPos blockpos1, RenderChunk renderchunk, BlockPos blockpos, boolean flag, Queue<RenderGlobal.ContainerLocalRenderInformation> queue, boolean flag1) {
        iterationParallel((RenderGlobal) (Object) this, blockpos, flag1, frameCount, camera, this.renderInfos, queue);
        queue.clear();
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
        int targetCapacity = world.loadedTileEntityList.size();
        targetCapacity = MathUtils.ceilToPOT(targetCapacity + targetCapacity / 2);

        FastObjectArrayList<TileEntity> list = renderTileEntityList.get();

        if (list.getCapacity() > targetCapacity << 1) {
            list.trim(targetCapacity);
        } else {
            list.ensureCapacity(targetCapacity);
        }
    }

    @ModifyArg(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false), index = 0)
    private Object setupTerrain$Inject$INVOKE$add$1(Object value) {
        List<TileEntity> list = ExtensionsKt.getRenderChunk((RenderGlobal.ContainerLocalRenderInformation) value).getCompiledChunk().getTileEntities();
        if (!list.isEmpty()) {
            FastObjectArrayList<TileEntity> mainList = renderTileEntityList.get();

            if (list instanceof ObjectArrayList<?>) {
                mainList.addAll(((ObjectArrayList<TileEntity>) list));
            } else {
                mainList.addAll(list);
            }
        }
        return value;
    }

    @Redirect(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private List<RenderGlobal.ContainerLocalRenderInformation> renderEntities$Redirect$INVOKE$FIELD$renderInfos$GETFIELD$0(RenderGlobal instance) {
        return Collections.emptyList();
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "renderEntities", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;retain()Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void renderEntities$Inject$INVOKE$retain(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci, int pass, double d0, double d1, double d2, Entity entity, double d3, double d4, double d5, List<Entity> list, List<Entity> list1, List<Entity> list2, BlockPos.PooledMutableBlockPos mutableBlockPos) {
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
}
