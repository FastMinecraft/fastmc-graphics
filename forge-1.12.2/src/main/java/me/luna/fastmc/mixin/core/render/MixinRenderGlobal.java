package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.renderer.EntityRenderer;
import me.luna.fastmc.renderer.TileEntityRenderer;
import me.luna.fastmc.renderer.WorldRenderer;
import me.luna.fastmc.resource.ResourceManager;
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer;
import me.luna.fastmc.shared.resource.IResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderGlobal.class, priority = Integer.MAX_VALUE)
public abstract class MixinRenderGlobal {
    @Shadow
    @Final
    private Minecraft mc;


    @Inject(method = "setWorldAndLoadRenderers", at = @At("HEAD"))
    private void setWorldAndLoadRenderers$Inject$HEAD(WorldClient worldClientIn, CallbackInfo ci) {
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().clear();
        FastMcMod.INSTANCE.getWorldRenderer().getEntityRenderer().clear();
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntityStatic(Lnet/minecraft/entity/Entity;FZ)V"))
    private void renderEntities$Redirect$INVOKE$renderEntityStatic(RenderManager instance, Entity entityIn, float partialTicks, boolean debug) {
        if (!((EntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getEntityRenderer()).hasRenderer(entityIn)) {
            instance.renderEntityStatic(entityIn, partialTicks, debug);
        }
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;release()V"))
    private void renderEntities$Inject$INVOKE$release(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        mc.profiler.endStartSection("fastMcEntity");
        FastMcMod.INSTANCE.getWorldRenderer().preRender(partialTicks);
        FastMcMod.INSTANCE.getWorldRenderer().getEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;setRenderOutlines(Z)V", ordinal = 1))
    private void renderEntities$Inject$INVOKE$setRenderOutlines$1(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        FastMcMod.INSTANCE.getWorldRenderer().getEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;disableLightmap()V"))
    private void renderEntities$Inject$INVOKE$disableLightmap(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        mc.profiler.endStartSection("fastMcTileEntity");
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    private void refreshResources$Inject$RETURN(CallbackInfo ci) {
        Minecraft mc = this.mc;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer), new EntityRenderer(mc, worldRenderer));

        FastMcMod.INSTANCE.reloadRenderer(resourceManager, worldRenderer);
    }
}
