package dev.fastmc.graphics.mixin.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.fastmc.common.collection.FastObjectArrayList;
import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.mixin.accessor.AccessorBackgroundRenderer;
import dev.fastmc.graphics.mixin.accessor.AccessorLightmapTextureManager;
import dev.fastmc.graphics.shared.renderer.WorldRenderer;
import dev.fastmc.graphics.shared.terrain.RenderChunk;
import dev.fastmc.graphics.shared.terrain.TerrainRenderer;
import dev.fastmc.graphics.shared.terrain.TerrainShaderManager;
import dev.fastmc.graphics.util.AdaptersKt;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.SortedSet;

import static dev.fastmc.graphics.shared.opengl.GLWrapperKt.*;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;

@SuppressWarnings("deprecation")
@Mixin(value = net.minecraft.client.render.WorldRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinCoreWorldRenderer {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private ClientWorld world;
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow
    private @Nullable Frustum capturedFrustum;
    @Shadow
    private int regularEntityCount;
    @Shadow
    private int blockEntityCount;
    @Shadow
    private @Nullable Framebuffer entityFramebuffer;
    @Shadow
    private @Nullable Framebuffer weatherFramebuffer;
    @Shadow
    private @Nullable Framebuffer entityOutlinesFramebuffer;
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;
    @Shadow
    @Final
    private Vector3d capturedFrustumPosition;
    @Shadow
    private boolean shouldCaptureFrustum;
    @Shadow
    private @Nullable ShaderEffect entityOutlineShader;
    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;
    @Shadow
    private @Nullable Framebuffer translucentFramebuffer;
    @Shadow
    private @Nullable Framebuffer particlesFramebuffer;
    @Shadow
    private @Nullable ShaderEffect transparencyShader;
    @Shadow
    private @Nullable Framebuffer cloudsFramebuffer;

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
    protected abstract void resetTransparencyShader();

    @Shadow
    protected abstract void loadTransparencyShader();

    @Shadow
    protected abstract boolean canDrawEntityOutlines();

    @Shadow
    protected abstract void captureFrustum(
        Matrix4f modelMatrix,
        Matrix4f matrix4f,
        double x,
        double y,
        double z,
        Frustum frustum
    );

    @Shadow
    protected abstract void checkEmpty(MatrixStack matrices);

    @Shadow
    protected abstract void drawBlockOutline(
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        Entity entity,
        double d,
        double e,
        double f,
        BlockPos blockPos,
        BlockState blockState
    );

    @Shadow
    protected abstract void renderWeather(LightmapTextureManager manager, float f, double d, double e, double g);

    @Shadow
    protected abstract void renderWorldBorder(Camera camera);

    @Shadow
    protected abstract void renderChunkDebugInfo(Camera camera);

    @Shadow
    protected abstract void renderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers
    );

    @Shadow
    @Final
    private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    @Shadow
    private Frustum frustum;

    @Shadow
    public abstract void renderClouds(
        MatrixStack par1,
        Matrix4f par2,
        float par3,
        double par4,
        double par5,
        double par6
    );

    @Shadow
    public abstract void renderSky(
        MatrixStack par1,
        Matrix4f par2,
        float par3,
        Camera par4,
        boolean par5,
        Runnable par6
    );

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

    /**
     * @author Luna
     * @reason Mojang made a whole mess
     */
    @Overwrite
    public void render(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f projection
    ) {
        assert this.client.world != null;
        Profiler profiler = this.world.getProfiler();
        profiler.swap("setup");

        RenderSystem.setShaderGameTime(this.world.getTime(), tickDelta);
        this.blockEntityRenderDispatcher.configure(this.world, camera, this.client.crosshairTarget);
        this.entityRenderDispatcher.configure(this.world, camera, this.client.targetedEntity);
        Vec3d cameraPos = camera.getPos();
        double renderPosX = cameraPos.getX();
        double renderPosY = cameraPos.getY();
        double renderPosZ = cameraPos.getZ();
        Matrix4f modelView = matrices.peek().getPositionMatrix().copy();
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        worldRenderer.updateScreenSize(
            client.getWindow().getFramebufferWidth(),
            client.getWindow().getFramebufferHeight()
        );
        TerrainRenderer terrainRenderer = worldRenderer.getTerrainRenderer();
        org.joml.Matrix4f projection1 = AdaptersKt.toJoml(projection);
        org.joml.Matrix4f modelView1 = AdaptersKt.toJoml(modelView);
        worldRenderer.updateMatrix(projection1, modelView1);
        worldRenderer.updateCameraPos(renderPosX, renderPosY, renderPosZ);
        worldRenderer.updateRenderPos(renderPosX, renderPosY, renderPosZ);
        worldRenderer.updateCameraRotation(camera.getYaw(), camera.getPitch());
        worldRenderer.updateFrustum();
        worldRenderer.updateGlobalUBO(tickDelta);

        profiler.swap("light_update_queue");
        this.world.runQueuedChunkUpdates();
        profiler.swap("updateTerrain");
        terrainRenderer.update();

        profiler.swap("culling");
        boolean bl = this.capturedFrustum != null;
        Frustum frustum;
        if (bl) {
            frustum = this.capturedFrustum;
            frustum.setPosition(
                this.capturedFrustumPosition.x,
                this.capturedFrustumPosition.y,
                this.capturedFrustumPosition.z
            );
        } else {
            frustum = this.frustum;
        }

        this.client.getProfiler().swap("captureFrustum");
        if (this.shouldCaptureFrustum) {
            this.captureFrustum(
                modelView,
                projection,
                renderPosX,
                renderPosY,
                renderPosZ,
                bl ? new Frustum(modelView, projection) : frustum
            );
            this.shouldCaptureFrustum = false;
        }

        profiler.swap("clear");
        BackgroundRenderer.render(
            camera,
            tickDelta,
            this.client.world,
            this.client.options.viewDistance,
            gameRenderer.getSkyDarkness(tickDelta)
        );
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
        float viewDistance = gameRenderer.getViewDistance();
        boolean thickFog = this.client.world.getDimensionEffects().useThickFog(
            MathHelper.floor(renderPosX),
            MathHelper.floor(renderPosY)
        ) || this.client.inGameHud.getBossBarHud().shouldThickenFog();

        profiler.swap("sky");
        RenderSystem.setShader(GameRenderer::getPositionShader);
        this.renderSky(
            matrices,
            projection,
            tickDelta,
            camera,
            thickFog,
            () -> BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, viewDistance, thickFog)
        );

        profiler.swap("fog");
        float fogDistance = Math.max(viewDistance - 16.0F, 32.0F);
        BackgroundRenderer.applyFog(
            camera,
            BackgroundRenderer.FogType.FOG_TERRAIN,
            viewDistance,
            thickFog
        );
        setupTerrainFog(camera, fogDistance, thickFog);

        profiler.swap("terrain");
        renderTerrainPass1(lightmapTextureManager);

        if (this.world.getDimensionEffects().isDarkened()) {
            DiffuseLighting.enableForLevel(matrices.peek().getPositionMatrix());
        } else {
            DiffuseLighting.disableForLevel(matrices.peek().getPositionMatrix());
        }

        profiler.swap("entities");
        profiler.push("setup");
        this.regularEntityCount = 0;
        this.blockEntityCount = 0;

        if (this.entityFramebuffer != null) {
            this.entityFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.entityFramebuffer.copyDepthFrom(this.client.getFramebuffer());
            this.client.getFramebuffer().beginWrite(false);
        }

        if (this.weatherFramebuffer != null) {
            this.weatherFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
        }

        if (this.canDrawEntityOutlines()) {
            assert this.entityOutlinesFramebuffer != null;
            this.entityOutlinesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.client.getFramebuffer().beginWrite(false);
        }

        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();

        profiler.swap("vanilla");
        boolean entityRendered = renderEntity(
            matrices,
            tickDelta,
            camera,
            renderPosX,
            renderPosY,
            renderPosZ,
            frustum,
            immediate
        );
        profiler.pop();

        // Tile entities
        profiler.swap("tileEntities");
        profiler.push("vanilla");
        renderTileEntityVanilla(matrices, tickDelta, renderPosX, renderPosY, renderPosZ, immediate);
        profiler.swap("fastMinecraft");
        renderTileEntityFastMc(tickDelta);
        profiler.pop();

        // Entity outline
        profiler.swap("entities");
        profiler.push("outline");
        this.bufferBuilders.getOutlineVertexConsumers().draw();
        if (entityRendered) {
            assert this.entityOutlineShader != null;
            this.entityOutlineShader.render(tickDelta);
            this.client.getFramebuffer().beginWrite(false);
        }
        profiler.pop();

        // Breaking progress
        profiler.swap("destroyProgress");
        for (Long2ObjectMap.Entry<SortedSet<BlockBreakingInfo>> blockBreakingInfoEntry : this.blockBreakingProgressions.long2ObjectEntrySet()) {
            BlockPos blockPos3 = BlockPos.fromLong(blockBreakingInfoEntry.getLongKey());
            double h = (double) blockPos3.getX() - renderPosX;
            double x = (double) blockPos3.getY() - renderPosY;
            double y = (double) blockPos3.getZ() - renderPosZ;
            if (!(h * h + x * x + y * y > 1024.0D)) {
                SortedSet<BlockBreakingInfo> sortedSet2 = blockBreakingInfoEntry.getValue();
                if (sortedSet2 != null && !sortedSet2.isEmpty()) {
                    int z = sortedSet2.last().getStage();
                    matrices.push();
                    matrices.translate(
                        (double) blockPos3.getX() - renderPosX,
                        (double) blockPos3.getY() - renderPosY,
                        (double) blockPos3.getZ() - renderPosZ
                    );
                    MatrixStack.Entry entry3 = matrices.peek();
                    VertexConsumer vertexConsumer2 = new OverlayVertexConsumer(
                        this.bufferBuilders.getEffectVertexConsumers().getBuffer(
                            ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(z)),
                        entry3.getPositionMatrix(),
                        entry3.getNormalMatrix()
                    );
                    this.client.getBlockRenderManager().renderDamage(
                        this.world.getBlockState(blockPos3),
                        blockPos3,
                        this.world,
                        matrices,
                        vertexConsumer2
                    );
                    matrices.pop();
                }
            }
        }

        // Selected block outline
        this.checkEmpty(matrices);
        HitResult hitResult = this.client.crosshairTarget;
        if (renderBlockOutline && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            profiler.swap("outline");
            BlockPos blockPos4 = ((BlockHitResult) hitResult).getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos4);
            if (!blockState.isAir() && this.world.getWorldBorder().contains(blockPos4)) {
                VertexConsumer vertexConsumer3 = immediate.getBuffer(RenderLayer.getLines());
                this.drawBlockOutline(
                    matrices,
                    vertexConsumer3,
                    camera.getFocusedEntity(),
                    renderPosX,
                    renderPosY,
                    renderPosZ,
                    blockPos4,
                    blockState
                );
            }
        }

        // Debug renderer
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();
        this.client.debugRenderer.render(matrices, immediate, renderPosX, renderPosY, renderPosZ);
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        immediate.draw(TexturedRenderLayers.getEntityTranslucentCull());
        immediate.draw(TexturedRenderLayers.getBannerPatterns());
        immediate.draw(TexturedRenderLayers.getShieldPatterns());
        immediate.draw(RenderLayer.getArmorGlint());
        immediate.draw(RenderLayer.getArmorEntityGlint());
        immediate.draw(RenderLayer.getGlint());
        immediate.draw(RenderLayer.getDirectGlint());
        immediate.draw(RenderLayer.getGlintTranslucent());
        immediate.draw(RenderLayer.getEntityGlint());
        immediate.draw(RenderLayer.getDirectEntityGlint());
        immediate.draw(RenderLayer.getWaterMask());
        this.bufferBuilders.getEffectVertexConsumers().draw();

        profiler.swap("terrain");
        if (this.translucentFramebuffer != null) {
            immediate.draw(RenderLayer.getLines());
            immediate.draw();
            this.translucentFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.translucentFramebuffer.copyDepthFrom(this.client.getFramebuffer());

            renderTerrainPass2(lightmapTextureManager);

            profiler.swap("particles");
            assert this.particlesFramebuffer != null;
            this.particlesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.particlesFramebuffer.copyDepthFrom(this.client.getFramebuffer());
            ((RenderPhase) RenderPhase.PARTICLES_TARGET).startDrawing();
            this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
            ((RenderPhase) RenderPhase.PARTICLES_TARGET).endDrawing();

        } else {
            renderTerrainPass2(lightmapTextureManager);

            immediate.draw(RenderLayer.getLines());
            immediate.draw();

            profiler.swap("particles");
            this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
        }

        matrixStack.push();
        matrixStack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();
        if (this.client.options.getCloudRenderMode() != CloudRenderMode.OFF) {
            profiler.swap("clouds");
            if (this.transparencyShader != null) {
                assert this.cloudsFramebuffer != null;
                this.cloudsFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                ((RenderPhase) RenderPhase.CLOUDS_TARGET).startDrawing();
                this.renderClouds(matrices, projection, tickDelta, renderPosX, renderPosY, renderPosZ);
                ((RenderPhase) RenderPhase.CLOUDS_TARGET).endDrawing();
            } else {
                this.renderClouds(matrices, projection, tickDelta, renderPosX, renderPosY, renderPosZ);
            }
        }

        if (this.transparencyShader != null) {
            ((RenderPhase) RenderPhase.WEATHER_TARGET).startDrawing();
            profiler.swap("weather");
            this.renderWeather(lightmapTextureManager, tickDelta, renderPosX, renderPosY, renderPosZ);
            this.renderWorldBorder(camera);
            ((RenderPhase) RenderPhase.WEATHER_TARGET).endDrawing();
            this.transparencyShader.render(tickDelta);
            this.client.getFramebuffer().beginWrite(false);
        } else {
            RenderSystem.depthMask(false);
            profiler.swap("weather");
            this.renderWeather(lightmapTextureManager, tickDelta, renderPosX, renderPosY, renderPosZ);
            this.renderWorldBorder(camera);
            RenderSystem.depthMask(true);
        }

        this.renderChunkDebugInfo(camera);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        BackgroundRenderer.clearFog();
    }

    private void renderTerrainPass1(LightmapTextureManager lightmapTextureManager) {
        this.client.getProfiler().push("solid");

        TerrainRenderer terrainRenderer = getTerrainRenderer();
        TerrainShaderManager shaderManager = terrainRenderer.getShaderManager();

        int lightMapTexture = getTexture((((AccessorLightmapTextureManager) lightmapTextureManager).getTextureIdentifier())).getGlId();
        glTextureParameteri(lightMapTexture, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTextureParameteri(lightMapTexture, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTextureParameteri(lightMapTexture, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTextureParameteri(lightMapTexture, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTextureUnit(FastMcMod.INSTANCE.getGlWrapper().getLightMapUnit(), lightMapTexture);

        AbstractTexture blockTexture = getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        blockTexture.bindTexture();
        blockTexture.setFilter(false, true);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL_LEQUAL);

        TerrainShaderManager.TerrainShaderProgram deferredShader = shaderManager.getShader();
        deferredShader.bind();

        terrainRenderer.renderLayer(0);

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        glBindVertexArray(0);

        this.client.getProfiler().pop();
    }

    private void renderTerrainPass2(LightmapTextureManager lightmapTextureManager) {
        this.client.getProfiler().push("translucent");
        lightmapTextureManager.enable();

        AbstractTexture blockTexture = getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        blockTexture.bindTexture();
        blockTexture.setFilter(false, true);

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

        Framebuffer translucent = this.translucentFramebuffer;
        Framebuffer weather = this.weatherFramebuffer;

        boolean usingFbo = MinecraftClient.isFabulousGraphicsOrBetter() && translucent != null && weather != null;
        if (usingFbo) {
            translucent.beginWrite(false);
        } else {
            MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        }

        TerrainRenderer terrainRenderer = getTerrainRenderer();
        TerrainShaderManager shaderManager = terrainRenderer.getShaderManager();

        TerrainShaderManager.TerrainShaderProgram shader = shaderManager.getShader();
        shader.bind();

        terrainRenderer.renderLayer(1);

        if (usingFbo) {
            weather.beginWrite(false);
        }
        this.client.getProfiler().swap("tripwire");
        terrainRenderer.renderLayer(2);
        this.client.getProfiler().pop();

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        glBindVertexArray(0);
        shader.unbind();

        lightmapTextureManager.disable();
    }

    private AbstractTexture getTexture(Identifier identifier) {
        AbstractTexture texture = client.getTextureManager().getTexture(identifier);
        if (texture == null) {
            texture = new ResourceTexture(identifier);
            client.getTextureManager().registerTexture(identifier, texture);
        }

        return texture;
    }

    private boolean renderEntity(
        MatrixStack matrices,
        float tickDelta,
        Camera camera,
        double renderPosX,
        double renderPosY,
        double renderPosZ,
        Frustum frustum,
        VertexConsumerProvider.Immediate immediate
    ) {
        boolean entityRendered = false;

        for (Entity entity : this.world.getEntities()) {
            if (entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity) continue;
            if (entity == camera.getFocusedEntity() && !camera.isThirdPerson()
                && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity) camera.getFocusedEntity()).isSleeping()))
                continue;
            if (!this.entityRenderDispatcher.shouldRender(entity, frustum, renderPosX, renderPosY, renderPosZ)
                && !entity.hasPassengerDeep(this.client.player)) continue;

            ++this.regularEntityCount;
            if (entity.age == 0) {
                entity.lastRenderX = entity.getX();
                entity.lastRenderY = entity.getY();
                entity.lastRenderZ = entity.getZ();
            }

            VertexConsumerProvider vertexConsumerProvider;
            if (this.canDrawEntityOutlines() && this.client.hasOutline(entity)) {
                entityRendered = true;
                OutlineVertexConsumerProvider outlineVertexConsumerProvider = this.bufferBuilders.getOutlineVertexConsumers();
                vertexConsumerProvider = outlineVertexConsumerProvider;
                int k = entity.getTeamColorValue();
                int t = k >> 16 & 255;
                int u = k >> 8 & 255;
                int w = k & 255;
                outlineVertexConsumerProvider.setColor(t, u, w, 255);
            } else {
                vertexConsumerProvider = immediate;
            }

            this.renderEntity(entity, renderPosX, renderPosY, renderPosZ, tickDelta, matrices, vertexConsumerProvider);
        }

        immediate.drawCurrentLayer();
        this.checkEmpty(matrices);
        immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));

        return entityRendered;
    }

    private void renderTileEntityFastMc(float tickDelta) {
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        worldRenderer.preRender(tickDelta);
        worldRenderer.getTileEntityRenderer().render();
        worldRenderer.postRender();
    }

    @SuppressWarnings("unchecked")
    public void renderTileEntityVanilla(
        @NotNull MatrixStack matrices,
        float tickDelta,
        double renderPosX,
        double renderPosY,
        double renderPosZ,
        @NotNull VertexConsumerProvider.Immediate immediate
    ) {
        TerrainRenderer terrainRenderer = getTerrainRenderer();
        FastObjectArrayList<BlockEntity> renderTileEntityList = (FastObjectArrayList<BlockEntity>) (Object) terrainRenderer.getRenderTileEntityList().get();
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

        FastObjectArrayList<BlockEntity> globalTileEntityList = (FastObjectArrayList<BlockEntity>) (Object) terrainRenderer.getGlobalTileEntityList().get();
        for (int i = 0; i < globalTileEntityList.size(); i++) {
            BlockEntity blockEntity = globalTileEntityList.get(i);
            BlockPos pos = blockEntity.getPos();
            matrices.push();
            matrices.translate(pos.getX() - renderPosX, pos.getY() - renderPosY, pos.getZ() - renderPosZ);
            blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, immediate);
            matrices.pop();
        }

        this.checkEmpty(matrices);
        immediate.draw(RenderLayer.getSolid());
        immediate.draw(RenderLayer.getEndPortal());
        immediate.draw(RenderLayer.getEndGateway());
        immediate.draw(TexturedRenderLayers.getEntitySolid());
        immediate.draw(TexturedRenderLayers.getEntityCutout());
        immediate.draw(TexturedRenderLayers.getBeds());
        immediate.draw(TexturedRenderLayers.getShulkerBoxes());
        immediate.draw(TexturedRenderLayers.getSign());
        immediate.draw(TexturedRenderLayers.getChest());
    }

    @SuppressWarnings("deprecation")
    private static void setupTerrainFog(
        Camera camera,
        float viewDistance,
        boolean thickFog
    ) {
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

    @NotNull
    private TerrainRenderer getTerrainRenderer() {
        return FastMcMod.INSTANCE.getWorldRenderer().getTerrainRenderer();
    }
}