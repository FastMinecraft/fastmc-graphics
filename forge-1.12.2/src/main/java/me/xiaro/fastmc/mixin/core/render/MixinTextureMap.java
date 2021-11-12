package me.xiaro.fastmc.mixin.core.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL43.glCopyImageSubData;

@Mixin(TextureMap.class)
public abstract class MixinTextureMap extends AbstractTexture {
    @Shadow private int mipmapLevels;

    private int swapTextureID = -1;

    @Inject(method = "updateAnimations", at = @At("RETURN"))
    private void updateAnimations$Inject$RETURN(CallbackInfo ci) {
        int id = this.getGlTextureId();
        this.glTextureId = swapTextureID;
        swapTextureID = id;
    }

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void loadTextureAtlas$Inject$RETURN(IResourceManager resourceManager, CallbackInfo ci, int maxSize, Stitcher stitcher) {
        if (swapTextureID != -1) {
            GlStateManager.deleteTexture(swapTextureID);
            swapTextureID = -1;
        }

        swapTextureID = GlStateManager.generateTexture();
        TextureUtil.allocateTextureImpl(swapTextureID, this.mipmapLevels, stitcher.getCurrentWidth(), stitcher.getCurrentHeight());

        GlStateManager.bindTexture(0);
        for (int i = 0; i <= mipmapLevels; ++i) {
            glCopyImageSubData(this.getGlTextureId(), GL_TEXTURE_2D, i, 0, 0, 0, swapTextureID, GL_TEXTURE_2D, i, 0, 0, 0, stitcher.getCurrentWidth() >> i, stitcher.getCurrentHeight() >> i, 1);
        }
    }
}
