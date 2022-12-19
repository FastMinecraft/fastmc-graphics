package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.graphics.shared.terrain.IPatchedRenderLayer;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockRenderLayer.class)
public class MixinCoreBlockRenderLayer implements IPatchedRenderLayer {
    static {
        ((MixinCoreBlockRenderLayer) (Object) BlockRenderLayer.SOLID).index = 0;

        ((MixinCoreBlockRenderLayer) (Object) BlockRenderLayer.CUTOUT_MIPPED).index = 0;
        ((MixinCoreBlockRenderLayer) (Object) BlockRenderLayer.CUTOUT_MIPPED).attribute = 0b0000_0011;

        ((MixinCoreBlockRenderLayer) (Object) BlockRenderLayer.CUTOUT).index = 0;
        ((MixinCoreBlockRenderLayer) (Object) BlockRenderLayer.CUTOUT).attribute = 0b0000_0010;

        ((MixinCoreBlockRenderLayer) (Object) BlockRenderLayer.TRANSLUCENT).index = 1;
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
