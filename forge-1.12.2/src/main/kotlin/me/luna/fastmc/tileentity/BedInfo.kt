package me.luna.fastmc.tileentity

import me.luna.fastmc.shared.instancing.tileentity.info.IBedInfo
import net.minecraft.block.BlockBed
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntityBed
import net.minecraft.util.EnumFacing

interface BedInfo : HDirectionalTileEntityInfo<TileEntityBed>, IBedInfo<TileEntityBed> {
    override val property: PropertyEnum<EnumFacing>
        get() = BlockBed.FACING

    override val color: Int
        get() = entity.color.metadata

    override val isHead: Boolean
        get() = if (entity.hasWorld()) entity.isHeadPiece else true
}