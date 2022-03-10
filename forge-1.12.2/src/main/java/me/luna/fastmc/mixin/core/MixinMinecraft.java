package me.luna.fastmc.mixin.core;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.GLWrapper;
import me.luna.fastmc.renderer.EntityRenderer;
import me.luna.fastmc.renderer.FontRendererWrapper;
import me.luna.fastmc.renderer.TileEntityRenderer;
import me.luna.fastmc.renderer.WorldRenderer;
import me.luna.fastmc.resource.ResourceManager;
import me.luna.fastmc.shared.font.IFontRendererWrapper;
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer;
import me.luna.fastmc.shared.resource.IResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Shadow @Final public Profiler profiler;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;initializeTextures()V", shift = At.Shift.AFTER))
    public void init$Inject$INVOKE$initializeTextures(CallbackInfo ci) {
        FastMcMod.INSTANCE.initGLWrapper(new GLWrapper());
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void init$Inject$RETURN(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);
        IFontRendererWrapper fontRenderer = new FontRendererWrapper(mc);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer), new EntityRenderer(mc, worldRenderer));
        fontRenderer.getWrapped().setUnicode(mc.gameSettings.forceUnicodeFont);

        FastMcMod.INSTANCE.init(resourceManager, worldRenderer, fontRenderer);
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    public void runTick$Inject$RETURN(CallbackInfo ci) {
        this.profiler.startSection("fastMinecraft");
        FastMcMod.INSTANCE.onPostTick();
        this.profiler.endSection();
    }

    @Inject(method = "refreshResources", at = @At("RETURN"))
    public void refreshResources$Inject$RETURN(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer), new EntityRenderer(mc, worldRenderer));

        FastMcMod.INSTANCE.reloadResource(resourceManager, worldRenderer);
    }

    @Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false))
    public void runGameLoop$Redirect$INVOKE$yield() {

    }
}
