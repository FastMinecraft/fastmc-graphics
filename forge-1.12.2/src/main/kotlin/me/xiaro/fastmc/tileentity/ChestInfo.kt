package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.tileentity.info.IChestInfo
import net.minecraft.block.BlockChest
import net.minecraft.tileentity.TileEntityChest

class ChestInfo : TileEntityInfo<TileEntityChest>(), IChestInfo<TileEntityChest> {
    override val direction: Int
        get() = if (tileEntity.hasWorld()) {
            tileEntity.blockMetadata
        } else {
            0
        }

    override val isTrap: Boolean
        get() = tileEntity.chestType == BlockChest.Type.TRAP

    override val prevLidAngle: Float
        get() = tileEntity.prevLidAngle

    override val lidAngle: Float
        get() = tileEntity.lidAngle
}