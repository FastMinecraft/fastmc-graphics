package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.tileentity.info.IShulkerBoxInfo
import net.minecraft.block.BlockShulkerBox
import net.minecraft.tileentity.TileEntityShulkerBox

class ShulkerBoxInfo : DirectionalTileEntityInfo<TileEntityShulkerBox>(BlockShulkerBox.FACING),
    IShulkerBoxInfo<TileEntityShulkerBox> {
    override val color: Int
        get() = tileEntity.color.metadata

    override val prevProgress: Float
        get() = tileEntity.getProgress(0.0f)

    override val progress: Float
        get() = tileEntity.getProgress(1.0f)
}