package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.tileentity.info.IShulkerBoxInfo
import net.minecraft.block.BlockShulkerBox
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.EnumFacing

class ShulkerBoxInfo : TileEntityInfo<TileEntityShulkerBox>(), IShulkerBoxInfo {
    override lateinit var tileEntity: TileEntityShulkerBox

    @Suppress("UNNECESSARY_SAFE_CALL")
    override val direction: Int
        get() = tileEntity.world?.let {
            val blockState = it.getBlockState(tileEntity.pos)
            if (blockState.block is BlockShulkerBox) {
                (blockState.getValue(BlockShulkerBox.FACING) as EnumFacing).index
            } else {
                1
            }
        } ?: 1

    override val color: Int
        get() = tileEntity.color.metadata

    override val prevProgress: Float
        get() = tileEntity.getProgress(0.0f)

    override val progress: Float
        get() = tileEntity.getProgress(1.0f)
}