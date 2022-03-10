package me.luna.fastmc.mixin.patch.render;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.channels.Channel;
import kotlinx.coroutines.channels.SendChannel;
import me.luna.fastmc.mixin.IPatchedRenderGlobal;
import me.luna.fastmc.mixin.IPatchedVisGraph;
import me.luna.fastmc.shared.util.DoubleBufferedCollection;
import me.luna.fastmc.util.ExtensionsKt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal implements IPatchedRenderGlobal {

    @Shadow
    private WorldClient world;

    @Shadow
    @Nullable
    protected abstract RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing);

    @Shadow
    private List<RenderGlobal.ContainerLocalRenderInformation> renderInfos;
    private final DoubleBufferedCollection<List<TileEntity>> renderTileEntities = new DoubleBufferedCollection<>(new ArrayList<>());

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

    @Override
    public void iterationRecursive0(@NotNull RenderGlobal thisRef, @NotNull CoroutineScope scope, @NotNull SendChannel<? super RenderGlobal.ContainerLocalRenderInformation> actor, @NotNull AtomicInteger counts, @NotNull BlockPos playerPos, boolean flag, int frameCount, @NotNull ICamera camera, @NotNull List<RenderGlobal.ContainerLocalRenderInformation> renderInfos, @NotNull RenderGlobal.ContainerLocalRenderInformation info) {
        RenderChunk renderChunk = ExtensionsKt.getRenderChunk(info);
        EnumFacing facing = ExtensionsKt.getFacing(info);

        for (EnumFacing nextFacing : EnumFacing.values()) {
            RenderChunk nextRenderChunk = this.getRenderChunkOffset(playerPos, renderChunk, nextFacing);

            if ((!flag || !info.hasDirection(nextFacing.getOpposite()))
                && (!flag || facing == null || renderChunk.getCompiledChunk().isVisible(facing.getOpposite(), nextFacing))
                && nextRenderChunk != null && nextRenderChunk.setFrameIndex(frameCount) && camera.isBoundingBoxInFrustum(nextRenderChunk.boundingBox)) {
                RenderGlobal.ContainerLocalRenderInformation nextInfo = thisRef.new ContainerLocalRenderInformation(nextRenderChunk, nextFacing, ExtensionsKt.getCounter(info) + 1);
                nextInfo.setDirection(ExtensionsKt.getSetFacing(info), nextFacing);
                iterationRecursive(thisRef, scope, actor, counts, playerPos, flag, frameCount, camera, renderInfos, nextInfo);
            }
        }
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;drawBatch(I)V", remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
    private void renderEntities$Inject$INVOKE$drawBatch(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci, int pass) {
        for (TileEntity tileEntity : renderTileEntities.get()) {
            if (!tileEntity.shouldRenderInPass(pass) || !camera.isBoundingBoxInFrustum(tileEntity.getRenderBoundingBox()))
                continue;
            TileEntityRendererDispatcher.instance.render(tileEntity, partialTicks, -1);
        }
    }

    @Redirect(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;", opcode = Opcodes.GETFIELD, ordinal = 1))
    private List<RenderGlobal.ContainerLocalRenderInformation> renderEntities$Redirect$INVOKE$FIELD$renderInfos$GETFIELD(RenderGlobal instance) {
        return Collections.emptyList();
    }

    @Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false))
    private void setupTerrain$Inject$HEAD(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        renderTileEntities.swap();
    }

    @ModifyArg(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false), index = 0)
    private Object setupTerrain$Inject$INVOKE$add$1(Object value) {
        List<TileEntity> list = ExtensionsKt.getRenderChunk((RenderGlobal.ContainerLocalRenderInformation) value).getCompiledChunk().getTileEntities();
        if (!list.isEmpty()) {
            renderTileEntities.get().addAll(list);
        }
        return value;
    }
}
