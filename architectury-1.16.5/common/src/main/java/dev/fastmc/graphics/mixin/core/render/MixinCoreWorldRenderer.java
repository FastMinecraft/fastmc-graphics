package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.common.collection.FastObjectArrayList;
import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.mixin.accessor.AccessorBackgroundRenderer;
import dev.fastmc.graphics.mixin.accessor.AccessorLightmapTextureManager;
import dev.fastmc.graphics.shared.mixin.ICoreWorldRenderer;
import dev.fastmc.graphics.shared.renderer.WorldRenderer;
import dev.fastmc.graphics.shared.terrain.RenderChunk;
import dev.fastmc.graphics.shared.terrain.TerrainRenderer;
import dev.fastmc.graphics.shared.terrain.TerrainShaderManager;
import dev.fastmc.graphics.util.AdaptersKt;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.SortedSet;

import static dev.fastmc.graphics.shared.opengl.GLWrapperKt.*;

@SuppressWarnings("deprecation")
@Mixin(value = net.minecraft.client.render.WorldRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinCoreWorldRenderer implements ICoreWorldRenderer {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private ClientWorld world;
    @Shadow
    private @Nullable Framebuffer weatherFramebuffer;
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;
    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;
    @Shadow
    private @Nullable Framebuffer translucentFramebuffer;
    @Shadow
    @Final
    private TextureManager textureManager;
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
    @Shadow
    private Set<ChunkBuilder.BuiltChunk> chunksToRebuild;
    @Mutable
    @Shadow
    @Final
    private ObjectList<?> visibleChunks;

    @Shadow
    protected abstract void resetTransparencyShader();

    @Shadow
    protected abstract void loadTransparencyShader();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Inject$init$RETURN(MinecraftClient client, BufferBuilderStorage bufferBuilders, CallbackInfo ci) {
        this.chunks = null;
        this.chunkBuilder = null;
        this.noCullingBlockEntities = ObjectSets.emptySet();
        this.chunksToRebuild = ObjectSets.emptySet();
        this.visibleChunks = ObjectLists.emptyList();
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
        if (this.world == null) return;

        if (MinecraftClient.isFabulousGraphicsOrBetter()) {
            this.loadTransparencyShader();
        } else {
            this.resetTransparencyShader();
        }

        this.world.reloadColor();

        this.cloudsDirty = true;
        RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());
        this.viewDistance = this.client.options.viewDistance;

        TerrainRenderer terrainRenderer = getTerrainRenderer();
        terrainRenderer.clear();
        terrainRenderer.updateChunkStorage(this.client.options.viewDistance);
        terrainRenderer.reload();
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

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZIZ)V"))
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
        Matrix4f modelView = matrices.peek().getModel();
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        worldRenderer.updateScreenSize(
            client.getWindow().getFramebufferWidth(),
            client.getWindow().getFramebufferHeight()
        );
        org.joml.Matrix4f projection1 = AdaptersKt.toJoml(projection);
        org.joml.Matrix4f modelView1 = AdaptersKt.toJoml(modelView);
        worldRenderer.updateMatrix(projection1, modelView1);
        worldRenderer.updateCameraPos(renderPosX, renderPosY, renderPosZ);
        worldRenderer.updateRenderPos(renderPosX, renderPosY, renderPosZ);
        worldRenderer.updateCameraRotation(camera.getYaw(), camera.getPitch());
        worldRenderer.updateFrustum();
        worldRenderer.updateGlobalUBO(tickDelta);
        worldRenderer.getTerrainRenderer().update(true);

        float viewDistance = gameRenderer.getViewDistance();
        assert this.client.world != null;
        boolean thickFog = this.client.world.getSkyProperties().useThickFog(
            MathHelper.floor(renderPosX),
            MathHelper.floor(renderPosY)
        ) || this.client.inGameHud.getBossBarHud().shouldThickenFog();
        float fogDistance = Math.max(viewDistance - 16.0F, 32.0F);
        setupTerrainFogShader(camera, fogDistance, thickFog);
    }

    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    private void Inject$setupTerrain$HEAD(
        Camera camera,
        Frustum frustum,
        boolean hasForcedFrustum,
        int frame,
        boolean spectator,
        CallbackInfo ci
    ) {
        ci.cancel();
    }

    @Inject(method = "updateChunks", at = @At("HEAD"), cancellable = true)
    private void Inject$updateChunks$HEAD(long limitTime, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true)
    private void Inject$renderLayer$HEAD(
        RenderLayer renderLayer,
        MatrixStack matrixStack,
        double d,
        double e,
        double f,
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
            case 0:
                preRenderSolid();
                break;
            case 1:
                preRenderTranslucent();
                setupTranslucentFbo(this.translucentFramebuffer);
                break;
            case 2:
                preRenderTranslucent();
                setupTranslucentFbo(this.weatherFramebuffer);
                break;
            default:
                throw new IllegalArgumentException("Invalid layer index: " + layerIndex);
        }
    }

    private static void preRenderSolid() {
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDisable(GL_ALPHA_TEST);
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
        glDepthFunc(GL_LEQUAL);
    }

    private static void setupTranslucentFbo(Framebuffer weather) {
        boolean usingFbo = MinecraftClient.isFabulousGraphicsOrBetter() && weather != null;
        if (usingFbo) {
            weather.beginWrite(false);
        } else {
            MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        }
    }

