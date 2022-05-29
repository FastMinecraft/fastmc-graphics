package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.shared.FpsDisplay;
import me.luna.fastmc.shared.renderer.WorldRenderer;
import me.luna.fastmc.shared.terrain.TerrainFogManager;
import me.luna.fastmc.shared.terrain.TerrainRenderer;
import me.luna.fastmc.shared.util.MathUtils;
import me.luna.fastmc.shared.util.MathUtilsKt;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.luna.fastmc.shared.opengl.GLWrapperKt.*;
import static org.lwjgl.opengl.GL11.*;

@Mixin(value = EntityRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinCoreEntityRenderer {

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private float farPlaneDistance;

    @Shadow
    private boolean cloudFog;

    @Shadow
    private float fogColorRed;

    @Shadow
    private float fogColorGreen;

    @Shadow
    private float fogColorBlue;
    @Shadow
    private int frameCount;
    @Shadow
    private boolean debugView;
    @Shadow
    private boolean renderHand;
    @Shadow
    private float thirdPersonDistancePrev;
    @Shadow
    private int debugViewDirection;
    @Shadow
    private int rendererUpdateCount;
    @Shadow
    private double cameraYaw;
    @Shadow
    private double cameraZoom;
    @Shadow
    private double cameraPitch;

    @Shadow
    protected abstract void setupCameraTransform(float partialTicks, int pass);

    @Shadow
    protected abstract void updateFogColor(float partialTicks);

    @Shadow
    protected abstract boolean isDrawBlockOutline();

    @Shadow
    protected abstract float getFOVModifier(float partialTicks, boolean useFOVSetting);

    @Shadow
    protected abstract void setupFog(int startCoords, float partialTicks);

    @Shadow
    protected abstract void renderCloudsCheck(
        RenderGlobal renderGlobalIn,
        float partialTicks,
        int pass,
        double x,
        double y,
        double z
    );

    @Shadow
    public abstract void disableLightmap();

    @Shadow
    public abstract void enableLightmap();

    @Shadow
    protected abstract void renderRainSnow(float partialTicks);

    @Shadow
    protected abstract void renderHand(float partialTicks, int pass);

    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    private void setupCameraTransform$Inject$RETURN(float partialTicks, int pass, CallbackInfo ci) {
        org.joml.Matrix4f projection = new org.joml.Matrix4f();
        org.joml.Matrix4f modelView = new org.joml.Matrix4f();

        setupCameraTransform0(partialTicks, pass, projection, modelView);

        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        worldRenderer.updateMatrix(projection, modelView);

        Entity entity = this.mc.getRenderViewEntity();
        if (entity != null) {
            worldRenderer.updateRenderPos(
                MathUtils.lerp(entity.prevPosX, entity.posX, partialTicks),
                MathUtils.lerp(entity.prevPosY, entity.posY, partialTicks),
                MathUtils.lerp(entity.prevPosZ, entity.posZ, partialTicks)
            );

            float yaw = MathUtils.lerp(entity.prevRotationYaw, entity.rotationYaw, partialTicks);
            float pitch = MathUtils.lerp(entity.prevRotationPitch, entity.rotationPitch, partialTicks);

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
                BlockPos blockpos = new BlockPos(entity);
                IBlockState iblockstate = this.mc.world.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if (block.isBed(iblockstate, this.mc.world, blockpos, entity)) {
                    yaw += block.getBedDirection(iblockstate, this.mc.world, blockpos).getHorizontalIndex() * 90.0f;
                }
            } else if (this.mc.gameSettings.thirdPersonView == 2) {
                yaw += 180.0f;
            }

            worldRenderer.updateCameraRotation(yaw, pitch);
        }
    }

    public void setupCameraTransform0(
        float partialTicks,
        int pass,
        org.joml.Matrix4f projection,
        org.joml.Matrix4f modelView
    ) {
        this.farPlaneDistance = (float) (this.mc.gameSettings.renderDistanceChunks * 16);
        projection.identity();

        if (this.mc.gameSettings.anaglyph) {
            projection.translate((float) (-(pass * 2 - 1)) * 0.07f, 0.0f, 0.0f);
        }

        if (this.cameraZoom != 1.0D) {
            projection.translate((float) this.cameraYaw, (float) (-this.cameraPitch), 0.0f);
            projection.scale((float) this.cameraZoom, (float) this.cameraZoom, 1.0f);
        }

        projection.perspective(
            MathUtilsKt.toRadians(this.getFOVModifier(partialTicks, true)),
            (float) this.mc.displayWidth / (float) this.mc.displayHeight,
            0.05f,
            this.farPlaneDistance * MathHelper.SQRT_2
        );

        modelView.identity();

        if (this.mc.gameSettings.anaglyph) {
            modelView.translate((float) (pass * 2 - 1) * 0.1f, 0.0f, 0.0f);
        }

        this.hurtCameraEffect0(partialTicks, modelView);

        if (this.mc.gameSettings.viewBobbing) {
            this.applyBobbing0(partialTicks, modelView);
        }

        float f1 = this.mc.player.prevTimeInPortal + (this.mc.player.timeInPortal - this.mc.player.prevTimeInPortal) * partialTicks;

        if (f1 > 0.0f) {
            int i = 20;

            if (this.mc.player.isPotionActive(MobEffects.NAUSEA)) {
                i = 7;
            }

            float f2 = 5.0f / (f1 * f1 + 5.0f) - f1 * 0.04f;
            f2 = f2 * f2;
            modelView.rotate(
                MathUtilsKt.toRadians(((float) this.rendererUpdateCount + partialTicks) * (float) i),
                0.0f,
                1.0f,
                1.0f
            );
            modelView.scale(1.0f / f2, 1.0f, 1.0f);
            modelView.rotate(
                MathUtilsKt.toRadians(-((float) this.rendererUpdateCount + partialTicks) * (float) i),
                0.0f,
                1.0f,
                1.0f
            );
        }

        this.orientCamera0(partialTicks, modelView);

        if (this.debugView) {
            switch (this.debugViewDirection) {
                case 0:
                    modelView.rotate(MathUtilsKt.toRadians(90.0f), 0.0f, 1.0f, 0.0f);
                    break;
                case 1:
                    modelView.rotate(MathUtilsKt.toRadians(180.0f), 0.0f, 1.0f, 0.0f);
                    break;
                case 2:
                    modelView.rotate(MathUtilsKt.toRadians(-90.0f), 0.0f, 1.0f, 0.0f);
                    break;
                case 3:
                    modelView.rotate(MathUtilsKt.toRadians(90.0f), 1.0f, 0.0f, 0.0f);
                    break;
                case 4:
                    modelView.rotate(MathUtilsKt.toRadians(-90.0f), 1.0f, 0.0f, 0.0f);
                    break;
            }
        }
    }

    public void hurtCameraEffect0(float partialTicks, org.joml.Matrix4f modelView) {
        if (this.mc.getRenderViewEntity() instanceof EntityLivingBase) {
            EntityLivingBase entitylivingbase = (EntityLivingBase) this.mc.getRenderViewEntity();
            float f = (float) entitylivingbase.hurtTime - partialTicks;

            if (entitylivingbase.getHealth() <= 0.0F) {
                float f1 = (float) entitylivingbase.deathTime + partialTicks;
                modelView.rotate(MathUtilsKt.toRadians(40.0F - 8000.0F / (f1 + 200.0F)), 0.0F, 0.0F, 1.0F);
            }

            if (f < 0.0F) {
                return;
            }

            f = f / (float) entitylivingbase.maxHurtTime;
            f = MathHelper.sin(f * f * f * f * (float) Math.PI);
            float f2 = entitylivingbase.attackedAtYaw;
            modelView.rotate(MathUtilsKt.toRadians(-f2), 0.0F, 1.0F, 0.0F);
            modelView.rotate(MathUtilsKt.toRadians(-f * 14.0F), 0.0F, 0.0F, 1.0F);
            modelView.rotate(MathUtilsKt.toRadians(f2), 0.0F, 1.0F, 0.0F);
        }
    }

    public void applyBobbing0(float partialTicks, org.joml.Matrix4f modelView) {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) this.mc.getRenderViewEntity();
            float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
            float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
            float f2 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
            float f3 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
            modelView.translate(
                MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F,
                -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2),
                0.0F
            );
            modelView.rotate(MathUtilsKt.toRadians(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F), 0.0F, 0.0F, 1.0F);
            modelView.rotate(
                MathUtilsKt.toRadians(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F),
                1.0F,
                0.0F,
                0.0F
            );
            modelView.rotate(MathUtilsKt.toRadians(f3), 1.0F, 0.0F, 0.0F);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void orientCamera0(float partialTicks, org.joml.Matrix4f modelView) {
        Entity entity = this.mc.getRenderViewEntity();
        float f = entity.getEyeHeight();
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;

        if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
            f = (float) ((double) f + 1.0D);
            modelView.translate(0.0F, 0.3F, 0.0F);

            if (!this.mc.gameSettings.debugCamEnable) {
                BlockPos blockpos = new BlockPos(entity);
                IBlockState iblockstate = this.mc.world.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if (block.isBed(iblockstate, this.mc.world, blockpos, entity)) {
                    modelView.rotate(
                        MathUtilsKt.toRadians(block.getBedDirection(
                            iblockstate,
                            this.mc.world,
                            blockpos
                        ).getHorizontalIndex() * 90.0f),
                        0.0F,
                        1.0F,
                        0.0F
                    );
                }

                modelView.rotate(
                    MathUtilsKt.toRadians(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F),
                    0.0F,
                    -1.0F,
                    0.0F
                );
                modelView.rotate(
                    MathUtilsKt.toRadians(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks),
                    -1.0F,
                    0.0F,
                    0.0F
                );
            }
        } else if (this.mc.gameSettings.thirdPersonView > 0) {
            double d3 = this.thirdPersonDistancePrev + (4.0F - this.thirdPersonDistancePrev) * partialTicks;

            if (this.mc.gameSettings.debugCamEnable) {
                modelView.translate(0.0F, 0.0F, (float) (-d3));
            } else {
                float f1 = entity.rotationYaw;
                float f2 = entity.rotationPitch;

                if (this.mc.gameSettings.thirdPersonView == 2) {
                    f2 += 180.0F;
                }

                double d4 = (double) (-MathHelper.sin(f1 * 0.017453292F) * MathHelper.cos(f2 * 0.017453292F)) * d3;
                double d5 = (double) (MathHelper.cos(f1 * 0.017453292F) * MathHelper.cos(f2 * 0.017453292F)) * d3;
                double d6 = (double) (-MathHelper.sin(f2 * 0.017453292F)) * d3;

                for (int i = 0; i < 8; ++i) {
                    float f3 = (float) ((i & 1) * 2 - 1);
                    float f4 = (float) ((i >> 1 & 1) * 2 - 1);
                    float f5 = (float) ((i >> 2 & 1) * 2 - 1);
                    f3 = f3 * 0.1F;
                    f4 = f4 * 0.1F;
                    f5 = f5 * 0.1F;
                    RayTraceResult raytraceresult = this.mc.world.rayTraceBlocks(new Vec3d(
                        d0 + (double) f3,
                        d1 + (double) f4,
                        d2 + (double) f5
                    ), new Vec3d(d0 - d4 + (double) f3 + (double) f5, d1 - d6 + (double) f4, d2 - d5 + (double) f5));

                    if (raytraceresult != null) {
                        double d7 = raytraceresult.hitVec.distanceTo(new Vec3d(d0, d1, d2));

                        if (d7 < d3) {
                            d3 = d7;
                        }
                    }
                }

                if (this.mc.gameSettings.thirdPersonView == 2) {
                    modelView.rotate(MathUtilsKt.toRadians(180.0F), 0.0F, 1.0F, 0.0F);
                }

                modelView.rotate(MathUtilsKt.toRadians(entity.rotationPitch - f2), 1.0F, 0.0F, 0.0F);
                modelView.rotate(MathUtilsKt.toRadians(entity.rotationYaw - f1), 0.0F, 1.0F, 0.0F);
                modelView.translate(0.0F, 0.0F, (float) (-d3));
                modelView.rotate(MathUtilsKt.toRadians(f1 - entity.rotationYaw), 0.0F, 1.0F, 0.0F);
                modelView.rotate(MathUtilsKt.toRadians(f2 - entity.rotationPitch), 1.0F, 0.0F, 0.0F);
            }
        } else {
            modelView.translate(0.0F, 0.0F, 0.05F);
        }

        if (!this.mc.gameSettings.debugCamEnable) {
            float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F;
            float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            float roll = 0.0F;
            if (entity instanceof EntityAnimal) {
                EntityAnimal entityanimal = (EntityAnimal) entity;
                yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F;
            }
            IBlockState state = ActiveRenderInfo.getBlockStateAtEntityViewpoint(this.mc.world, entity, partialTicks);
            net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup event = new net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup(
                (EntityRenderer) (Object) this,
                entity,
                state,
                partialTicks,
                yaw,
                pitch,
                roll
            );
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            modelView.rotate(MathUtilsKt.toRadians(event.getRoll()), 0.0F, 0.0F, 1.0F);
            modelView.rotate(MathUtilsKt.toRadians(event.getPitch()), 1.0F, 0.0F, 0.0F);
            modelView.rotate(MathUtilsKt.toRadians(event.getYaw()), 0.0F, 1.0F, 0.0F);
        }

        modelView.translate(0.0F, -f, 0.0F);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    public void updateCameraAndRender$Inject$INVOKE$renderGameOverlay(
        float partialTicks,
        long nanoTime,
        CallbackInfo ci
    ) {
        if (mc.gameSettings.showDebugInfo) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        FpsDisplay.INSTANCE.render(
            resolution.getScaledWidth(),
            resolution.getScaledHeight(),
            FastMcMod.INSTANCE.getFontRenderer().getWrapped()
        );
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano) {
        RenderGlobal renderGlobal = this.mc.renderGlobal;
        ParticleManager particleManager = this.mc.effectRenderer;
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        TerrainRenderer terrainRenderer = worldRenderer.getTerrainRenderer();
        TerrainFogManager fogManager = terrainRenderer.getFogManager();

        boolean drawBlockOutline = this.isDrawBlockOutline();
        Entity viewEntity = this.mc.getRenderViewEntity();
        assert viewEntity != null;
        GlStateManager.enableCull();

        this.mc.profiler.endStartSection("clear");
        GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
        this.updateFogColor(partialTicks);
        GlStateManager.clear(16640);

        this.mc.profiler.endStartSection("camera");
        this.setupCameraTransform(partialTicks, pass);
        ActiveRenderInfo.updateRenderInfo(
            viewEntity,
            this.mc.gameSettings.thirdPersonView == 2
        ); //Forge: MC-46445 Spectator mode particles and sounds computed from where you have been before
        Vec3d cameraPosition = ActiveRenderInfo.projectViewFromEntity(viewEntity, partialTicks);
        worldRenderer.updateCameraPos(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        worldRenderer.updateFrustum();

        this.mc.profiler.endStartSection("frustum");
        ClippingHelperImpl.getInstance();

        this.mc.profiler.endStartSection("culling");
        ICamera camera = new Frustum();
        double renderPosX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * (double) partialTicks;
        double renderPosY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * (double) partialTicks;
        double renderPosZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * (double) partialTicks;
        camera.setPosition(renderPosX, renderPosY, renderPosZ);

        if (this.mc.gameSettings.renderDistanceChunks >= 4) {
            this.mc.profiler.endStartSection("sky");
            this.setupFog(-1, partialTicks);
            float fovModifier = this.getFOVModifier(partialTicks, true);

            GlStateManager.matrixMode(GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            Project.gluPerspective(
                fovModifier,
                (float) this.mc.displayWidth / (float) this.mc.displayHeight,
                0.05F,
                this.farPlaneDistance * 2.0F
            );
            GlStateManager.matrixMode(GL_MODELVIEW);
            GlStateManager.pushMatrix();
            renderGlobal.renderSky(partialTicks, pass);

            GlStateManager.matrixMode(GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL_MODELVIEW);
            GlStateManager.popMatrix();
        }

        this.setupFog(0, partialTicks);
        GlStateManager.shadeModel(GL_SMOOTH);

        if (viewEntity.posY + (double) viewEntity.getEyeHeight() < 128.0D) {
            this.mc.profiler.endStartSection("belowCloud");
            this.renderCloudsCheck(renderGlobal, partialTicks, pass, renderPosX, renderPosY, renderPosZ);
        }

        this.mc.profiler.endStartSection("fog");
        this.setupFog(0, partialTicks);
        setupTerrainFog(partialTicks);

        this.mc.profiler.endStartSection("updateTerrain");
        if (pass == 0 || pass == 2) {
            terrainRenderer.update();
        }

        this.mc.profiler.endStartSection("terrain");
        RenderHelper.disableStandardItemLighting();
        this.enableLightmap();
        ITextureObject blockTexture = this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.bindTexture(blockTexture.getGlTextureId());
        GlStateManager.disableAlpha();

        TerrainFogManager.ShaderProgram shader = fogManager.getShader();
        shader.bind();

        this.mc.profiler.startSection("solid");
        terrainRenderer.renderLayer(0);

        fogManager.alphaTest(true);
        shader = fogManager.getShader();
        shader.bind();

        this.mc.profiler.endStartSection("cutoutMipped");
        blockTexture.setBlurMipmap(false, this.mc.gameSettings.mipmapLevels > 0);
        terrainRenderer.renderLayer(1);

        this.mc.profiler.endStartSection("cutout");
        blockTexture.restoreLastBlurMipmap();
        blockTexture.setBlurMipmap(false, false);
        terrainRenderer.renderLayer(2);
        blockTexture.restoreLastBlurMipmap();

        fogManager.alphaTest(false);
        shader = fogManager.getShader();

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        glBindVertexArray(0);
        GlStateManager.enableAlpha();
        shader.unbind();
        this.mc.profiler.endSection();

        GlStateManager.shadeModel(GL_FLAT);

        if (!this.debugView) {
            RenderHelper.enableStandardItemLighting();
            net.minecraftforge.client.ForgeHooksClient.setRenderPass(0);
            renderGlobal.renderEntities(viewEntity, camera, partialTicks);
            net.minecraftforge.client.ForgeHooksClient.setRenderPass(0);
            RenderHelper.disableStandardItemLighting();
            this.disableLightmap();
        }

        if (drawBlockOutline && this.mc.objectMouseOver != null && !viewEntity.isInsideOfMaterial(Material.WATER)) {
            EntityPlayer entityplayer = (EntityPlayer) viewEntity;
            GlStateManager.disableAlpha();
            this.mc.profiler.endStartSection("outline");
            if (!net.minecraftforge.client.ForgeHooksClient.onDrawBlockHighlight(
                renderGlobal,
                entityplayer,
                mc.objectMouseOver,
                0,
                partialTicks
            ))
                renderGlobal.drawSelectionBox(entityplayer, this.mc.objectMouseOver, 0, partialTicks);
            GlStateManager.enableAlpha();
        }

        if (this.mc.debugRenderer.shouldRender()) {
            this.mc.debugRenderer.renderDebug(partialTicks, finishTimeNano);
        }

        this.mc.profiler.endStartSection("destroyProgress");
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        blockTexture.setBlurMipmap(false, false);
        renderGlobal.drawBlockDamageTexture(
            Tessellator.getInstance(),
            Tessellator.getInstance().getBuffer(),
            viewEntity,
            partialTicks
        );
        blockTexture.restoreLastBlurMipmap();
        GlStateManager.disableBlend();

        if (!this.debugView) {

            this.mc.profiler.endStartSection("litParticles");
            this.enableLightmap();
            particleManager.renderLitParticles(viewEntity, partialTicks);
            RenderHelper.disableStandardItemLighting();
            this.setupFog(0, partialTicks);

            this.mc.profiler.endStartSection("particles");
            particleManager.renderParticles(viewEntity, partialTicks);
            this.disableLightmap();
        }

        this.mc.profiler.endStartSection("weather");
        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        this.renderRainSnow(partialTicks);
        ;

        this.mc.profiler.endStartSection("worldBorder");
        renderGlobal.renderWorldBorder(viewEntity, partialTicks);

        this.mc.profiler.endStartSection("terrain");
        this.mc.profiler.startSection("translucent");
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );

        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL_LEQUAL);

        shader.bind();
        terrainRenderer.renderLayer(3);
        shader.unbind();

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        glBindVertexArray(0);
        this.mc.profiler.endSection();

        if (!this.debugView) {
            RenderHelper.enableStandardItemLighting();
            net.minecraftforge.client.ForgeHooksClient.setRenderPass(1);
            renderGlobal.renderEntities(viewEntity, camera, partialTicks);
            // restore blending function changed by RenderGlobal.preRenderDamagedBlocks
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            );
            net.minecraftforge.client.ForgeHooksClient.setRenderPass(-1);
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.shadeModel(GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();

        if (viewEntity.posY + (double) viewEntity.getEyeHeight() >= 128.0D) {
            this.mc.profiler.endStartSection("aboveClouds");
            this.renderCloudsCheck(renderGlobal, partialTicks, pass, renderPosX, renderPosY, renderPosZ);
        }

        this.mc.profiler.endStartSection("forgeRenderLast");
        net.minecraftforge.client.ForgeHooksClient.dispatchRenderLast(renderGlobal, partialTicks);

        this.mc.profiler.endStartSection("hand");
        if (this.renderHand) {
            GlStateManager.clear(256);
            this.renderHand(partialTicks, pass);
        }
    }

    private void setupTerrainFog(float partialTicks) {
        EntityRenderer entityRenderer = (EntityRenderer) (Object) this;
        TerrainFogManager fogManager = FastMcMod.INSTANCE.getWorldRenderer().getTerrainRenderer().getFogManager();
        float red = this.fogColorRed;
        float green = this.fogColorGreen;
        float blue = this.fogColorBlue;

        Entity viewEntity = this.mc.getRenderViewEntity();
        assert viewEntity != null;

        IBlockState blockState = ActiveRenderInfo.getBlockStateAtEntityViewpoint(
            this.mc.world,
            viewEntity,
            partialTicks
        );

        float eventDensity = ForgeHooksClient.getFogDensity(
            entityRenderer,
            viewEntity,
            blockState,
            partialTicks,
            0.1f
        );

        if (eventDensity >= 0.0f) {
            fogManager.expFog(eventDensity, red, green, blue);
            return;
        }

        if (viewEntity instanceof EntityLivingBase) {
            PotionEffect blindness = ((EntityLivingBase) viewEntity).getActivePotionEffect(MobEffects.BLINDNESS);
            if (blindness != null) {
                float fogDistance = 5.0f;
                int duration = blindness.getDuration();
                if (duration < 20) {
                    fogDistance = 5.0f + (this.farPlaneDistance - 5.0f) * (1.0f - (float) duration / 20.0f);
                }
                fogManager.linearFog(fogDistance * 0.25f, fogDistance, red, green, blue);
                return;
            }
        }

        if (this.cloudFog) {
            GlStateManager.setFog(GlStateManager.FogMode.EXP);
            GlStateManager.setFogDensity(0.1f);
            fogManager.expFog(0.1f, red, green, blue);
        } else if (blockState.getMaterial() == Material.WATER) {
            GlStateManager.setFog(GlStateManager.FogMode.EXP);

            float density;

            if (viewEntity instanceof EntityLivingBase) {
                if (((EntityLivingBase) viewEntity).isPotionActive(MobEffects.WATER_BREATHING)) {
                    density = 0.01f;
                } else {
                    density = 0.1f - (float) EnchantmentHelper.getRespirationModifier((EntityLivingBase) viewEntity) * 0.03f;
                }
            } else {
                density = 0.1f;
            }

            fogManager.expFog(density, red, green, blue);
        } else if (blockState.getMaterial() == Material.LAVA) {
            fogManager.expFog(2.0f, red, green, blue);
        } else {
            float viewDistance = this.farPlaneDistance;
            float start;
            float end;

            if (this.mc.world.provider.doesXZShowFog((int) viewEntity.posX, (int) viewEntity.posZ)
                || this.mc.ingameGUI.getBossOverlay().shouldCreateFog()) {
                start = viewDistance * 0.05f;
                end = Math.min(viewDistance, 192.0f) * 0.5f;
            } else {
                start = viewDistance * 0.75f;
                end = viewDistance;
            }

            fogManager.linearFog(start, end, red, green, blue);

            ForgeHooksClient.onFogRender(
                entityRenderer,
                viewEntity,
                blockState,
                partialTicks,
                0,
                viewDistance
            );
        }
    }
}
