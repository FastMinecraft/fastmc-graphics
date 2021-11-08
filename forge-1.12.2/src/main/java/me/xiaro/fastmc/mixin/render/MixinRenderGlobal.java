package me.xiaro.fastmc.mixin.render;

import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.renderer.TileEntityRenderer;
import me.xiaro.fastmc.renderer.WorldRenderer;
import me.xiaro.fastmc.resource.ResourceManager;
import me.xiaro.fastmc.shared.renderer.AbstractWorldRenderer;
import me.xiaro.fastmc.shared.resource.IResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(value = RenderGlobal.class, priority = Integer.MAX_VALUE)
public abstract class MixinRenderGlobal {
    @Shadow @Final private Minecraft mc;

    @Inject(method = "updateTileEntities", at = @At("HEAD"))
    public void updateNoCullingBlockEntities$Inject$HEAD(Collection<TileEntity> tileEntitiesToRemove, Collection<TileEntity> tileEntitiesToAdd, CallbackInfo ci) {
        TileEntityRenderer tileEntityRenderer = (TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer();
        tileEntitiesToAdd.removeIf(tileEntityRenderer::hasRenderer);
    }

    @ModifyVariable(method = "renderEntities", at = @At(value = "STORE", ordinal = 0), ordinal = 3)
    public List<TileEntity> renderEntities$ModifyVariable$STORE(List<TileEntity> input) {
        TileEntityRenderer tileEntityRenderer = (TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer();
        List<TileEntity> list = new ArrayList<>();

        for (TileEntity tileEntity : input) {
            if (!tileEntityRenderer.hasRenderer(tileEntity)) list.add(tileEntity);
        }

        return list;
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
