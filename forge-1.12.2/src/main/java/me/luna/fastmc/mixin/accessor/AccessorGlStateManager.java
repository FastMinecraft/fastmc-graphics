package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface AccessorGlStateManager {
    @Accessor
    static GlStateManager.TextureState[] getTextureState() {throw new UnsupportedOperationException();}
}
