package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedRenderLayer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderLayer.class)
public class MixinRenderLayer implements IPatchedRenderLayer {
    static {
        ((MixinRenderLayer) (Object) RenderLayer.getSolid()).index = 0;
        ((MixinRenderLayer) (Object) RenderLayer.getCutoutMipped()).index = 1;
        ((MixinRenderLayer) (Object) RenderLayer.getCutout()).index = 2;
        ((MixinRenderLayer) (Object) RenderLayer.getTranslucent()).index = 3;
        ((MixinRenderLayer) (Object) RenderLayer.getTripwire()).index = 4;
    }

    private int index = -1;

    @Override
    public int getIndex() {
        return index;
    }
}
