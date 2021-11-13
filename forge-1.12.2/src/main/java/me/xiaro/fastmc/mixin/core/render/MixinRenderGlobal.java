package me.xiaro.fastmc.mixin.core.render;

import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.renderer.TileEntityRenderer;
import me.xiaro.fastmc.renderer.WorldRenderer;
import me.xiaro.fastmc.resource.ResourceManager;
import me.xiaro.fastmc.shared.renderer.AbstractWorldRenderer;
import me.xiaro.fastmc.shared.resource.IResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(value = RenderGlobal.class, priority = Integer.MAX_VALUE)
public abstract class MixinRenderGlobal {
    @Shadow @Final private Minecraft mc;

    @Inject(method = "updateTileEntities", at = @At("HEAD"))
    public void updateNoCullingBlockEntities$Inject$HEAD(Collection<TileEntity> tileEntitiesToRemove, Collection<TileEntity> tileEntitiesToAdd, CallbackInfo ci) {
        TileEntityRenderer tileEntityRenderer = (TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer();
        tileEntitiesToAdd.removeIf(tileEntityRenderer::hasRenderer);
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;render(Lnet/minecraft/tileentity/TileEntity;FI)V"))
    public void renderEntities$Redirect$INVOKE$render(TileEntityRendererDispatcher instance, TileEntity tileentityIn, float partialTicks, int destroyStage) {
        if (!((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer()).hasRenderer(tileentityIn)) {
            instance.render(tileentityIn, partialTicks, destroyStage);
        }
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;disableLightmap()V"))
    public void renderEntities$Inject$INVOKE$preRenderDamagedBlocks(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        mc.profiler.endStartSection("tileEntities");
        FastMcMod.INSTANCE.getWorldRenderer().preRender(partialTicks);
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    public void refreshResources$Inject$RETURN(CallbackInfo ci) {
        Minecraft mc = this.mc;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer));

        FastMcMod.INSTANCE.reloadEntityRenderer(resourceManager, worldRenderer);
    }
}
