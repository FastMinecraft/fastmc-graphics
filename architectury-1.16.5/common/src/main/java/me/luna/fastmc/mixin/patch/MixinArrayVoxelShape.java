package me.luna.fastmc.mixin.patch;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import me.luna.fastmc.mixin.IPatchedVoxelShape;
import net.minecraft.util.shape.ArrayVoxelShape;
import net.minecraft.util.shape.VoxelSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrayVoxelShape.class)
public class MixinArrayVoxelShape implements IPatchedVoxelShape {
    private static final int CLASS_HASH = ArrayVoxelShape.class.hashCode();

    private int hash = 0;

    @Inject(method = "<init>(Lnet/minecraft/util/shape/VoxelSet;Lit/unimi/dsi/fastutil/doubles/DoubleList;Lit/unimi/dsi/fastutil/doubles/DoubleList;Lit/unimi/dsi/fastutil/doubles/DoubleList;)V", at = @At("RETURN"))
    private void init$Inject$RETURN(
        VoxelSet shape,
        DoubleList xPoints,
        DoubleList yPoints,
        DoubleList zPoints,
        CallbackInfo ci
    ) {
        hash = CLASS_HASH;
        hash = 31 * hash + xPoints.hashCode();
        hash = 31 * hash + yPoints.hashCode();
        hash = 31 * hash + zPoints.hashCode();
    }

    @Override
    public int hash() {
        return hash;
    }
}
