package me.luna.fastmc.mixin.patch;

import me.luna.fastmc.mixin.IPatchedVoxelShape;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VoxelShape.class)
public class MixinVoxelShape implements IPatchedVoxelShape {

}
