package me.xiaro.fastmc.mixin;

import me.xiaro.fastmc.AbstractEntityRenderer;
import me.xiaro.fastmc.EntityRenderer;
import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.GLWrapper;
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

        FastMcMod.INSTANCE.init(resourceManager, entityRenderer);
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

        FastMcMod.INSTANCE.reloadResource(resourceManager, entityRenderer);
    }
}
