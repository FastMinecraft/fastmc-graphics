package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.tileentity.info.IChestInfo
import net.minecraft.block.BlockChest
import net.minecraft.tileentity.TileEntityChest

class ChestInfo : TileEntityInfo<TileEntityChest>(), IChestInfo {
    override lateinit var tileEntity: TileEntityChest

    override val direction: Int
        get() = if (tileEntity.hasWorld()) {
            tileEntity.blockMetadata
        } else {
            0
        }

    override val hasAdjChestXNeg: Boolean
        get() = tileEntity.adjacentChestXNeg != null

    override val hasAdjChestZNeg: Boolean
        get() = tileEntity.adjacentChestZNeg != null

    override val hasAdjChestXPos: Boolean
        get() = tileEntity.adjacentChestXPos != null

    override val hasAdjChestZPos: Boolean
        get() = tileEntity.adjacentChestZPos != null

    override val isTrap: Boolean
        get() = tileEntity.chestType == BlockChest.Type.TRAP

    override val prevLidAngle: Float
        get() = tileEntity.prevLidAngle

    override val lidAngle: Float
        get() = tileEntity.lidAngle
}