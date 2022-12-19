package dev.fastmc.graphics.mixin.patch.render;

import dev.fastmc.graphics.shared.terrain.IPatchedRenderLayer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderLayer.class)
public class MixinRenderLayer implements IPatchedRenderLayer {
    static {
        ((MixinRenderLayer) (Object) RenderLayer.getSolid()).index = 0;

        ((MixinRenderLayer) (Object) RenderLayer.getCutoutMipped()).index = 0;
        ((MixinRenderLayer) (Object) RenderLayer.getCutoutMipped()).attribute = 0b0000_0011;

        ((MixinRenderLayer) (Object) RenderLayer.getCutout()).index = 0;
        ((MixinRenderLayer) (Object) RenderLayer.getCutout()).attribute = 0b0000_0010;

        ((MixinRenderLayer) (Object) RenderLayer.getTranslucent()).index = 1;

        ((MixinRenderLayer) (Object) RenderLayer.getTripwire()).index = 2;
    }

    private int index = -1;
    private int attribute = 0b0000_0001;

    @Override
    public int getLayerIndex() {
        return index;
    }

    @Override
    public int getModelAttribute() {
        return attribute;
    }
}