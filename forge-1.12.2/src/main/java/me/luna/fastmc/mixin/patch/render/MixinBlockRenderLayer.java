package me.luna.fastmc.mixin.patch.render;

import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderLayer.class)
public class MixinBlockRenderLayer {

    private static BlockRenderLayer[] valuesOverride;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void clinit$Inject$RETURNs(CallbackInfo ci) {
        valuesOverride = new BlockRenderLayer[]{ BlockRenderLayer.SOLID, BlockRenderLayer.CUTOUT_MIPPED, BlockRenderLayer.CUTOUT, BlockRenderLayer.TRANSLUCENT };
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public static BlockRenderLayer[] values() {
        return valuesOverride;
    }
}
