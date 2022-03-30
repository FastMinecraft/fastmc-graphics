package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBlockModelRenderer;
import me.luna.fastmc.mixin.IPatchedBlockRenderManager;
import me.luna.fastmc.terrain.ChunkBuilderContext;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(BlockRenderManager.class)
public abstract class MixinPatchBlockRenderManager implements IPatchedBlockRenderManager {
    @Shadow
    @Final
    private BlockModelRenderer blockModelRenderer;

    @Shadow
    public abstract BakedModel getModel(BlockState state);

    @Override
    public boolean renderBlock0(@NotNull ChunkBuilderContext context, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull BlockRenderView world, @NotNull MatrixStack matrix, @NotNull VertexConsumer vertexConsumer, boolean cull, @NotNull Random random) {
        try {
            return state.getRenderType() == BlockRenderType.MODEL && ((IPatchedBlockModelRenderer) this.blockModelRenderer).render0(
                context,
                world,
                this.getModel(state),
                state,
                pos,
                matrix,
                vertexConsumer,
                cull,
                random,
                state.getRenderingSeed(pos),
                OverlayTexture.DEFAULT_UV
            );
        } catch (Throwable var11) {
            CrashReport crashReport = CrashReport.create(var11, "Tesselating block in world");
            CrashReportSection crashReportSection = crashReport.addElement("Block being tesselated");
            CrashReportSection.addBlockInfo(crashReportSection, pos, state);
            throw new CrashException(crashReport);
        }
    }
}
