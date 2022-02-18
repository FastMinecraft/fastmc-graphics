package me.luna.fastmc.tileentity

import me.luna.fastmc.TileEntityShulkerBox
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IShulkerBoxInfo
import net.minecraft.block.ShulkerBoxBlock

class ShulkerBoxInfo : DirectionalTileEntityInfo<TileEntityShulkerBox>(ShulkerBoxBlock.FACING),
    IShulkerBoxInfo<TileEntityShulkerBox> {
    override val color: Int
        get() = entity.color?.id ?: 16

    override val prevProgress: Float
        get() = entity.getAnimationProgress(0.0f)

    override val progress: Float
        get() = entity.getAnimationProgress(1.0f)
}