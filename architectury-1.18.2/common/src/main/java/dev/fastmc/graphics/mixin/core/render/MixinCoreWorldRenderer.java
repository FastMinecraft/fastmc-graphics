package dev.fastmc.graphics.mixin.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Shadow
    protected abstract void resetTransparencyShader();

    @Shadow
    protected abstract void loadTransparencyShader();

    @Shadow private @Nullable Framebuffer translucentFramebuffer;

    @Shadow private @Nullable Framebuffer weatherFramebuffer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Inject$init$RETURN(MinecraftClient client, BufferBuilderStorage bufferBuilders, CallbackInfo ci) {
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
            if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                this.loadTransparencyShader();
            } else {
                this.resetTransparencyShader();
            }

            RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());

            this.cloudsDirty = true;
            this.viewDistance = this.client.options.viewDistance;
            this.world.reloadColor();

            TerrainRenderer terrainRenderer = getTerrainRenderer();
            terrainRenderer.clear();
            terrainRenderer.updateChunkStorage(this.client.options.viewDistance);
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
        org.joml.Matrix4f projection1 = AdaptersKt.toJoml(projection);
        org.joml.Matrix4f modelView1 = AdaptersKt.toJoml(modelView);
        worldRenderer.updateMatrix(projection1, modelView1);
        worldRenderer.updateCameraPos(renderPosX, renderPosY, renderPosZ);
        worldRenderer.updateRenderPos(renderPosX, renderPosY, renderPosZ);
        worldRenderer.updateCameraRotation(camera.getYaw(), camera.getPitch());
        worldRenderer.updateFrustum();
        worldRenderer.updateGlobalUBO(tickDelta);
        worldRenderer.getTerrainRenderer().update();

        float viewDistance = gameRenderer.getViewDistance();
        assert this.client.world != null;
        boolean thickFog = this.client.world.getDimensionEffects().useThickFog(
            MathHelper.floor(renderPosX),
            MathHelper.floor(renderPosY)
        ) || this.client.inGameHud.getBossBarHud().shouldThickenFog();
        float fogDistance = Math.max(viewDistance - 16.0F, 32.0F);
        applyFogShader(camera, fogDistance, thickFog);
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
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL_LEQUAL);
    }

    private static void preRenderTranslucent() {
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.SRC_ALPHA,
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SrcFactor.ONE,
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL_LEQUAL);
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

        FastObjectArrayList<BlockEntity> renderTileEntityList = (FastObjectArrayList<BlockEntity>) (Object) terrainRenderer
            .getRenderTileEntityList().get();
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
                        entry.getNormalMatrix()
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

        FastObjectArrayList<BlockEntity> globalTileEntityList = (FastObjectArrayList<BlockEntity>) (Object) terrainRenderer
            .getGlobalTileEntityList().get();
        for (int i = 0; i < globalTileEntityList.size(); i++) {
            BlockEntity blockEntity = globalTileEntityList.get(i);
            BlockPos pos = blockEntity.getPos();
            matrices.push();
            matrices.translate(pos.getX() - renderPosX, pos.getY() - renderPosY, pos.getZ() - renderPosZ);
            blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, immediate);
            matrices.pop();
        }
    }

    @SuppressWarnings("deprecation")
    private void applyFogShader(Camera camera, float viewDistance, boolean thickFog) {
        float end;
        float start;
        CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
        Entity entity = camera.getFocusedEntity();
        TerrainShaderManager.FogShape fogShape = TerrainShaderManager.FogShape.SPHERE;
        if (cameraSubmersionType == CameraSubmersionType.LAVA) {
            if (entity.isSpectator()) {
                start = -8.0f;
                end = viewDistance * 0.5f;
            } else if (entity instanceof LivingEntity && ((LivingEntity) entity).hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                start = 0.0f;
                end = 3.0f;
            } else {
                start = 0.25f;
                end = 1.0f;
            }
        } else if (cameraSubmersionType == CameraSubmersionType.POWDER_SNOW) {
            if (entity.isSpectator()) {
                start = -8.0f;
                end = viewDistance * 0.5f;
            } else {
                start = 0.0f;
                end = 2.0f;
            }
        } else if (entity instanceof LivingEntity && ((LivingEntity) entity).hasStatusEffect(StatusEffects.BLINDNESS)) {
            @SuppressWarnings("ConstantConditions")
            int i = ((LivingEntity) entity).getStatusEffect(StatusEffects.BLINDNESS).getDuration();
            float h = MathHelper.lerp(Math.min(1.0f, (float) i / 20.0f), viewDistance, 5.0f);
            start = cameraSubmersionType == CameraSubmersionType.WATER ? -4.0f : h * 0.25f;
            end = h;
        } else if (cameraSubmersionType == CameraSubmersionType.WATER) {
            start = -8.0f;
            end = 96.0f;
            if (entity instanceof ClientPlayerEntity clientPlayerEntity) {
                end *= Math.max(0.25f, clientPlayerEntity.getUnderwaterVisibility());
                RegistryEntry<Biome> registryEntry = clientPlayerEntity.world.getBiome(clientPlayerEntity.getBlockPos());
                if (Biome.getCategory(registryEntry) == Biome.Category.SWAMP) {
                    end *= 0.85f;
                }
            }
            if (end > viewDistance) {
                end = viewDistance;
                fogShape = TerrainShaderManager.FogShape.CYLINDER;
            }
        } else if (thickFog) {
            start = viewDistance * 0.05f;
            end = Math.min(viewDistance * 0.5f, 96.0f);
        } else {
            start = viewDistance - MathHelper.clamp(viewDistance / 10.0f, 4.0f, 64.0f);
            end = viewDistance;
            fogShape = TerrainShaderManager.FogShape.CYLINDER;
        }

        FastMcMod.INSTANCE.getWorldRenderer().getTerrainRenderer().getShaderManager().linearFog(
            fogShape,
            start,
            end,
            AccessorBackgroundRenderer.getRed(),
            AccessorBackgroundRenderer.getGreen(),
            AccessorBackgroundRenderer.getBlue()
        );
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