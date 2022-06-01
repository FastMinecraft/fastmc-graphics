package me.luna.fastmc.mixin.patch;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import me.luna.fastmc.mixin.IPatchedVoxelShape;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.SimpleVoxelShape;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleVoxelShape.class)
public class MixinSimpleVoxelShape extends VoxelShape implements IPatchedVoxelShape {
    private static final int CLASS_HASH = SimpleVoxelShape.class.hashCode();
    private int hash = 0;

    public MixinSimpleVoxelShape(VoxelSet voxels) {
        super(voxels);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(VoxelSet voxelSet, CallbackInfo ci) {
        hash = CLASS_HASH;
        hash = 31 * hash + IPatchedVoxelShape.getFractionalDoubleList(voxelSet.getXSize()).size();
        hash = 31 * hash + IPatchedVoxelShape.getFractionalDoubleList(voxelSet.getYSize()).size();
        hash = 31 * hash + IPatchedVoxelShape.getFractionalDoubleList(voxelSet.getZSize()).size();
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public DoubleList getPointPositions(Direction.Axis axis) {
        return IPatchedVoxelShape.getFractionalDoubleList(this.voxels.getSize(axis));
    }

    @Override
    public int hash() {
        return hash;
    }
}
