package me.luna.fastmc.mixin.patch.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(Block.class)
public class MixinBlock {
    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    protected static void addCollisionBoxToList(
        BlockPos pos,
        AxisAlignedBB entityBox,
        List<AxisAlignedBB> collidingBoxes,
        AxisAlignedBB blockBox
    ) {
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

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Deprecated
    @SideOnly(Side.CLIENT)
    @Overwrite
    public boolean shouldSideBeRendered(
        IBlockState blockState,
        IBlockAccess blockAccess,
        BlockPos pos,
        EnumFacing side
    ) {
        AxisAlignedBB axisalignedbb = blockState.getBoundingBox(blockAccess, pos);

        switch (side) {
            case DOWN:

                if (axisalignedbb.minY > 0.0D) {
                    return true;
                }

                break;
            case UP:

                if (axisalignedbb.maxY < 1.0D) {
                    return true;
                }

                break;
            case NORTH:

                if (axisalignedbb.minZ > 0.0D) {
                    return true;
                }

                break;
            case SOUTH:

                if (axisalignedbb.maxZ < 1.0D) {
                    return true;
                }

                break;
            case WEST:

                if (axisalignedbb.minX > 0.0D) {
                    return true;
                }

                break;
            case EAST:

                if (axisalignedbb.maxX < 1.0D) {
                    return true;
                }
        }

        return !blockAccess.getBlockState(pos.offset(side)).doesSideBlockRendering(
            blockAccess,
            pos.offset(side),
            side.getOpposite()
        );
    }
}
