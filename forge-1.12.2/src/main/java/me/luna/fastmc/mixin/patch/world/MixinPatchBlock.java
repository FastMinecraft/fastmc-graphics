package me.luna.fastmc.mixin.patch.world;

import me.luna.fastmc.mixin.IPatchedIBlockAccess;
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
public class MixinPatchBlock {
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
    @SideOnly(Side.CLIENT)
    @Deprecated
    @Overwrite
    public boolean shouldSideBeRendered(
        IBlockState blockState,
        IBlockAccess blockAccess,
        BlockPos pos,
        EnumFacing side
    ) {
        AxisAlignedBB box = blockState.getBoundingBox(blockAccess, pos);

        switch (side) {
            case DOWN:
                if (box.minY > 0.0D) return true;
                break;
            case UP:
                if (box.maxY < 1.0D) return true;
                break;
            case NORTH:
                if (box.minZ > 0.0D) return true;
                break;
            case SOUTH:
                if (box.maxZ < 1.0D) return true;
                break;
            case WEST:
                if (box.minX > 0.0D) return true;
                break;
            case EAST:
                if (box.maxX < 1.0D) return true;
        }

        return !((IPatchedIBlockAccess) blockAccess).getBlockState(
            pos.getX() + side.getXOffset(),
            pos.getY() + side.getYOffset(),
            pos.getZ() + side.getZOffset()
        ).isOpaqueCube();
    }
}
