package dev.fastmc.graphics.mixin.patch;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.SlicedVoxelShape;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlicedVoxelShape.class)
public class MixinSlicedVoxelShape {
    private static final int CLASS_HASH = SlicedVoxelShape.class.hashCode();
    private int hash = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(VoxelShape shape, Direction.Axis axis, int sliceWidth, CallbackInfo ci) {
        hash = CLASS_HASH;
        hash = 31 * hash + shape.hashCode();
        hash = 31 * hash + axis.hashCode();
        hash = 31 * hash + sliceWidth;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}