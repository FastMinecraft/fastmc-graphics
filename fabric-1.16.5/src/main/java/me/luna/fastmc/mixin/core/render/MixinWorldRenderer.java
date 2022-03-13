package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.AdaptersKt;
import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.renderer.EntityRenderer;
import me.luna.fastmc.renderer.TileEntityRenderer;
import me.luna.fastmc.resource.ResourceManager;
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer;
import me.luna.fastmc.shared.resource.IResourceManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Mixin(value = WorldRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinWorldRenderer {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private ClientWorld world;

    private boolean first = false;

    @Inject(method = "setWorld", at = @At("HEAD"))
    public void setWorld$Inject$HEAD(CallbackInfo ci) {
        if (this.world == null) {
            first = true;
            return;
        }
        FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer().clear();
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void renderEntities$Inject$HEAD(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {

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
        if (this.world == null || first) {
            first = false;
            return;
        }
        MinecraftClient mc = this.client;

        IResourceManager resourceManager = new ResourceManager(mc);
        FastMcMod.INSTANCE.getLogger().info("Resource manager initialized");

        AbstractWorldRenderer worldRenderer = new me.luna.fastmc.renderer.WorldRenderer(mc, resourceManager);
        worldRenderer.init(new TileEntityRenderer(mc, worldRenderer), new EntityRenderer(mc, worldRenderer));
        FastMcMod.INSTANCE.getLogger().info("World renderer initialized");

        FastMcMod.INSTANCE.reloadRenderer(resourceManager, worldRenderer);
    }
}
