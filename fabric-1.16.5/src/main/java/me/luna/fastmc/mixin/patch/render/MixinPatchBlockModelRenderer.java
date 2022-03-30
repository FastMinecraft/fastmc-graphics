package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBlockModelRenderer;
import me.luna.fastmc.terrain.ChunkBuilderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;
import java.util.List;
import java.util.Random;

@Mixin(BlockModelRenderer.class)
public abstract class MixinPatchBlockModelRenderer implements IPatchedBlockModelRenderer {
    @Shadow
    @Final
    private BlockColors colorMap;

    @Shadow
    protected abstract void getQuadDimensions(BlockRenderView world, BlockState state, BlockPos pos, int[] vertexData, Direction face, float[] box, BitSet flags);

    @Override
    public boolean render0(@NotNull ChunkBuilderContext context, @NotNull BlockRenderView world, @NotNull BakedModel model, BlockState state, @NotNull BlockPos pos, MatrixStack matrix, @NotNull VertexConsumer vertexConsumer, boolean cull, @NotNull Random random, long seed, int overlay) {
        boolean aoEnabled = MinecraftClient.isAmbientOcclusionEnabled() && state.getLuminance() == 0 && model.useAmbientOcclusion();
        Vec3d vec3d = state.getModelOffset(world, pos);
        matrix.translate(vec3d.x, vec3d.y, vec3d.z);

        try {
            return aoEnabled ? this.renderSmooth0(context, world, model, state, pos, matrix, vertexConsumer, cull, random, seed, overlay) :
                this.renderFlat0(context, world, model, state, pos, matrix, vertexConsumer, cull, random, seed, overlay);
        } catch (Throwable var17) {
            CrashReport crashReport = CrashReport.create(var17, "Tesselating block model");
            CrashReportSection crashReportSection = crashReport.addElement("Block model being tesselated");
            CrashReportSection.addBlockInfo(crashReportSection, pos, state);
            crashReportSection.add("Using AO", aoEnabled);
            throw new CrashException(crashReport);
        }
    }

    private boolean renderFlat0(ChunkBuilderContext context, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack buffer, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        boolean rendered = false;
        BitSet flags = context.flags;

        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; ++i) {
            Direction direction = directions[i];

            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random);
            if (!list.isEmpty() && (!cull || context.shouldDrawSide(state, world, pos, direction))) {
                int j = WorldRenderer.getLightmapCoordinates(world, state, pos.offset(direction));
                this.renderQuadsFlat0(context, world, state, pos, j, overlay, false, buffer, vertexConsumer, list, flags);
                rendered = true;
            }
        }

        random.setSeed(seed);
        List<BakedQuad> list = model.getQuads(state, null, random);
        if (!list.isEmpty()) {
            this.renderQuadsFlat0(context, world, state, pos, -1, overlay, true, buffer, vertexConsumer, list, flags);
            rendered = true;
        }

        return rendered;
    }

    private boolean renderSmooth0(ChunkBuilderContext context, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack buffer, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        boolean rendered = false;
        BitSet flags = context.flags;
        float[] box = context.boxDimensionArray;

        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; ++i) {
            Direction direction = directions[i];

            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random);
            if (!list.isEmpty() && (!cull || context.shouldDrawSide(state, world, pos, direction))) {
                this.renderQuadsSmooth0(context, world, state, pos, buffer, vertexConsumer, list, box, flags, overlay);
                rendered = true;
            }
        }

        random.setSeed(seed);
        List<BakedQuad> list = model.getQuads(state, null, random);
        if (!list.isEmpty()) {
            this.renderQuadsSmooth0(context, world, state, pos, buffer, vertexConsumer, list, box, flags, overlay);
            rendered = true;
        }

        return rendered;
    }

    private void renderQuadsSmooth0(ChunkBuilderContext context, BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrix, VertexConsumer vertexConsumer, List<BakedQuad> quads, float[] box, BitSet flags, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedQuad = quads.get(i);
            this.getQuadDimensions(world, state, pos, bakedQuad.getVertexData(), bakedQuad.getFace(), box, flags);
            context.calculateAO(world, state, pos, bakedQuad.getFace(), box, flags, bakedQuad.hasShade());

            float[] brightnessArray = context.brightnessArray;
            int[] lightArray = context.lightArray;

            this.renderQuad0(
                world,
                state,
                pos,
                vertexConsumer,
                matrix.peek(),
                bakedQuad,
                brightnessArray,
                lightArray,
                overlay
            );
        }
    }

    private void renderQuadsFlat0(ChunkBuilderContext context, BlockRenderView world, BlockState state, BlockPos pos, int light, int overlay, boolean useWorldLight, MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, BitSet flags) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedQuad = quads.get(i);
            if (useWorldLight) {
                this.getQuadDimensions(world, state, pos, bakedQuad.getVertexData(), bakedQuad.getFace(), null, flags);
                BlockPos blockPos = flags.get(0) ? pos.offset(bakedQuad.getFace()) : pos;
                light = WorldRenderer.getLightmapCoordinates(world, state, blockPos);
            }

            float brightness = world.getBrightness(bakedQuad.getFace(), bakedQuad.hasShade());
            float[] brightnessArray = context.brightnessArray;
            int[] lightArray = context.lightArray;

            brightnessArray[0] = brightness;
            brightnessArray[1] = brightness;
            brightnessArray[2] = brightness;
            brightnessArray[3] = brightness;

            lightArray[0] = light;
            lightArray[1] = light;
            lightArray[2] = light;
            lightArray[3] = light;

            this.renderQuad0(world, state, pos, vertexConsumer, matrices.peek(), bakedQuad, brightnessArray, lightArray, overlay);
        }
    }

    private void renderQuad0(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, MatrixStack.Entry matrixEntry, BakedQuad quad, float[] brightness, int[] light, int overlay) {
        float r;
        float g;
        float b;

        if (quad.hasColor()) {
            int color = this.colorMap.getColor(state, world, pos, quad.getColorIndex());
            r = (float) (color >> 16 & 255) / 255.0F;
            g = (float) (color >> 8 & 255) / 255.0F;
            b = (float) (color & 255) / 255.0F;
        } else {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

        vertexConsumer.quad(matrixEntry, quad, brightness, r, g, b, light, overlay, true);
    }
}
