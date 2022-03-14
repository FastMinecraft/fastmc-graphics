package me.luna.fastmc.mixin.patch.render;

import com.google.common.collect.ImmutableList;
import me.luna.fastmc.mixin.IPatchedRenderLayer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(RenderLayer.class)
public class MixinRenderLayer implements IPatchedRenderLayer {
    private static final List<RenderLayer> blockLayers = ImmutableList.of(RenderLayer.getSolid(), RenderLayer.getCutoutMipped(), RenderLayer.getCutout(), RenderLayer.getTranslucent(), RenderLayer.getTripwire());

    private int index = -1;

    @Override
    public int getIndex() {
        return index;
    }

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
        ((MixinRenderLayer) (Object) RenderLayer.getCutoutMipped()).index = 1;
        ((MixinRenderLayer) (Object) RenderLayer.getCutout()).index = 2;
        ((MixinRenderLayer) (Object) RenderLayer.getTranslucent()).index = 3;
        ((MixinRenderLayer) (Object) RenderLayer.getTripwire()).index = 4;
    }
}