//    @SuppressWarnings("deprecation")
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
//            VertexConsumerProvider vertexConsumerProvider2;
//            if (this.canDrawEntityOutlines() && this.client.hasOutline(entity)) {
//                entityRendered = true;
//                OutlineVertexConsumerProvider outlineVertexConsumerProvider = this.bufferBuilders.getOutlineVertexConsumers();
//                vertexConsumerProvider2 = outlineVertexConsumerProvider;
//                int k = entity.getTeamColorValue();
//                int t = k >> 16 & 255;
//                int u = k >> 8 & 255;
//                int w = k & 255;
//                outlineVertexConsumerProvider.setColor(t, u, w, 255);
//            } else {
//                vertexConsumerProvider2 = immediate;
//            }
//
//            this.renderEntity(entity, renderPosX, renderPosY, renderPosZ, tickDelta, matrices, vertexConsumerProvider2);
//        }
//
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

        FastObjectArrayList<BlockEntity> renderTileEntityList = (FastObjectArrayList<BlockEntity>) (Object) terrainRenderer
            .getRenderTileEntityList().getFront();
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
                    MatrixStack.Entry blockEntityMatrixEntry = matrices.peek();
                    VertexConsumer vertexConsumer = new OverlayVertexConsumer(
                        this.bufferBuilders.getEffectVertexConsumers()
                            .getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(
                                w)),
                        blockEntityMatrixEntry.getModel(),
                        blockEntityMatrixEntry.getNormal()
                    );
                    vertexConsumerProvider = (renderLayer) -> {
                        VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
                        return renderLayer.hasCrumbling() ? VertexConsumers.union(
                            vertexConsumer,
                            vertexConsumer2
                        ) : vertexConsumer2;
                    };
                }
            }

            BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrices, vertexConsumerProvider);
            matrices.pop();
        }

        FastObjectArrayList<BlockEntity> globalTileEntityList = (FastObjectArrayList<BlockEntity>) (Object) terrainRenderer
            .getGlobalTileEntityList().getFront();
        for (int i = 0; i < globalTileEntityList.size(); i++) {
            BlockEntity blockEntity = globalTileEntityList.get(i);
            BlockPos pos = blockEntity.getPos();
            matrices.push();
            matrices.translate(pos.getX() - renderPosX, pos.getY() - renderPosY, pos.getZ() - renderPosZ);
            BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrices, immediate);
            matrices.pop();
        }
    }

    private void setupTerrainFogShader(Camera camera, float viewDistance, boolean thickFog) {
        float red = AccessorBackgroundRenderer.getRed();
        float green = AccessorBackgroundRenderer.getGreen();
        float blue = AccessorBackgroundRenderer.getBlue();

        FluidState fluidState = camera.getSubmergedFluidState();
        Entity entity = camera.getFocusedEntity();
        TerrainShaderManager fogManager = getTerrainRenderer().getShaderManager();

        if (fluidState.isIn(FluidTags.WATER)) {
            float density = 0.05F;
            if (entity instanceof ClientPlayerEntity) {
                ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity) entity;
                float underwaterVisibility = clientPlayerEntity.getUnderwaterVisibility();
                density -= underwaterVisibility * underwaterVisibility * 0.03F;
                Biome biome = clientPlayerEntity.world.getBiome(clientPlayerEntity.getBlockPos());
                if (biome.getCategory() == Biome.Category.SWAMP) {
                    density += 0.005F;
                }
            }

            fogManager.exp2Fog(TerrainShaderManager.FogShape.SPHERE, density, red, green, blue);
        } else {
            float start;
            float end;
            if (fluidState.isIn(FluidTags.LAVA)) {
                if (entity instanceof LivingEntity && ((LivingEntity) entity).hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                    start = 0.0F;
                    end = 3.0F;
                } else {
                    start = 0.25F;
                    end = 1.0F;
                }
            } else if (entity instanceof LivingEntity && ((LivingEntity) entity).hasStatusEffect(StatusEffects.BLINDNESS)) {
                int duration = ((LivingEntity) entity).getStatusEffect(StatusEffects.BLINDNESS).getDuration();
                float amount = MathHelper.lerp(Math.min(1.0F, (float) duration / 20.0F), viewDistance, 5.0F);
                start = amount * 0.25F;
                end = amount;
            } else if (thickFog) {
                start = viewDistance * 0.05F;
                end = Math.min(viewDistance, 192.0F) * 0.5F;
            } else {
                start = viewDistance * 0.75F;
                end = viewDistance;
            }

            fogManager.linearFog(TerrainShaderManager.FogShape.SPHERE, start, end, red, green, blue);
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
        AbstractTexture texture = textureManager.getTexture(identifier);
        if (texture == null) {
            texture = new ResourceTexture(identifier);
            textureManager.registerTexture(identifier, texture);
        }

        return texture;
    }
}