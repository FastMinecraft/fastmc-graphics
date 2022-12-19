package dev.fastmc.graphics.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.IBedInfo
import dev.fastmc.graphics.util.TileEntityBed
import net.minecraft.block.BedBlock
import net.minecraft.block.enums.BedPart
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

interface BedInfo : HDirectionalTileEntityInfo<TileEntityBed>, IBedInfo<TileEntityBed> {
    override val property: EnumProperty<Direction>
        get() = BedBlock.FACING

    override val color: Int
        get() = entity.color.id

    override val isHead: Boolean
        get() = blockState?.get(BedBlock.PART) != BedPart.FOOT
}