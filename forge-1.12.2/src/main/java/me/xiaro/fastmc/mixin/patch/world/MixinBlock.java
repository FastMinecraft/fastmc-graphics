package me.xiaro.fastmc.mixin.patch.world;

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
        if (blockBox != null) {
            double minX = blockBox.minX + pos.getX();
            double minY = blockBox.minY + pos.getY();
            double minZ = blockBox.minZ + pos.getZ();
            double maxX = blockBox.maxX + pos.getX();
            double maxY = blockBox.maxY + pos.getY();
            double maxZ = blockBox.maxZ + pos.getZ();

            if (entityBox.intersects(minX, minY, minZ, maxX, maxY, maxZ)) {
                collidingBoxes.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
            }
        }
    }
}
