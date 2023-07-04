package dev.fastmc.graphics.mixin.core;

import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.RendererReloader;
import dev.fastmc.graphics.shared.FpsDisplay;
import dev.luna5ama.glwrapper.api.GLWrapperImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinCoreMinecraft {
    @Shadow
    @Final
    public Profiler profiler;

    @Shadow
    private IReloadableResourceManager resourceManager;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;initializeTextures()V", shift = At.Shift.AFTER))
    public void init$Inject$INVOKE$initializeTextures(CallbackInfo ci) {
        FastMcMod.INSTANCE.initGLWrapper(new GLWrapperImpl(), 1);
        FastMcMod.INSTANCE.initProfiler(new dev.fastmc.graphics.mixin.Profiler((Minecraft) (Object) this));
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;viewport(IIII)V"))
    public void Inject$init$RETURN(CallbackInfo ci) {
        this.resourceManager.registerReloadListener(RendererReloader.INSTANCE);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    public void runTick$Inject$RETURN(CallbackInfo ci) {
        this.profiler.startSection("fastMinecraft");
        FastMcMod.INSTANCE.onPostTick();
        this.profiler.endSection();
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isFramerateLimitBelowMax()Z", shift = At.Shift.BEFORE))
    public void runGameLoop$Inject$INVOKE$isFramerateLimitBelowMax(CallbackInfo ci) {
        FpsDisplay.INSTANCE.onPostRenderTick();
    }
}