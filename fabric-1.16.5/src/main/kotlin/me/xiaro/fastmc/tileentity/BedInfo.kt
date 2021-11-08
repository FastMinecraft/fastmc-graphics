package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntityBed
import me.xiaro.fastmc.tileentity.info.IBedInfo
import net.minecraft.block.BedBlock
import net.minecraft.block.entity.BedBlockEntity
import net.minecraft.block.enums.BedPart

class BedInfo : HDirectionalTileEntityInfo<TileEntityBed>(BedBlock.FACING), IBedInfo<TileEntityBed> {
    override val color: Int
        get() = tileEntity.color.id

    override val isHead: Boolean
        get() = blockState?.get(BedBlock.PART) != BedPart.FOOT
}