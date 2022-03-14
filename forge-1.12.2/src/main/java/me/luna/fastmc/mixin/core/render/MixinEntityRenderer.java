package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.shared.FpsDisplay;
import me.luna.fastmc.shared.util.MathUtilsKt;
import me.luna.fastmc.shared.util.MatrixUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private float farPlaneDistance;

    @Shadow
    private double cameraZoom;

    @Shadow
    private double cameraYaw;

    @Shadow
    private double cameraPitch;

    @Shadow
    protected abstract float getFOVModifier(float partialTicks, boolean useFOVSetting);

    @Shadow
    private int rendererUpdateCount;

    @Shadow
    private boolean debugView;

    @Shadow
    private int debugViewDirection;

    @Shadow
    private float thirdPersonDistancePrev;

    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    private void setupCameraTransform$Inject$RETURN(float partialTicks, int pass, CallbackInfo ci) {
        Matrix4f projection = new Matrix4f();
        Matrix4f modelView = new Matrix4f();

        setupCameraTransform0(partialTicks, pass, projection, modelView);

        FastMcMod.INSTANCE.getWorldRenderer().setupCamera(projection, modelView);
    }

    public void setupCameraTransform0(float partialTicks, int pass, Matrix4f projection, @NotNull Matrix4f modelView) {
        this.farPlaneDistance = (float) (this.mc.gameSettings.renderDistanceChunks * 16);
        projection.identity();

        if (this.mc.gameSettings.anaglyph) {
            projection.translate((float) (-(pass * 2 - 1)) * 0.07f, 0.0f, 0.0f);
        }

        if (this.cameraZoom != 1.0D) {
            projection.translate((float) this.cameraYaw, (float) (-this.cameraPitch), 0.0f);
            projection.scale((float) this.cameraZoom, (float) this.cameraZoom, 1.0f);
        }

        projection.perspective(MathUtilsKt.toRadians(this.getFOVModifier(partialTicks, true)), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05f, this.farPlaneDistance * MathHelper.SQRT_2);

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
            modelView.rotate(MathUtilsKt.toRadians(((float) this.rendererUpdateCount + partialTicks) * (float) i), 0.0f, 1.0f, 1.0f);
            modelView.scale(1.0f / f2, 1.0f, 1.0f);
            modelView.rotate(MathUtilsKt.toRadians(-((float) this.rendererUpdateCount + partialTicks) * (float) i), 0.0f, 1.0f, 1.0f);
        }

        this.orientCamera0(partialTicks, modelView);

        if (this.debugView) {
            switch (this.debugViewDirection) {
                case 0:
//                    GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f);
                    modelView.rotate(MathUtilsKt.toRadians(90.0f), 0.0f, 1.0f, 0.0f);
                    break;
                case 1:
//                    GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
                    modelView.rotate(MathUtilsKt.toRadians(180.0f), 0.0f, 1.0f, 0.0f);
                    break;
                case 2:
//                    GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f);
                    modelView.rotate(MathUtilsKt.toRadians(-90.0f), 0.0f, 1.0f, 0.0f);
                    break;
                case 3:
//                    GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f);
                    modelView.rotate(MathUtilsKt.toRadians(90.0f), 1.0f, 0.0f, 0.0f);
                    break;
                case 4:
//                    GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f);
                    modelView.rotate(MathUtilsKt.toRadians(-90.0f), 1.0f, 0.0f, 0.0f);
                    break;
            }
        }
    }

    public void hurtCameraEffect0(float partialTicks, Matrix4f modelView) {
        if (this.mc.getRenderViewEntity() instanceof EntityLivingBase) {
            EntityLivingBase entitylivingbase = (EntityLivingBase) this.mc.getRenderViewEntity();
            float f = (float) entitylivingbase.hurtTime - partialTicks;

            if (entitylivingbase.getHealth() <= 0.0F) {
                float f1 = (float) entitylivingbase.deathTime + partialTicks;
//                GlStateManager.rotate(40.0F - 8000.0F / (f1 + 200.0F), 0.0F, 0.0F, 1.0F);
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

    public void applyBobbing0(float partialTicks, Matrix4f modelView) {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) this.mc.getRenderViewEntity();
            float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
            float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
            float f2 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
            float f3 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
            modelView.translate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
            modelView.rotate(MathUtilsKt.toRadians(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F), 0.0F, 0.0F, 1.0F);
            modelView.rotate(MathUtilsKt.toRadians(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F), 1.0F, 0.0F, 0.0F);
            modelView.rotate(MathUtilsKt.toRadians(f3), 1.0F, 0.0F, 0.0F);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void orientCamera0(float partialTicks, Matrix4f modelView) {
        Entity entity = this.mc.getRenderViewEntity();
        float f = entity.getEyeHeight();
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;

        if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
            f = (float) ((double) f + 1.0D);
//            GlStateManager.translate(0.0F, 0.3F, 0.0F);
            modelView.translate(0.0F, 0.3F, 0.0F);

            if (!this.mc.gameSettings.debugCamEnable) {
                BlockPos blockpos = new BlockPos(entity);
                IBlockState iblockstate = this.mc.world.getBlockState(blockpos);
                net.minecraftforge.client.ForgeHooksClient.orientBedCamera(this.mc.world, blockpos, iblockstate, entity);

                modelView.rotate(MathUtilsKt.toRadians(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F), 0.0F, -1.0F, 0.0F);
                modelView.rotate(MathUtilsKt.toRadians(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks), -1.0F, 0.0F, 0.0F);
            }
        } else if (this.mc.gameSettings.thirdPersonView > 0) {
            double d3 = this.thirdPersonDistancePrev + (4.0F - this.thirdPersonDistancePrev) * partialTicks;

            if (this.mc.gameSettings.debugCamEnable) {
//                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
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
                    RayTraceResult raytraceresult = this.mc.world.rayTraceBlocks(new Vec3d(d0 + (double) f3, d1 + (double) f4, d2 + (double) f5), new Vec3d(d0 - d4 + (double) f3 + (double) f5, d1 - d6 + (double) f4, d2 - d5 + (double) f5));

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
//            GlStateManager.translate(0.0F, 0.0F, 0.05F);
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
            net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup event = new net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup((EntityRenderer) (Object) this, entity, state, partialTicks, yaw, pitch, roll);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            modelView.rotate(MathUtilsKt.toRadians(event.getRoll()), 0.0F, 0.0F, 1.0F);
            modelView.rotate(MathUtilsKt.toRadians(event.getPitch()), 1.0F, 0.0F, 0.0F);
            modelView.rotate(MathUtilsKt.toRadians(event.getYaw()), 0.0F, 1.0F, 0.0F);
        }

//        GlStateManager.translate(0.0F, -f, 0.0F);
        modelView.translate(0.0F, -f, 0.0F);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    public void updateCameraAndRender$Inject$INVOKE$renderGameOverlay(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (mc.gameSettings.showDebugInfo) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        FpsDisplay.INSTANCE.render(resolution.getScaledWidth(), resolution.getScaledHeight(), FastMcMod.INSTANCE.getFontRenderer().getWrapped());
    }
}
