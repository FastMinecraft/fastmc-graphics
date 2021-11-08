package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntityShulkerBox
import me.xiaro.fastmc.shared.tileentity.info.IShulkerBoxInfo
import net.minecraft.block.ShulkerBoxBlock

class ShulkerBoxInfo : DirectionalTileEntityInfo<TileEntityShulkerBox>(ShulkerBoxBlock.FACING),
    IShulkerBoxInfo<TileEntityShulkerBox> {
    override val color: Int
        get() = tileEntity.color?.id ?: 16

    override val prevProgress: Float
        get() = tileEntity.getAnimationProgress(0.0f)

    override val progress: Float
        get() = tileEntity.getAnimationProgress(1.0f)
}