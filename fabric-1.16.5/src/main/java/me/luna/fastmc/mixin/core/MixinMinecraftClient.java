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
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow public abstract Profiler getProfiler();

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V", shift = At.Shift.AFTER))
    public void init$Inject$INVOKE$initializeTextures(CallbackInfo ci) {
        FastMcMod.INSTANCE.initGLWrapper(new GLWrapper());
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init$Inject$RETURN(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        IResourceManager resourceManager = new ResourceManager(mc);
        FastMcMod.INSTANCE.getLogger().info("Resource manager initialized");

        IFontRendererWrapper fontRenderer = new FontRendererWrapper(mc);
        FastMcMod.INSTANCE.getLogger().info("Font Renderer initialized");
        fontRenderer.getWrapped().setUnicode(mc.options.forceUnicodeFont);

        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);
        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer), new EntityRenderer(mc, worldRenderer));
        FastMcMod.INSTANCE.getLogger().info("World renderer initialized");

        FastMcMod.INSTANCE.init(resourceManager, worldRenderer, fontRenderer);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void runTick$Inject$RETURN(CallbackInfo ci) {
        getProfiler().push("fastMinecraft");
        FastMcMod.INSTANCE.onPostTick();
        getProfiler().pop();
    }

    @Inject(method = "reloadResources", at = @At("RETURN"))
    public void refreshResources$Inject$RETURN(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        IResourceManager resourceManager = new ResourceManager(mc);
        FastMcMod.INSTANCE.getLogger().info("Resource manager initialized");

        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);
        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer), new EntityRenderer(mc, worldRenderer));
        FastMcMod.INSTANCE.getLogger().info("World renderer initialized");

        FastMcMod.INSTANCE.reloadResource(resourceManager, worldRenderer);
    }
}
