package dev.fastmc.graphics.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.IShulkerBoxInfo
import dev.fastmc.graphics.util.TileEntityShulkerBox
import net.minecraft.block.ShulkerBoxBlock
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

interface ShulkerBoxInfo : DirectionalTileEntityInfo<TileEntityShulkerBox>, IShulkerBoxInfo<TileEntityShulkerBox> {
    override val property: EnumProperty<Direction>
        get() = ShulkerBoxBlock.FACING

    override val color: Int
        get() = entity.color?.id ?: 16

    override val prevProgress: Float
        get() = entity.getAnimationProgress(0.0f)

    override val progress: Float
        get() = entity.getAnimationProgress(1.0f)
}