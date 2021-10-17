package me.xiaro.fastmc.mixin;

import me.xiaro.fastmc.*;
import me.xiaro.fastmc.font.IFontRendererWrapper;
import me.xiaro.fastmc.resource.IResourceManager;
import me.xiaro.fastmc.resource.ResourceManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;initializeTextures()V", shift = At.Shift.AFTER))
    public void init$Inject$INVOKE$initializeTextures(CallbackInfo ci) {
        FastMcMod.INSTANCE.initGLWrapper(new GLWrapper());
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void init$Inject$RETURN(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractEntityRenderer entityRenderer = new EntityRenderer(mc, resourceManager);
        IFontRendererWrapper fontRenderer = new FontRendererWrapper(mc);
        fontRenderer.getWrapped().setUnicode(mc.gameSettings.forceUnicodeFont);

        FastMcMod.INSTANCE.init(resourceManager, entityRenderer, fontRenderer);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    public void runTick$Inject$RETURN(CallbackInfo ci) {
        FastMcMod.INSTANCE.onPostTick();
    }

    @Inject(method = "refreshResources", at = @At("RETURN"))
    public void refreshResources$Inject$RETURN(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractEntityRenderer entityRenderer = new EntityRenderer(mc, resourceManager);
        IFontRendererWrapper fontRenderer = new FontRendererWrapper(mc);
        fontRenderer.getWrapped().setUnicode(mc.gameSettings.forceUnicodeFont);

        FastMcMod.INSTANCE.reloadResource(resourceManager, entityRenderer, fontRenderer);
    }
}
