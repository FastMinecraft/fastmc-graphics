package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.accessor.AccessorRenderGlobalContainerLocalRenderInformation;
import me.luna.fastmc.renderer.EntityRenderer;
import me.luna.fastmc.renderer.TileEntityRenderer;
import me.luna.fastmc.renderer.WorldRenderer;
import me.luna.fastmc.resource.ResourceManager;
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer;
import me.luna.fastmc.shared.resource.IResourceManager;
import me.luna.fastmc.shared.util.DoubleBufferedCollection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(value = RenderGlobal.class, priority = Integer.MAX_VALUE)
public abstract class MixinRenderGlobal {
    @Shadow
    @Final
    private Minecraft mc;
    private final DoubleBufferedCollection<List<TileEntity>> renderTileEntities = new DoubleBufferedCollection<>(new ArrayList<>());

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntityStatic(Lnet/minecraft/entity/Entity;FZ)V"))
    public void renderEntities$Redirect$INVOKE$renderEntityStatic(RenderManager instance, Entity entityIn, float partialTicks, boolean debug) {
        if (!((EntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getEntityRenderer()).hasRenderer(entityIn)) {
            instance.renderEntityStatic(entityIn, partialTicks, debug);
        }
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;release()V"))
    public void renderEntities$Inject$INVOKE$release(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        mc.profiler.endStartSection("fastMcEntity");
        FastMcMod.INSTANCE.getWorldRenderer().preRender(partialTicks);
        FastMcMod.INSTANCE.getWorldRenderer().getEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;setRenderOutlines(Z)V", ordinal = 1))
    public void renderEntities$Inject$INVOKE$setRenderOutlines$1(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        FastMcMod.INSTANCE.getWorldRenderer().getEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;disableLightmap()V"))
    private void renderEntities$Inject$INVOKE$disableLightmap(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        mc.profiler.endStartSection("fastMcTileEntity");
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;drawBatch(I)V", remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
    private void renderEntities$Inject$INVOKE$drawBatch(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci, int pass) {
        for (TileEntity tileEntity : renderTileEntities.get()) {
            if (!tileEntity.shouldRenderInPass(pass) || !camera.isBoundingBoxInFrustum(tileEntity.getRenderBoundingBox()))
                continue;
            TileEntityRendererDispatcher.instance.render(tileEntity, partialTicks, -1);
        }
    }

    @Redirect(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;", opcode = Opcodes.GETFIELD, ordinal = 1))
    private List<RenderGlobal.ContainerLocalRenderInformation> renderEntities$Redirect$INVOKE$FIELD$renderInfos$GETFIELD(RenderGlobal instance) {
        return Collections.emptyList();
    }

    @Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false))
    private void setupTerrain$Inject$HEAD(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        renderTileEntities.swap();
    }

    @ModifyArg(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false), index = 0)
    private Object setupTerrain$Inject$INVOKE$add$1(Object value) {
        List<TileEntity> list = ((AccessorRenderGlobalContainerLocalRenderInformation) value).getRenderChunk().getCompiledChunk().getTileEntities();
        if (!list.isEmpty()) {
            renderTileEntities.get().addAll(list);
        }
        return value;
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    public void refreshResources$Inject$RETURN(CallbackInfo ci) {
        Minecraft mc = this.mc;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractWorldRenderer worldRenderer = new WorldRenderer(mc, resourceManager);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer), new EntityRenderer(mc, worldRenderer));

        FastMcMod.INSTANCE.reloadRenderer(resourceManager, worldRenderer);
    }
}
