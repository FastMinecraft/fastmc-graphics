package me.xiaro.fastmc.mixin.render;

import me.xiaro.fastmc.AbstractWorldRenderer;
import me.xiaro.fastmc.TileEntityRenderer;
import me.xiaro.fastmc.WorldRenderer;
import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.resource.IResourceManager;
import me.xiaro.fastmc.resource.ResourceManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
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
import java.util.Map;

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
        AbstractWorldRenderer worldRenderer = new me.xiaro.fastmc.WorldRenderer(mc, resourceManager);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer));

        FastMcMod.INSTANCE.reloadEntityRenderer(resourceManager, worldRenderer);
    }
}
