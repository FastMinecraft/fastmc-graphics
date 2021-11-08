package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.tileentity.info.IChestInfo
import net.minecraft.block.BlockChest
import net.minecraft.tileentity.TileEntityChest

class ChestInfo : HDirectionalTileEntityInfo<TileEntityChest>(BlockChest.FACING), IChestInfo<TileEntityChest> {
    override val isTrap: Boolean
        get() = tileEntity.chestType == BlockChest.Type.TRAP

    override val prevLidAngle: Float
        get() = tileEntity.prevLidAngle

    override val lidAngle: Float
        get() = tileEntity.lidAngle
}