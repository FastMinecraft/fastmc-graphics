package me.xiaro.fastmc.patch.world;

import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(Block.class)
public class MixinBlock {
    /**
     * @author Xiaro
     * @reason Memory allocation optimization
     */
    @Overwrite
    protected static void addCollisionBoxToList(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, AxisAlignedBB blockBox) {
        if (blockBox != null && entityBox.intersects(
            blockBox.minX + pos.getX(),
            blockBox.minY + pos.getY(),
            blockBox.minZ + pos.getZ(),
            blockBox.maxX + pos.getX(),
            blockBox.maxY + pos.getY(),
            blockBox.maxZ + pos.getZ())
        ) {
            collidingBoxes.add(blockBox.offset(pos));
        }
    }
}
