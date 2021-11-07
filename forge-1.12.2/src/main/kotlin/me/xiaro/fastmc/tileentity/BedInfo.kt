package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.tileentity.info.IBedInfo
import net.minecraft.tileentity.TileEntityBed

class BedInfo : TileEntityInfo<TileEntityBed>(), IBedInfo<TileEntityBed> {
    override val direction: Int
        get() = if (tileEntity.hasWorld()) {
            tileEntity.blockMetadata and 3
        } else {
            0
        }

    override val color: Int
        get() = tileEntity.color.metadata

    override val isHead: Boolean
        get() = if (tileEntity.hasWorld()) tileEntity.isHeadPiece else true
}