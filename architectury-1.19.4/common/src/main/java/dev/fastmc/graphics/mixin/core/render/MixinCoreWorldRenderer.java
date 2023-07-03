package dev.fastmc.graphics.mixin.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.fastmc.common.collection.FastObjectArrayList;
import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.mixin.accessor.AccessorLightmapTextureManager;
import dev.fastmc.graphics.shared.mixin.ICoreWorldRenderer;
import dev.fastmc.graphics.shared.opengl.GLWrapperKt;
import dev.fastmc.graphics.shared.renderer.WorldRenderer;
import dev.fastmc.graphics.shared.terrain.RenderChunk;
import dev.fastmc.graphics.shared.terrain.TerrainRenderer;
import dev.fastmc.graphics.shared.terrain.TerrainShaderManager;
import dev.fastmc.graphics.util.AdaptersKt;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.SortedSet;

import static dev.fastmc.graphics.shared.opengl.GLWrapperKt.*;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;

@SuppressWarnings("deprecation")
@Mixin(value = net.minecraft.client.render.WorldRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinCoreWorldRenderer implements ICoreWorldRenderer {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private ClientWorld world;
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;
    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;
    @Shadow
    private boolean cloudsDirty;
    @Shadow
    private int viewDistance;
    @Shadow
    private BuiltChunkStorage chunks;
    @Shadow
    private ChunkBuilder chunkBuilder;
    @Mutable
    @Shadow
    @Final
    private Set<BlockEntity> noCullingBlockEntities;
    @Mutable
    @Shadow
    @Final
    private ObjectArrayList<Object> chunkInfos;
    @Shadow
    @Final
    private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    @Shadow private @Nullable Framebuffer translucentFramebuffer;

    @Shadow private @Nullable Framebuffer weatherFramebuffer;

    @Shadow public abstract void reloadTransparencyPostProcessor();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Inject$init$RETURN(
        MinecraftClient client,
        EntityRenderDispatcher entityRenderDispatcher,
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        BufferBuilderStorage bufferBuilders,
        CallbackInfo ci
    ) {
        this.chunks = null;
        this.chunkBuilder = null;
        this.noCullingBlockEntities = ObjectSets.emptySet();
        this.chunkInfos = new ObjectArrayList<>(0);
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    public void setWorld$Inject$RETURN(@Nullable ClientWorld world, CallbackInfo ci) {
        if (world == null) {
            getTerrainRenderer().clear();
        }
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public void reload() {
        if (this.world != null) {
            reloadTransparencyPostProcessor();

            RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());

            this.cloudsDirty = true;
            this.viewDistance = this.client.options.getViewDistance().getValue();
            this.world.reloadColor();

            TerrainRenderer terrainRenderer = getTerrainRenderer();
            terrainRenderer.clear();
            terrainRenderer.updateChunkStorage(this.client.options.getViewDistance().getValue());
            terrainRenderer.reload();
        }
    }

    /**
     * @author Luna
     * @reason Chunk update optimization
     */
    @Overwrite
    public boolean isTerrainRenderComplete() {
        return getTerrainRenderer().getChunkBuilder().getTotalTaskCount() == 0;
    }

    /**
     * @author Luna
     * @reason Chunk update optimization
     */
    @Overwrite
    public boolean isRenderingReady(BlockPos pos) {
        return true;
    }

    /**
     * @author Luna
     * @reason Chunk update optimization
     */
    @Overwrite
    public int getCompletedChunkCount() {
        int count = 0;

        RenderChunk[] chunkArray = getTerrainRenderer().getChunkStorage().renderChunkArray;
        for (int i = 0; i < chunkArray.length; i++) {
            if (chunkArray[i].isBuilt) {
                count++;
            }
        }

        return count;
    }

    /**
     * @author Luna
     * @reason Chunk update optimization
     */
    @Overwrite
    private void scheduleChunkRender(int x, int y, int z, boolean important) {
        RenderChunk renderChunk = getTerrainRenderer().getChunkStorage().getRenderChunkByChunk(x, y, z);
        if (renderChunk != null) {
            renderChunk.isDirty = true;
        }
    }

    /**
     * @author Luna
     * @reason Debug
     */
    @Overwrite
    public String getChunksDebugString() {
        return getTerrainRenderer().getDebugInfoString();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;doLightUpdates(IZZ)I"))
    private int Redirect$render$INVOKE$doLightUpdates(
        LightingProvider instance,
        int i,
        boolean doSkylight,
        boolean skipEdgeLightPropagation
    ) {
        return 8964;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"))
    private void Inject$render$INVOKE$setupTerrain(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f projection,
        CallbackInfo ci
    ) {

        Vec3d cameraPos = camera.getPos();
        double renderPosX = cameraPos.getX();
        double renderPosY = cameraPos.getY();
        double renderPosZ = cameraPos.getZ();
        Matrix4f modelView = matrices.peek().getPositionMatrix();
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        worldRenderer.updateScreenSize(
            client.getWindow().getFramebufferWidth(),
            client.getWindow().getFramebufferHeight()
        );
        worldRenderer.updateMatrix(projection, modelView);
        worldRenderer.updateCameraPos(renderPosX, renderPosY, renderPosZ);
        worldRenderer.updateRenderPos(renderPosX, renderPosY, renderPosZ);
        worldRenderer.updateCameraRotation(camera.getYaw(), camera.getPitch());
        worldRenderer.updateFrustum();
        worldRenderer.updateGlobalUBO(tickDelta);
        worldRenderer.getTerrainRenderer().update(true);
    }

    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    private void Inject$setupTerrain$HEAD(
        Camera camera,
        Frustum frustum,
        boolean hasForcedFrustum,
        boolean spectator,
        CallbackInfo ci
    ) {
        ci.cancel();
    }

    @Inject(method = "updateChunks", at = @At("HEAD"), cancellable = true)
    private void Inject$updateChunks$HEAD(Camera camera, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true)
    private void Inject$renderLayer$HEAD(
        RenderLayer renderLayer,
        MatrixStack matrices,
        double d,
        double e,
        double f,
        Matrix4f positionMatrix,
        CallbackInfo ci
    ) {
        ci.cancel();

        if (renderLayer == RenderLayer.getSolid()) {
            renderLayerPass(0);
        } else if (renderLayer == RenderLayer.getTranslucent()) {
            renderLayerPass(1);
        } else if (renderLayer == RenderLayer.getTripwire()) {
            renderLayerPass(2);
        }
    }

    @Override
    public void preRenderLayer(int layerIndex) {
        ICoreWorldRenderer.super.preRenderLayer(layerIndex);
        switch (layerIndex) {
            case 0 -> preRenderSolid();
            case 1 -> {
                preRenderTranslucent();
                setupTranslucentFbo(this.translucentFramebuffer);
            }
            case 2 -> {
                preRenderTranslucent();
                setupTranslucentFbo(this.weatherFramebuffer);
            }
            default -> throw new IllegalArgumentException("Invalid layer index: " + layerIndex);
        }
    }

    private static void preRenderSolid() {
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GLWrapperKt.GL_LEQUAL);
    }

    private static void preRenderTranslucent() {
        glEnable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFuncSeparate(
            GL_SRC_ALPHA,
            GL_ONE_MINUS_SRC_ALPHA,
            GL_ONE,
            GL_ONE_MINUS_SRC_ALPHA
        );
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GLWrapperKt.GL_LEQUAL);
    }

    private static void setupTranslucentFbo(Framebuffer weather) {
        boolean usingFbo = MinecraftClient.isFabulousGraphicsOrBetter() && weather != null;
        if (usingFbo) {
            weather.beginWrite(false);
        } else {
            MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        }
    }

//    private boolean renderEntity(
//        MatrixStack matrices,
//        float tickDelta,
//        Camera camera,
//        double renderPosX,
//        double renderPosY,
//        double renderPosZ,
//        Frustum frustum,
//        VertexConsumerProvider.Immediate immediate
//    ) {
//        boolean entityRendered = false;
//
//        for (Entity entity : this.world.getEntities()) {
//            if (entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity) continue;
//            if (entity == camera.getFocusedEntity() && !camera.isThirdPerson()
//                && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity) camera.getFocusedEntity()).isSleeping()))
//                continue;
//            if (!this.entityRenderDispatcher.shouldRender(entity, frustum, renderPosX, renderPosY, renderPosZ)
//                && !entity.hasPassengerDeep(this.client.player)) continue;
//
//            ++this.regularEntityCount;
//            if (entity.age == 0) {
//                entity.lastRenderX = entity.getX();
//                entity.lastRenderY = entity.getY();
//                entity.lastRenderZ = entity.getZ();
//            }
//
//            VertexConsumerProvider vertexConsumerProvider;
//            if (this.canDrawEntityOutlines() && this.client.hasOutline(entity)) {
//                entityRendered = true;
//                OutlineVertexConsumerProvider outlineVertexConsumerProvider = this.bufferBuilders.getOutlineVertexConsumers();
//                vertexConsumerProvider = outlineVertexConsumerProvider;
//                int k = entity.getTeamColorValue();
//                int t = k >> 16 & 255;
//                int u = k >> 8 & 255;
//                int w = k & 255;
//                outlineVertexConsumerProvider.setColor(t, u, w, 255);
//            } else {
//                vertexConsumerProvider = immediate;
//            }
//
//            this.renderEntity(entity, renderPosX, renderPosY, renderPosZ, tickDelta, matrices, vertexConsumerProvider);
//        }
//
//        immediate.drawCurrentLayer();
//        this.checkEmpty(matrices);
//        immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
//        immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
//        immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
//        immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
//
//        return entityRendered;
//    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V", ordinal = 1))
    private void Inject$render$INVOKE$checkEmpty$1(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f projection,
        CallbackInfo ci
    ) {
        Profiler profiler = this.world.getProfiler();
        profiler.push("vanilla");
        renderTileEntityVanilla(matrices, tickDelta, camera);
        profiler.swap("fastmc");
        renderTileEntityFastMc(tickDelta);
        profiler.pop();
    }

    @SuppressWarnings("unchecked")
    private void renderTileEntityVanilla(MatrixStack matrices, float tickDelta, Camera camera) {
        TerrainRenderer terrainRenderer = getTerrainRenderer();
        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        Vec3d cameraPos = camera.getPos();
        double renderPosX = cameraPos.getX();
        double renderPosY = cameraPos.getY();
        double renderPosZ = cameraPos.getZ();

        FastObjectArrayList<BlockEntity> renderTileEntityList = (FastObjectArrayList<BlockEntity>) (Object) terrainRenderer.getRenderTileEntityList().getFront();
        VertexConsumerProvider.Immediate effectVertexConsumers = this.bufferBuilders.getEffectVertexConsumers();

        for (int i = 0; i < renderTileEntityList.size(); i++) {
            BlockEntity blockEntity = renderTileEntityList.get(i);
            BlockPos pos = blockEntity.getPos();
            VertexConsumerProvider vertexConsumerProvider = immediate;

            matrices.push();
            matrices.translate(pos.getX() - renderPosX, pos.getY() - renderPosY, pos.getZ() - renderPosZ);

            SortedSet<BlockBreakingInfo> sortedSet = this.blockBreakingProgressions.get(pos.asLong());

            if (sortedSet != null && !sortedSet.isEmpty()) {
                int w = sortedSet.last().getStage();
                if (w >= 0) {
                    MatrixStack.Entry entry = matrices.peek();
                    VertexConsumer vertexConsumer = new OverlayVertexConsumer(
                        effectVertexConsumers.getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(w)),
                        entry.getPositionMatrix(),
                        entry.getNormalMatrix(),
                        1.0f
                    );
                    vertexConsumerProvider = renderLayer -> {
                        VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
                        if (renderLayer.hasCrumbling()) {
                            return VertexConsumers.union(vertexConsumer, vertexConsumer2);
                        } else {
                            return vertexConsumer2;
                        }
                    };
                }
            }

            this.blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, vertexConsumerProvider);
            matrices.pop();
        }

        FastObjectArrayList<BlockEntity> globalTileEntityList = (FastObjectArrayList<BlockEntity>) (Object) terrainRenderer.getGlobalTileEntityList().getFront();
        for (int i = 0; i < globalTileEntityList.size(); i++) {
            BlockEntity blockEntity = globalTileEntityList.get(i);
            BlockPos pos = blockEntity.getPos();
            matrices.push();
            matrices.translate(pos.getX() - renderPosX, pos.getY() - renderPosY, pos.getZ() - renderPosZ);
            blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, immediate);
            matrices.pop();
        }
    }

    @Override
    public int getLightMapTexture() {
        LightmapTextureManager lightmapTextureManager = this.client.gameRenderer.getLightmapTextureManager();
        return getTexture((((AccessorLightmapTextureManager) lightmapTextureManager).getTextureIdentifier())).getGlId();
    }

    @Override
    public void bindBlockTexture() {
        AbstractTexture blockTexture = getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        blockTexture.bindTexture();
        blockTexture.setFilter(false, true);
    }

    private AbstractTexture getTexture(Identifier identifier) {
        AbstractTexture texture = client.getTextureManager().getTexture(identifier);
        if (texture == null) {
            texture = new ResourceTexture(identifier);
            client.getTextureManager().registerTexture(identifier, texture);
        }

        return texture;
    }
}