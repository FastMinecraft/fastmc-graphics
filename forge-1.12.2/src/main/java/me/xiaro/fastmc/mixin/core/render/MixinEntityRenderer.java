package me.xiaro.fastmc.mixin.core.render;

import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.shared.util.MatrixUtils;
import net.minecraft.client.renderer.EntityRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    private void setupCameraTransform$Inject$RETURN(float partialTicks, int pass, CallbackInfo ci) {
        glGetFloat(GL_PROJECTION_MATRIX, MatrixUtils.INSTANCE.getMatrixBuffer());
        Matrix4f projection = MatrixUtils.INSTANCE.getMatrix();

        glGetFloat(GL_MODELVIEW_MATRIX, MatrixUtils.INSTANCE.getMatrixBuffer());
        Matrix4f modelView = MatrixUtils.INSTANCE.getMatrix();

        FastMcMod.INSTANCE.getWorldRenderer().setupCamera(projection, modelView);
    }
}
