package me.luna.fastmc.mixin.patch.render;

import com.google.common.collect.ImmutableList;
import me.luna.fastmc.shared.terrain.IPatchedRenderLayer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(RenderLayer.class)
public class MixinRenderLayer implements IPatchedRenderLayer {
    private static final List<RenderLayer> blockLayers = ImmutableList.of(
        RenderLayer.getSolid(),
        RenderLayer.getCutoutMipped(),
        RenderLayer.getCutout(),
        RenderLayer.getTranslucent(),
        RenderLayer.getTripwire()
    );

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public static List<RenderLayer> getBlockLayers() {
        return blockLayers;
    }

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
