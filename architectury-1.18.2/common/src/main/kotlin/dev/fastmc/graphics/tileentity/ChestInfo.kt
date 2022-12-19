package dev.fastmc.graphics.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.IChestInfo
import dev.fastmc.graphics.util.TileEntityChest
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