package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.common.MathUtil;
import dev.fastmc.common.MathUtilKt;
import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.mixin.FixedFunctionMatrixStacks;
import dev.fastmc.graphics.shared.FpsDisplay;
import dev.fastmc.graphics.shared.mixin.ICoreWorldRenderer;
import dev.fastmc.graphics.shared.renderer.WorldRenderer;
import dev.fastmc.graphics.shared.terrain.TerrainShaderManager;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinCoreEntityRenderer implements ICoreWorldRenderer {
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
    private boolean debugView;
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
    @Final
    private ResourceLocation locationLightMap;

    @Shadow
    protected abstract float getFOVModifier(float partialTicks, boolean useFOVSetting);

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void Inject$setupCameraTransform$INVOKE$Project$gluPerspective(
        float partialTicks,
        int pass,
        CallbackInfo ci
    ) {
        FixedFunctionMatrixStacks.perspective(
            MathUtilKt.toRadians(this.getFOVModifier(partialTicks, true)),
            (float) this.mc.displayWidth / (float) this.mc.displayHeight,
            0.05f,
            this.farPlaneDistance * MathHelper.SQRT_2
        );
    }

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void Inject$renderHand$INVOKE$Project$gluPerspective(float partialTicks, int pass, CallbackInfo ci) {
        FixedFunctionMatrixStacks.perspective(
            MathUtilKt.toRadians(this.getFOVModifier(partialTicks, true)),
            (float) this.mc.displayWidth / (float) this.mc.displayHeight,
            0.05f,
            this.farPlaneDistance * 2.0f
        );
    }

    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    private void setupCameraTransform$Inject$RETURN(float partialTicks, int pass, CallbackInfo ci) {
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        worldRenderer.updateMatrix(
            new Matrix4f(FixedFunctionMatrixStacks.PROJECTION),
            new Matrix4f(FixedFunctionMatrixStacks.MODELVIEW)
        );

        Entity entity = this.mc.getRenderViewEntity();
        if (entity != null) {
            worldRenderer.updateRenderPos(
                MathUtil.lerp(entity.prevPosX, entity.posX, partialTicks),
                MathUtil.lerp(entity.prevPosY, entity.posY, partialTicks),
                MathUtil.lerp(entity.prevPosZ, entity.posZ, partialTicks)
            );

            float yaw = MathUtil.lerp(entity.prevRotationYaw, entity.rotationYaw, partialTicks);
            float pitch = MathUtil.lerp(entity.prevRotationPitch, entity.rotationPitch, partialTicks);

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

        worldRenderer.updateScreenSize(mc.displayWidth, mc.displayHeight);
        worldRenderer.updateGlobalUBO(partialTicks);
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

    @Unique
    private int currentPass = 0;

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;updateRenderInfo(Lnet/minecraft/entity/Entity;Z)V", shift = At.Shift.AFTER, remap = false))
    private void Inject$renderWorldPass$INVOKE$ActiveRenderInfo$updateRenderInfo$AFTER(
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        currentPass = pass;

        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        Entity viewEntity = this.mc.getRenderViewEntity();
        assert viewEntity != null;

        Vec3d cameraPosition = ActiveRenderInfo.projectViewFromEntity(viewEntity, partialTicks);
        worldRenderer.updateCameraPos(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        worldRenderer.updateFrustum();
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderCloudsCheck(Lnet/minecraft/client/renderer/RenderGlobal;FIDDD)V", ordinal = 0))
    private void Inject$renderWorldPass$INVOKE$EntityRenderer$renderCloudsCheck(
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        this.mc.profiler.endStartSection("clouds");
    }

    @Inject(method = "renderCloudsCheck", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false, ordinal = 0))
    private void Inject$renderCloudsCheck$INVOKE$Project$gluPerspective$0(
        RenderGlobal renderGlobalIn,
        float partialTicks,
        int pass,
        double x,
        double y,
        double z,
        CallbackInfo ci
    ) {
        FixedFunctionMatrixStacks.perspective(
            MathUtilKt.toRadians(this.getFOVModifier(partialTicks, true)),
            (float) this.mc.displayWidth / (float) this.mc.displayHeight,
            0.05f,
            this.farPlaneDistance * 4.0f
        );
    }

    @Inject(method = "renderCloudsCheck", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false, ordinal = 1))
    private void Inject$renderCloudsCheck$INVOKE$Project$gluPerspective$1(
        RenderGlobal renderGlobalIn,
        float partialTicks,
        int pass,
        double x,
        double y,
        double z,
        CallbackInfo ci
    ) {
        FixedFunctionMatrixStacks.perspective(
            MathUtilKt.toRadians(this.getFOVModifier(partialTicks, true)),
            (float) this.mc.displayWidth / (float) this.mc.displayHeight,
            0.05f,
            this.farPlaneDistance * MathHelper.SQRT_2
        );
    }


    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;loadIdentity()V", ordinal = 0))
    private void Inject$renderWorldPass$INVOKE$GlStateManager$loadIdentity$0(
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        GlStateManager.pushMatrix();
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(FI)V"))
    private void Inject$renderWorldPass$INVOKE$RenderGlobal$renderSky(
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        GlStateManager.pushMatrix();
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;loadIdentity()V", ordinal = 1))
    private void Redirect$renderWorldPass$INVOKE$RenderGlobal$loadIdentity$1() {

    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", ordinal = 0, remap = false))
    private void Inject$renderWorldPass$INVOKE$Project$gluPerspective$0(
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        FixedFunctionMatrixStacks.perspective(
            MathUtilKt.toRadians(this.getFOVModifier(partialTicks, true)),
            (float) this.mc.displayWidth / (float) this.mc.displayHeight,
            0.05f,
            this.farPlaneDistance * 2.0f
        );
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", ordinal = 1, remap = false))
    private void Redirect$renderWorldPass$INVOKE$Project$gluPerspective$1(
        float fovy,
        float aspect,
        float zNear,
        float zFar
    ) {
        GlStateManager.popMatrix();
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;matrixMode(I)V", ordinal = 3, shift = At.Shift.AFTER))
    private void Inject$renderWorldPass$INVOKE$GlStateManager$matrixMode$3$AFTER(
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        GlStateManager.popMatrix();
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=prepareterrain"))
    private void Redirect$renderWorldPass$INVOKE_STRING$Profiler$endStartSection$prepareterrain(
        Profiler instance,
        String name
    ) {

    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=terrain_setup"))
    private void Redirect$renderWorldPass$INVOKE_STRING$Profiler$endStartSection$terrain_setup(
        Profiler instance,
        String name
    ) {

    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V"))
    private void Redirect$renderWorldPass$INVOKE$RenderGlobal$setupTerrain(
        RenderGlobal instance,
        Entity viewEntity,
        double partialTicks,
        ICamera camera,
        int frameCount,
        boolean playerSpectator
    ) {
        this.mc.profiler.endStartSection("fog");
        setupTerrainFogShader((float) partialTicks);

        this.mc.profiler.endStartSection("updateTerrain");
        getTerrainRenderer().update(currentPass == 0 || currentPass == 2);
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;updateChunks(J)V"))
    private void Redirect$renderWorldPass$INVOKE$RenderGlobal$updateChunks(RenderGlobal instance, long finishTimeNano) {

    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=updatechunks"))
    private void Redirect$renderWorldPass$INVOKE_STRING$Profiler$endStartSection$updatechunks(
        Profiler instance,
        String name
    ) {

    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=translucent"))
    private void Redirect$renderWorldPass$INVOKE_STRING$Profiler$endStartSection$terrain(
        Profiler instance,
        String name
    ) {
        this.mc.profiler.endStartSection("terrain");
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I"))
    private int Redirect$renderWorldPass$INVOKE$RenderGlobal$renderBlockLayer(
        RenderGlobal instance,
        BlockRenderLayer layer,
        double partialTicks,
        int pass,
        Entity entityIn
    ) {
        switch (layer) {
            case SOLID:
                this.mc.profiler.startSection("solid");
                renderLayerPass(0);
                this.mc.profiler.endSection();
                break;
            case TRANSLUCENT:
                this.mc.profiler.startSection("translucent");
                renderLayerPass(1);
                this.mc.profiler.endSection();
                break;
        }
        return 0;
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableAlpha()V", ordinal = 0))
    private void Redirect$renderWorldPass$INVOKE$GlStateManager$enableAlpha$0() {

    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;alphaFunc(IF)V", ordinal = 0))
    private void Inject$renderWorldPass$INVOKE$GlStateManager$alphaFunc$0(
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        GlStateManager.enableAlpha();
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderWorldBorder(Lnet/minecraft/entity/Entity;F)V"))
    private void Inject$renderWorldPass$INVOKE$RenderGlobal$renderWorldBorder(
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        this.mc.profiler.endStartSection("worldBorder");
    }

    @Override
    public void preRenderLayer(int layerIndex) {
        ICoreWorldRenderer.super.preRenderLayer(layerIndex);
        switch (layerIndex) {
            case 0:
                GlStateManager.disableBlend();
                break;
            case 1:
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                );
                GlStateManager.enableDepth();
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                break;
            default:
                throw new IllegalArgumentException("Invalid layer index: " + layerIndex);
        }
    }

    private void setupTerrainFogShader(float partialTicks) {
        EntityRenderer entityRenderer = (EntityRenderer) (Object) this;
        TerrainShaderManager fogManager = FastMcMod.INSTANCE.getWorldRenderer().getTerrainRenderer().getShaderManager();
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
            fogManager.expFog(TerrainShaderManager.FogShape.SPHERE, eventDensity, red, green, blue);
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
                fogManager.linearFog(
                    TerrainShaderManager.FogShape.SPHERE,
                    fogDistance * 0.25f,
                    fogDistance,
                    red,
                    green,
                    blue
                );
                return;
            }
        }

        if (this.cloudFog) {
            GlStateManager.setFog(GlStateManager.FogMode.EXP);
            GlStateManager.setFogDensity(0.1f);
            fogManager.expFog(TerrainShaderManager.FogShape.SPHERE, 0.1f, red, green, blue);
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

            fogManager.expFog(TerrainShaderManager.FogShape.SPHERE, density, red, green, blue);
        } else if (blockState.getMaterial() == Material.LAVA) {
            fogManager.expFog(TerrainShaderManager.FogShape.SPHERE, 2.0f, red, green, blue);
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

            fogManager.linearFog(TerrainShaderManager.FogShape.SPHERE, start, end, red, green, blue);

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

    @Override
    public int getLightMapTexture() {
        return this.mc.getTextureManager().getTexture(this.locationLightMap).getGlTextureId();
    }

    @Override
    public void bindBlockTexture() {
        ITextureObject blockTexture = this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.bindTexture(blockTexture.getGlTextureId());
        blockTexture.setBlurMipmap(false, this.mc.gameSettings.mipmapLevels > 0);
    }
}