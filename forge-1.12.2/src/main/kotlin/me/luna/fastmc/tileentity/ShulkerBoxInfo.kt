package me.luna.fastmc.tileentity

import me.luna.fastmc.shared.instancing.tileentity.info.IShulkerBoxInfo
import net.minecraft.block.BlockShulkerBox
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.EnumFacing

interface ShulkerBoxInfo : DirectionalTileEntityInfo<TileEntityShulkerBox>, IShulkerBoxInfo<TileEntityShulkerBox> {
    override val property: PropertyEnum<EnumFacing>
        get() = BlockShulkerBox.FACING

    override val color: Int
        get() = entity.color.metadata

    override val prevProgress: Float
        get() = entity.getProgress(0.0f)

    override val progress: Float
        get() = entity.getProgress(1.0f)
}