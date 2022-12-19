package me.luna.fastmc.mixin.patch;

import net.minecraft.util.shape.SimpleVoxelShape;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleVoxelShape.class)
public abstract class MixinSimpleVoxelShape extends VoxelShape {
    private static final int CLASS_HASH = SimpleVoxelShape.class.hashCode();
    private int hash = 0;

    public MixinSimpleVoxelShape(VoxelSet voxels) {
        super(voxels);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(VoxelSet voxelSet, CallbackInfo ci) {
        hash = CLASS_HASH;
        hash = 31 * hash + voxelSet.getXSize();
        hash = 31 * hash + voxelSet.getYSize();
        hash = 31 * hash + voxelSet.getZSize();
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
