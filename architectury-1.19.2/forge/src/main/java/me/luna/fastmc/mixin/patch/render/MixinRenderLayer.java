package me.luna.fastmc.mixin.patch.render;

import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderLayer.class)
public class MixinRenderLayer {
// Broken in forge
//    private static final List<RenderLayer> blockLayers = ImmutableList.of(RenderLayer.getSolid(), RenderLayer.getCutoutMipped(), RenderLayer.getCutout(), RenderLayer.getTranslucent(), RenderLayer.getTripwire());
//
//    static {
//        ((MixinRenderLayer) (Object) RenderLayer.getSolid()).index = 0;
//        ((MixinRenderLayer) (Object) RenderLayer.getCutoutMipped()).index = 1;
//        ((MixinRenderLayer) (Object) RenderLayer.getCutout()).index = 2;
//        ((MixinRenderLayer) (Object) RenderLayer.getTranslucent()).index = 3;
//        ((MixinRenderLayer) (Object) RenderLayer.getTripwire()).index = 4;
//    }
//
//    private int index = -1;
//
//    /**
//     * @author Luna
//     * @reason Memory allocation optimization
//     */
//    @Overwrite
//    public static List<RenderLayer> getBlockLayers() {
//        return blockLayers;
//    }
//
//    @Override
//    public int getIndex() {
//        return index;
//    }
}
