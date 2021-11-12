package me.xiaro.fastmc.mixin.core.render;

import me.xiaro.fastmc.AdaptersKt;
import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.TileEntityRenderer;
import me.xiaro.fastmc.resource.ResourceManager;
import me.xiaro.fastmc.shared.renderer.AbstractWorldRenderer;
import me.xiaro.fastmc.shared.resource.IResourceManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Mixin(value = WorldRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinWorldRenderer {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "setWorld", at = @At("HEAD"))
    public void setWorld$Inject$HEAD(CallbackInfo ci) {
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().clear();
    }

    @Inject(method = "updateNoCullingBlockEntities", at = @At("HEAD"))
    public void updateNoCullingBlockEntities$Inject$HEAD(Collection<BlockEntity> removed, Collection<BlockEntity> added, CallbackInfo ci) {
        TileEntityRenderer tileEntityRenderer = (TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer();
        added.removeIf(tileEntityRenderer::hasRenderer);
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void renderEntities$Inject$HEAD(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {

    }

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    public List<BlockEntity> render$ModifyVariable$STORE(List<BlockEntity> input) {
        TileEntityRenderer tileEntityRenderer = (TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer();

        Iterator<BlockEntity> iterator = input.iterator();
        //noinspection Java8CollectionRemoveIf
        while (iterator.hasNext()) {
            BlockEntity tileEntity = iterator.next();
            if (tileEntityRenderer.hasRenderer(tileEntity)) iterator.remove();
        }

        return input;
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void renderEntities$Inject$INVOKE$draw(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        client.getProfiler().swap("tileEntities");

        MatrixStack.Entry entry = matrices.peek();
        FastMcMod.INSTANCE.getWorldRenderer().setupCamera(AdaptersKt.toJoml(matrix4f), AdaptersKt.toJoml(entry.getModel()));

        FastMcMod.INSTANCE.getWorldRenderer().preRender(tickDelta);
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().render();
        FastMcMod.INSTANCE.getWorldRenderer().postRender();

        client.getProfiler().swap("blockentities");
    }

    @Inject(method = "reload()V", at = @At("RETURN"))
    public void refreshResources$Inject$RETURN(CallbackInfo ci) {
        MinecraftClient mc = this.client;
        IResourceManager resourceManager = new ResourceManager(mc);
        AbstractWorldRenderer worldRenderer = new me.xiaro.fastmc.WorldRenderer(mc, resourceManager);

        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer));

        FastMcMod.INSTANCE.reloadEntityRenderer(resourceManager, worldRenderer);
    }
}
