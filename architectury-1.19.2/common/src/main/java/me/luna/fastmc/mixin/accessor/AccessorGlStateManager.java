package me.luna.fastmc.mixin.accessor;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface AccessorGlStateManager {
    @Accessor("TEXTURES")
    static GlStateManager.Texture2DState[] getTextures() {throw new UnsupportedOperationException();}
}
