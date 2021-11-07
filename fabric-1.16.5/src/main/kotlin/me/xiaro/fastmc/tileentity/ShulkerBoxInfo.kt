package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntityShulkerBox
import me.xiaro.fastmc.tileentity.info.IShulkerBoxInfo

class ShulkerBoxInfo : DirectionalTileEntityInfo<TileEntityShulkerBox>(), IShulkerBoxInfo<TileEntityShulkerBox> {
    override val color: Int
        get() = tileEntity.color?.id ?: 0

    override val prevProgress: Float
        get() = tileEntity.getAnimationProgress(0.0f)

    override val progress: Float
        get() = tileEntity.getAnimationProgress(1.0f)
}