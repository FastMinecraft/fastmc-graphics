package me.xiaro.fastmc.mixin.render;

import me.xiaro.fastmc.TextureUpdater;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSprite {
    @Shadow protected int tickCounter;
    @Shadow private AnimationMetadataSection animationMetadata;
    @Shadow protected int frameCounter;
    @Shadow protected List<int[][]> framesTextureData;
    @Shadow protected int[][] interpolatedFrameData;
    @Shadow protected int width;
    @Shadow protected int height;
    @Shadow protected int originX;
    @Shadow protected int originY;

    @Shadow protected abstract int interpolateColor(double p_188535_1_, int p_188535_3_, int p_188535_4_);
    @Shadow protected abstract void updateAnimationInterpolated();

    @Inject(method = "updateAnimation", at = @At("HEAD"), cancellable = true)
    private void updateAnimation$Inject$HEAD(CallbackInfo ci) {
        ci.cancel();

        ++this.tickCounter;

        if (this.tickCounter >= this.animationMetadata.getFrameTimeSingle(this.frameCounter)) {
            int i = this.animationMetadata.getFrameIndex(this.frameCounter);
            int j = this.animationMetadata.getFrameCount() == 0 ? this.framesTextureData.size() : this.animationMetadata.getFrameCount();
            this.frameCounter = (this.frameCounter + 1) % j;
            this.tickCounter = 0;
            int k = this.animationMetadata.getFrameIndex(this.frameCounter);

            if (i != k && k >= 0 && k < this.framesTextureData.size()) {
                upload(this.framesTextureData.get(k));
            }
        } else if (this.animationMetadata.isInterpolate()) {
            this.updateAnimationInterpolated();
        }
    }

    @Inject(method = "updateAnimationInterpolated", at = @At("HEAD"), cancellable = true)
    private void updateAnimationInterpolated$Inject$HEAD(CallbackInfo ci) {
        ci.cancel();

        double delta = 1.0D - (double) tickCounter / (double) animationMetadata.getFrameTimeSingle(frameCounter);
        int i = animationMetadata.getFrameIndex(frameCounter);
        int j = animationMetadata.getFrameCount() == 0 ? framesTextureData.size() : animationMetadata.getFrameCount();
        int k = animationMetadata.getFrameIndex((frameCounter + 1) % j);

        if (i != k && k >= 0 && k < framesTextureData.size()) {
            int[][] data1 = framesTextureData.get(i);
            int[][] data2 = framesTextureData.get(k);

            if (interpolatedFrameData == null || interpolatedFrameData.length != data1.length) {
                interpolatedFrameData = new int[data1.length][];
            }

            for (int l = 0; l < data1.length; ++l) {
                if (interpolatedFrameData[l] == null) {
                    interpolatedFrameData[l] = new int[data1[l].length];
                }

                if (l < data2.length && data2[l].length == data1[l].length) {
                    for (int i1 = 0; i1 < data1[l].length; ++i1) {
                        int j1 = data1[l][i1];
                        int k1 = data2[l][i1];
                        int l1 = interpolateColor(delta, j1 >> 16 & 255, k1 >> 16 & 255);
                        int i2 = interpolateColor(delta, j1 >> 8 & 255, k1 >> 8 & 255);
                        int j2 = interpolateColor(delta, j1 & 255, k1 & 255);
                        interpolatedFrameData[l][i1] = j1 & -16777216 | l1 << 16 | i2 << 8 | j2;
                    }
                }
            }

            upload(interpolatedFrameData);
        }
    }

    private void upload(int[][] mipmapData) {
        int textureID = glGetInteger(GL_TEXTURE_BINDING_2D);

        GlStateManager.glTexParameteri(3553, 10241, mipmapData.length > 1 ? 9986 : 9728);
        GlStateManager.glTexParameteri(3553, 10240, 9728);
        GlStateManager.glTexParameteri(3553, 10242, 10497);
        GlStateManager.glTexParameteri(3553, 10243, 10497);

        for (int level = 0; level < mipmapData.length; ++level) {
            int[] data = mipmapData[level];
            if ((width >> level <= 0) || (height >> level <= 0)) break;

            int maxSectionHeight = 0x400000 / (width >> level);
            int sectionHeight;

            for (int offset = 0; offset < (width >> level) * (height >> level); offset += (width >> level) * sectionHeight) {
                int sectionY = offset / (width >> level);
                sectionHeight = Math.min(maxSectionHeight, (height >> level) - sectionY);
                int length = (width >> level) * sectionHeight;

                int finalLevel = level;
                int finalOffset = offset;
                int finalSectionHeight = sectionHeight;

                TextureUpdater.INSTANCE.getUpdater().newTask(
                    buffer -> buffer.asIntBuffer().put(data, finalOffset, length).position(0).limit(length),
                    buffer -> {
                        GlStateManager.bindTexture(textureID);
                        glTexSubImage2D(3553, finalLevel, originX >> finalLevel, (originY >> finalLevel) + sectionY, width >> finalLevel, finalSectionHeight, 32993, 33639, buffer);
                    }
                );
            }
        }
    }
}
