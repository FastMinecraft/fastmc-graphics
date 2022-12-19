package dev.fastmc.graphics.mixin.accessor;

import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BackgroundRenderer.class)
public interface AccessorBackgroundRenderer {
    @Accessor
    static float getRed() {
        throw new UnsupportedOperationException();
    }

    @Accessor
    static float getGreen() {
        throw new UnsupportedOperationException();
    }

    @Accessor
    static float getBlue() {
        throw new UnsupportedOperationException();
    }
}