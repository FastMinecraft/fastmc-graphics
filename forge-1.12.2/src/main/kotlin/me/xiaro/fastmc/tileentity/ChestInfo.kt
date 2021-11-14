package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IChestInfo
import net.minecraft.block.BlockChest
import net.minecraft.tileentity.TileEntityChest

class ChestInfo : HDirectionalTileEntityInfo<TileEntityChest>(BlockChest.FACING), IChestInfo<TileEntityChest> {
    override val isTrap: Boolean
        get() = entity.chestType == BlockChest.Type.TRAP

    override val prevLidAngle: Float
        get() = entity.prevLidAngle

    override val lidAngle: Float
        get() = entity.lidAngle
}