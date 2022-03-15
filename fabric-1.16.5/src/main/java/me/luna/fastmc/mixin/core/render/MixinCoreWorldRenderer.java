package me.luna.fastmc.mixin.core.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.IPatchedWorldRenderer;
import me.luna.fastmc.util.AdaptersKt;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.Option;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.SortedSet;

@Mixin(value = WorldRenderer.class, priority = Integer.MAX_VALUE)
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
    protected abstract void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator);

    @Shadow
    protected abstract void updateChunks(long limitTime);

    @Shadow
    @Final
    private FpsSmoother chunkUpdateSmoother;

    @Shadow
    protected abstract void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f);

    @Shadow
    private int regularEntityCount;
    @Shadow
    private int blockEntityCount;
    @Shadow
    private @Nullable Framebuffer entityFramebuffer;
    @Shadow
    private @Nullable Framebuffer weatherFramebuffer;

    @Shadow
    protected abstract boolean canDrawEntityOutlines();

    @Shadow
    private @Nullable Framebuffer entityOutlinesFramebuffer;
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;
    @Shadow
    @Final
    private ObjectList<WorldRenderer.ChunkInfo> visibleChunks;
    @Shadow
    @Final
    private Vector3d capturedFrustumPosition;
    @Shadow
    private boolean shouldCaptureFrustum;

    @Shadow
    protected abstract void captureFrustum(Matrix4f modelMatrix, Matrix4f matrix4f, double x, double y, double z, Frustum frustum);

    @Shadow
    public abstract void renderSky(MatrixStack matrices, float tickDelta);

    @Shadow
    private int frame;

    @Shadow
    protected abstract void checkEmpty(MatrixStack matrices);

    @Shadow
    @Final
    private Set<BlockEntity> noCullingBlockEntities;
    @Shadow
    private @Nullable ShaderEffect entityOutlineShader;
    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;

    @Shadow
    protected abstract void drawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState);

    @Shadow
    private @Nullable Framebuffer translucentFramebuffer;
    @Shadow
    private @Nullable Framebuffer particlesFramebuffer;
    @Shadow
    private @Nullable ShaderEffect transparencyShader;
    @Shadow
    private @Nullable Framebuffer cloudsFramebuffer;

    @Shadow
    public abstract void renderClouds(MatrixStack matrices, float tickDelta, double cameraX, double cameraY, double cameraZ);

    @Shadow
    protected abstract void renderWeather(LightmapTextureManager manager, float f, double d, double e, double g);

    @Shadow
    protected abstract void renderWorldBorder(Camera camera);

    @Shadow
    protected abstract void renderChunkDebugInfo(Camera camera);

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Inject(method = "setWorld", at = @At("HEAD"))
    public void setWorld$Inject$HEAD(@Nullable ClientWorld world, CallbackInfo ci) {
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().clear();
        FastMcMod.INSTANCE.getWorldRenderer().getEntityRenderer().clear();
    }

    /**
     * @author Luna
     * @reason Mojang made a whole mess
     */
    @SuppressWarnings({ "ConstantConditions", "deprecation" })
    @Overwrite
    public void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f) {
        BlockEntityRenderDispatcher.INSTANCE.configure(this.world, this.client.getTextureManager(), this.client.textRenderer, camera, this.client.crosshairTarget);
        this.entityRenderDispatcher.configure(this.world, camera, this.client.targetedEntity);
        Profiler profiler = this.world.getProfiler();
        profiler.swap("light_updates");
        this.client.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
        Vec3d vec3d = camera.getPos();
        double renderPosX = vec3d.getX();
        double renderPosY = vec3d.getY();
        double renderPosZ = vec3d.getZ();
        Matrix4f matrix4f2 = matrices.peek().getModel();
        profiler.swap("culling");
        boolean bl = this.capturedFrustum != null;
        Frustum frustum;
        if (bl) {
            frustum = this.capturedFrustum;
            frustum.setPosition(this.capturedFrustumPosition.x, this.capturedFrustumPosition.y, this.capturedFrustumPosition.z);
        } else {
            frustum = new Frustum(matrix4f2, matrix4f);
            frustum.setPosition(renderPosX, renderPosY, renderPosZ);
        }

        this.client.getProfiler().swap("captureFrustum");
        if (this.shouldCaptureFrustum) {
            this.captureFrustum(matrix4f2, matrix4f, vec3d.x, vec3d.y, vec3d.z, bl ? new Frustum(matrix4f2, matrix4f) : frustum);
            this.shouldCaptureFrustum = false;
        }

        profiler.swap("clear");
        BackgroundRenderer.render(camera, tickDelta, this.client.world, this.client.options.viewDistance, gameRenderer.getSkyDarkness(tickDelta));
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
        float g = gameRenderer.getViewDistance();
        boolean bl2 = this.client.world.getSkyProperties().useThickFog(MathHelper.floor(renderPosX), MathHelper.floor(renderPosY)) || this.client.inGameHud.getBossBarHud().shouldThickenFog();
        if (this.client.options.viewDistance >= 4) {
            BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, g, bl2);
            profiler.swap("sky");
            this.renderSky(matrices, tickDelta);
        }

        profiler.swap("fog");
        BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(g - 16.0F, 32.0F), bl2);
        profiler.swap("terrain_setup");
        this.setupTerrain(camera, frustum, bl, this.frame++, this.client.player.isSpectator());
        profiler.swap("updatechunks");
        int j = this.client.options.maxFps;
        long n;
        if ((double) j == Option.FRAMERATE_LIMIT.getMax()) {
            n = 0L;
        } else {
            n = 1000000000 / j;
        }

        long o = Util.getMeasuringTimeNano() - limitTime;
        long p = this.chunkUpdateSmoother.getTargetUsedTime(o);
        long q = p * 3L / 2L;
        long r = MathHelper.clamp(q, n, 33333333L);
        this.updateChunks(limitTime + r);
        profiler.swap("terrain");
        this.renderLayer(RenderLayer.getSolid(), matrices, renderPosX, renderPosY, renderPosZ);
        this.renderLayer(RenderLayer.getCutoutMipped(), matrices, renderPosX, renderPosY, renderPosZ);
        this.renderLayer(RenderLayer.getCutout(), matrices, renderPosX, renderPosY, renderPosZ);
        if (this.world.getSkyProperties().isDarkened()) {
            DiffuseLighting.enableForLevel(matrices.peek().getModel());
        } else {
            DiffuseLighting.disableForLevel(matrices.peek().getModel());
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
            this.entityOutlinesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.client.getFramebuffer().beginWrite(false);
        }

        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();

        profiler.swap("vanilla");
        boolean entityRendered = renderEntity(
            matrices,
            tickDelta,
            limitTime,
            renderBlockOutline,
            camera,
            gameRenderer,
            lightmapTextureManager,
            matrix4f,
            profiler,
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
        renderTileEntityFastMc(matrices, tickDelta, matrix4f, profiler);
        profiler.pop();

        // Entity outline
        profiler.swap("entities");
        profiler.push("outline");
        if (entityRendered) {
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
                    matrices.translate((double) blockPos3.getX() - renderPosX, (double) blockPos3.getY() - renderPosY, (double) blockPos3.getZ() - renderPosZ);
                    MatrixStack.Entry entry3 = matrices.peek();
                    VertexConsumer vertexConsumer2 = new OverlayVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer((RenderLayer) ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(z)), entry3.getModel(), entry3.getNormal());
                    this.client.getBlockRenderManager().renderDamage(this.world.getBlockState(blockPos3), blockPos3, this.world, matrices, vertexConsumer2);
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
                this.drawBlockOutline(matrices, vertexConsumer3, camera.getFocusedEntity(), renderPosX, renderPosY, renderPosZ, blockPos4, blockState);
            }
        }

        // Debug renderer
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrices.peek().getModel());
        this.client.debugRenderer.render(matrices, immediate, renderPosX, renderPosY, renderPosZ);
        RenderSystem.popMatrix();
        immediate.draw(TexturedRenderLayers.getEntityTranslucentCull());
        immediate.draw(TexturedRenderLayers.getBannerPatterns());
        immediate.draw(TexturedRenderLayers.getShieldPatterns());
        immediate.draw(RenderLayer.getArmorGlint());
        immediate.draw(RenderLayer.getArmorEntityGlint());
        immediate.draw(RenderLayer.getGlint());
        immediate.draw(RenderLayer.getDirectGlint());
        immediate.draw(RenderLayer.method_30676());
        immediate.draw(RenderLayer.getEntityGlint());
        immediate.draw(RenderLayer.getDirectEntityGlint());
        immediate.draw(RenderLayer.getWaterMask());
        this.bufferBuilders.getEffectVertexConsumers().draw();

        if (this.translucentFramebuffer != null) {
            immediate.draw(RenderLayer.getLines());
            immediate.draw();
            this.translucentFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.translucentFramebuffer.copyDepthFrom(this.client.getFramebuffer());
            profiler.swap("translucent");
            this.renderLayer(RenderLayer.getTranslucent(), matrices, renderPosX, renderPosY, renderPosZ);
            profiler.swap("string");
            this.renderLayer(RenderLayer.getTripwire(), matrices, renderPosX, renderPosY, renderPosZ);
            this.particlesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.particlesFramebuffer.copyDepthFrom(this.client.getFramebuffer());
            RenderPhase.PARTICLES_TARGET.startDrawing();
            profiler.swap("particles");
            this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
            RenderPhase.PARTICLES_TARGET.endDrawing();
        } else {
            profiler.swap("translucent");
            this.renderLayer(RenderLayer.getTranslucent(), matrices, renderPosX, renderPosY, renderPosZ);
            immediate.draw(RenderLayer.getLines());
            immediate.draw();
            profiler.swap("string");
            this.renderLayer(RenderLayer.getTripwire(), matrices, renderPosX, renderPosY, renderPosZ);
            profiler.swap("particles");
            this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
        }

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrices.peek().getModel());
        if (this.client.options.getCloudRenderMode() != CloudRenderMode.OFF) {
            if (this.transparencyShader != null) {
                this.cloudsFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                RenderPhase.CLOUDS_TARGET.startDrawing();
                profiler.swap("clouds");
                this.renderClouds(matrices, tickDelta, renderPosX, renderPosY, renderPosZ);
                RenderPhase.CLOUDS_TARGET.endDrawing();
            } else {
                profiler.swap("clouds");
                this.renderClouds(matrices, tickDelta, renderPosX, renderPosY, renderPosZ);
            }
        }

        if (this.transparencyShader != null) {
            RenderPhase.WEATHER_TARGET.startDrawing();
            profiler.swap("weather");
            this.renderWeather(lightmapTextureManager, tickDelta, renderPosX, renderPosY, renderPosZ);
            this.renderWorldBorder(camera);
            RenderPhase.WEATHER_TARGET.endDrawing();
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
        RenderSystem.shadeModel(7424);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
        BackgroundRenderer.method_23792();
    }

    private boolean renderEntity(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Profiler profiler, double renderPosX, double renderPosY, double renderPosZ, Frustum frustum, VertexConsumerProvider.Immediate immediate) {
        boolean entityRendered = false;

        for (Entity entity : this.world.getEntities()) {
            if (!this.entityRenderDispatcher.shouldRender(entity, frustum, renderPosX, renderPosY, renderPosZ)
                && !entity.hasPassengerDeep(this.client.player)) continue;
            if (entity == camera.getFocusedEntity() && !camera.isThirdPerson()
                && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity) camera.getFocusedEntity()).isSleeping()))
                continue;
            if (entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity) continue;

            ++this.regularEntityCount;
            if (entity.age == 0) {
                entity.lastRenderX = entity.getX();
                entity.lastRenderY = entity.getY();
                entity.lastRenderZ = entity.getZ();
            }

            VertexConsumerProvider vertexConsumerProvider2;
            if (this.canDrawEntityOutlines() && this.client.hasOutline(entity)) {
                entityRendered = true;
                OutlineVertexConsumerProvider outlineVertexConsumerProvider = this.bufferBuilders.getOutlineVertexConsumers();
                vertexConsumerProvider2 = outlineVertexConsumerProvider;
                int k = entity.getTeamColorValue();
                int t = k >> 16 & 255;
                int u = k >> 8 & 255;
                int w = k & 255;
                outlineVertexConsumerProvider.setColor(t, u, w, 255);
            } else {
                vertexConsumerProvider2 = immediate;
            }

            this.renderEntity(entity, renderPosX, renderPosY, renderPosZ, tickDelta, matrices, vertexConsumerProvider2);
        }

        this.checkEmpty(matrices);
        immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));

        return entityRendered;
    }

    private void renderTileEntityFastMc(MatrixStack matrices, float tickDelta, Matrix4f matrix4f, Profiler profiler) {
        MatrixStack.Entry entry = matrices.peek();
        FastMcMod.INSTANCE.getWorldRenderer().setupCamera(AdaptersKt.toJoml(matrix4f), AdaptersKt.toJoml(entry.getModel()));
        FastMcMod.INSTANCE.getWorldRenderer().preRender(tickDelta);
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void renderTileEntityVanilla(@NotNull MatrixStack matrices, float tickDelta, double renderPosX, double renderPosY, double renderPosZ, @NotNull VertexConsumerProvider.Immediate immediate) {
        for (BlockEntity blockEntity : ((IPatchedWorldRenderer) this).getRenderTileEntityList()) {
            BlockPos blockPos = blockEntity.getPos();
            VertexConsumerProvider vertexConsumerProvider = immediate;

            matrices.push();
            matrices.translate(blockPos.getX() - renderPosX, blockPos.getY() - renderPosY, blockPos.getZ() - renderPosZ);

            SortedSet<BlockBreakingInfo> sortedSet = this.blockBreakingProgressions.get(blockPos.asLong());

            if (sortedSet != null && !sortedSet.isEmpty()) {
                int w = sortedSet.last().getStage();
                if (w >= 0) {
                    MatrixStack.Entry blockEntityMatrixEntry = matrices.peek();
                    VertexConsumer vertexConsumer = new OverlayVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(w)), blockEntityMatrixEntry.getModel(), blockEntityMatrixEntry.getNormal());
                    vertexConsumerProvider = (renderLayer) -> {
                        VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
                        return renderLayer.hasCrumbling() ? VertexConsumers.union(vertexConsumer, vertexConsumer2) : vertexConsumer2;
                    };
                }
            }

            BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrices, vertexConsumerProvider);
            matrices.pop();
        }

        synchronized (this.noCullingBlockEntities) {
            for (BlockEntity blockEntity2 : this.noCullingBlockEntities) {
                BlockPos blockPos2 = blockEntity2.getPos();
                matrices.push();
                matrices.translate((double) blockPos2.getX() - renderPosX, (double) blockPos2.getY() - renderPosY, (double) blockPos2.getZ() - renderPosZ);
                BlockEntityRenderDispatcher.INSTANCE.render(blockEntity2, tickDelta, matrices, immediate);
                matrices.pop();
            }
        }

        this.checkEmpty(matrices);
        immediate.draw(RenderLayer.getSolid());
        immediate.draw(TexturedRenderLayers.getEntitySolid());
        immediate.draw(TexturedRenderLayers.getEntityCutout());
        immediate.draw(TexturedRenderLayers.getBeds());
        immediate.draw(TexturedRenderLayers.getShulkerBoxes());
        immediate.draw(TexturedRenderLayers.getSign());
        immediate.draw(TexturedRenderLayers.getChest());
        this.bufferBuilders.getOutlineVertexConsumers().draw();
    }
}
