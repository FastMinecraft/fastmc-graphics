package me.xiaro.fastmc.mixin.render;

import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.utils.MathUtilsKt;
import me.xiaro.fastmc.utils.MatrixUtils;
import net.minecraft.client.gui.FontRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.opengl.GL11.*;

@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {
    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void drawString$Inject$HEAD(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        handleDrawString(text, x, y, color, dropShadow, cir);
    }

    @Inject(method = "renderString", at = @At("HEAD"), cancellable = true)
    private void renderString$Inject$HEAD(String text, float x, float y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        handleDrawString(text, x, y, color, false, cir);
    }

    private void handleDrawString(String text, float x, float y, int color, boolean drawShadow, CallbackInfoReturnable<Integer> cir) {
        if (FastMcMod.INSTANCE.isInitialized()) {
            glGetFloat(GL_PROJECTION_MATRIX, MatrixUtils.INSTANCE.getMatrixBuffer());
            Matrix4f projection = MatrixUtils.INSTANCE.getMatrix();

            glGetFloat(GL_MODELVIEW_MATRIX, MatrixUtils.INSTANCE.getMatrixBuffer());
            Matrix4f modelView = MatrixUtils.INSTANCE.getMatrix();

            FastMcMod.INSTANCE.getFontRenderer().drawString(projection, modelView, text, x, y, color, 1.0f, drawShadow);
            cir.setReturnValue(MathUtilsKt.fastCeil(x + FastMcMod.INSTANCE.getFontRenderer().getWrapped().getWidth(text)));
        }
    }

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    public void getStringWidth$Inject$HEAD(String text, CallbackInfoReturnable<Integer> cir) {
        if (FastMcMod.INSTANCE.isInitialized()) {
            cir.setReturnValue(MathUtilsKt.fastCeil(FastMcMod.INSTANCE.getFontRenderer().getWrapped().getWidth(text)));
        }
    }

    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true)
    public void getCharWidth$Inject$HEAD(char character, CallbackInfoReturnable<Integer> cir) {
        if (FastMcMod.INSTANCE.isInitialized()) {
            cir.setReturnValue(MathUtilsKt.fastCeil(FastMcMod.INSTANCE.getFontRenderer().getWrapped().getWidth(character)));
        }
    }

    @Inject(method = "setUnicodeFlag", at = @At("HEAD"))
    public void setUnicodeFlag$Inject$HEAD(boolean unicodeFlagIn, CallbackInfo ci) {
        if (FastMcMod.INSTANCE.isInitialized()) {
            FastMcMod.INSTANCE.getFontRenderer().getWrapped().setUnicode(unicodeFlagIn);
        }
    }
}
