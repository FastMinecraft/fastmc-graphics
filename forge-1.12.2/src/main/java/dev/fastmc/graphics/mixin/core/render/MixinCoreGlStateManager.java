package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.graphics.mixin.FixedFunctionMatrixStacks;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.util.vector.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

@Mixin(GlStateManager.class)
public class MixinCoreGlStateManager {
    @Inject(method = "matrixMode", at = @At("HEAD"))
    private static void Inject$matrixMode$HEAD(int mode, CallbackInfo ci) {
        FixedFunctionMatrixStacks.matrixMode(mode);
    }

    @Inject(method = "loadIdentity", at = @At("HEAD"))
    private static void Inject$loadIdentity$HEAD(CallbackInfo ci) {
        FixedFunctionMatrixStacks.loadIdentity();
    }

    @Inject(method = "pushMatrix", at = @At("HEAD"))
    private static void Inject$pushMatrix$HEAD(CallbackInfo ci) {
        FixedFunctionMatrixStacks.pushMatrix();
    }

    @Inject(method = "popMatrix", at = @At("HEAD"))
    private static void Inject$popMatrix$HEAD(CallbackInfo ci) {
        FixedFunctionMatrixStacks.popMatrix();
    }

    @Inject(method = "ortho", at = @At("HEAD"))
    private static void Inject$ortho$HEAD(
        double left,
        double right,
        double bottom,
        double top,
        double zNear,
        double zFar,
        CallbackInfo ci
    ) {
        FixedFunctionMatrixStacks.ortho(left, right, bottom, top, zNear, zFar);
    }

    @Inject(method = "rotate(FFFF)V", at = @At("HEAD"))
    private static void Inject$rotate$HEAD(float angle, float x, float y, float z, CallbackInfo ci) {
        FixedFunctionMatrixStacks.rotate(angle, x, y, z);
    }

    @Inject(method = "scale(FFF)V", at = @At("HEAD"))
    private static void Inject$scale$HEAD(float x, float y, float z, CallbackInfo ci) {
        FixedFunctionMatrixStacks.scale(x, y, z);
    }

    @Inject(method = "scale(DDD)V", at = @At("HEAD"))
    private static void Inject$scale$HEAD(double x, double y, double z, CallbackInfo ci) {
        FixedFunctionMatrixStacks.scale(x, y, z);
    }

    @Inject(method = "translate(FFF)V", at = @At("HEAD"))
    private static void Inject$translate$HEAD(float x, float y, float z, CallbackInfo ci) {
        FixedFunctionMatrixStacks.translate(x, y, z);
    }

    @Inject(method = "translate(DDD)V", at = @At("HEAD"))
    private static void Inject$translate$HEAD(double x, double y, double z, CallbackInfo ci) {
        FixedFunctionMatrixStacks.translate(x, y, z);
    }

    @Inject(method = "multMatrix", at = @At("HEAD"))
    private static void Inject$multMatrix$HEAD(FloatBuffer matrix, CallbackInfo ci) {
        FixedFunctionMatrixStacks.multMatrix(matrix);
    }

    @Inject(method = "rotate(Lorg/lwjgl/util/vector/Quaternion;)V", at = @At("HEAD"))
    private static void Inject$rotate$HEAD(Quaternion quaternionIn, CallbackInfo ci) {
        FixedFunctionMatrixStacks.rotate(quaternionIn);
    }
}
