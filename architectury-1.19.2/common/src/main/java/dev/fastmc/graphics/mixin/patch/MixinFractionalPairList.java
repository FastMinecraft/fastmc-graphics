package dev.fastmc.graphics.mixin.patch;

import dev.fastmc.graphics.mixin.IPatchedVoxelShape;
import net.minecraft.util.shape.FractionalDoubleList;
import net.minecraft.util.shape.FractionalPairList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FractionalPairList.class)
public class MixinFractionalPairList {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/util/shape/FractionalDoubleList"))
    private FractionalDoubleList init$Redirect$INVOKE$FractionalDoubleList$init(int sectionCount) {
        return IPatchedVoxelShape.getFractionalDoubleList(sectionCount);
    }
}