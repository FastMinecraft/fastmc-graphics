package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IBedInfo
import net.minecraft.block.BlockBed
import net.minecraft.tileentity.TileEntityBed

class BedInfo : HDirectionalTileEntityInfo<TileEntityBed>(BlockBed.FACING), IBedInfo<TileEntityBed> {
    override val color: Int
        get() = entity.color.metadata

    override val isHead: Boolean
        get() = if (entity.hasWorld()) entity.isHeadPiece else true
}