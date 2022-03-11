package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.shared.FpsDisplay;
import me.luna.fastmc.shared.util.MatrixUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.EntityRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow @Final private Minecraft mc;

    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    private void setupCameraTransform$Inject$RETURN(float partialTicks, int pass, CallbackInfo ci) {
        glGetFloat(GL_PROJECTION_MATRIX, MatrixUtils.INSTANCE.getMatrixBuffer());
        Matrix4f projection = MatrixUtils.INSTANCE.getMatrix();

        glGetFloat(GL_MODELVIEW_MATRIX, MatrixUtils.INSTANCE.getMatrixBuffer());
        Matrix4f modelView = MatrixUtils.INSTANCE.getMatrix();

        FastMcMod.INSTANCE.getWorldRenderer().setupCamera(projection, modelView);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    public void updateCameraAndRender$Inject$INVOKE$renderGameOverlay(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (mc.world == null || mc.player == null|| mc.gameSettings.showDebugInfo) return;

        ScaledResolution resolution = new ScaledResolution(mc);
        FpsDisplay.INSTANCE.render(resolution.getScaledWidth(), resolution.getScaledHeight(), FastMcMod.INSTANCE.getFontRenderer().getWrapped());
    }
}
