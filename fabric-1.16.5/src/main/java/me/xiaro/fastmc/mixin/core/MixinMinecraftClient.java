package me.xiaro.fastmc.mixin.core;

import me.xiaro.fastmc.*;
import me.xiaro.fastmc.resource.ResourceManager;
import me.xiaro.fastmc.shared.font.IFontRendererWrapper;
import me.xiaro.fastmc.shared.renderer.AbstractWorldRenderer;
import me.xiaro.fastmc.shared.resource.IResourceManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V", shift = At.Shift.AFTER))
    public void init$Inject$INVOKE$initializeTextures(CallbackInfo ci) {
        FastMcMod.INSTANCE.initGLWrapper(new GLWrapper());
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init$Inject$RETURN(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);
        IFontRendererWrapper fontRenderer = new FontRendererWrapper(mc);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer));
        fontRenderer.getWrapped().setUnicode(mc.options.forceUnicodeFont);

        FastMcMod.INSTANCE.init(resourceManager, worldRenderer, fontRenderer);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void runTick$Inject$RETURN(CallbackInfo ci) {
        FastMcMod.INSTANCE.onPostTick();
    }

    @Inject(method = "reloadResources", at = @At("RETURN"))
    public void refreshResources$Inject$RETURN(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer));

        FastMcMod.INSTANCE.reloadResource(resourceManager, worldRenderer);
    }
}
