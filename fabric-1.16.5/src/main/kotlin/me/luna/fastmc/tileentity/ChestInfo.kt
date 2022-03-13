package me.luna.fastmc.tileentity

import me.luna.fastmc.TileEntityChest
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IChestInfo
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.TrappedChestBlockEntity
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

interface ChestInfo : HDirectionalTileEntityInfo<TileEntityChest>, IChestInfo<TileEntityChest> {
    override val property: EnumProperty<Direction>
        get() = ChestBlock.FACING

    override val isTrap: Boolean
        get() = entity is TrappedChestBlockEntity

    override val prevLidAngle: Float
        get() = entity.getAnimationProgress(0.0f)

    override val lidAngle: Float
        get() = entity.getAnimationProgress(1.0f)
}