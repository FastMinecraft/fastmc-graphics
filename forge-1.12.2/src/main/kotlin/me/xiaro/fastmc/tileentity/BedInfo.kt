package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.tileentity.info.IBedInfo
import net.minecraft.block.BlockBed
import net.minecraft.tileentity.TileEntityBed

class BedInfo : HDirectionalTileEntityInfo<TileEntityBed>(BlockBed.FACING), IBedInfo<TileEntityBed> {
    override val color: Int
        get() = tileEntity.color.metadata

    override val isHead: Boolean
        get() = if (tileEntity.hasWorld()) tileEntity.isHeadPiece else true
}