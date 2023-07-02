package dev.fastmc.graphics.mixin.core;

import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("ConstantConditions")
@Mixin(Window.class)
public abstract class MixinCoreWindow {
    @Redirect(method = "<init>", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private long Redirect$glfwCreateWindow$INVOKE$init(int width, int height, CharSequence title, long monitor, long share) {
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        return GLFW.glfwCreateWindow(width, height, title, monitor, share);
    }
}