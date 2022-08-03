package me.luna.fastmc.tileentity

import me.luna.fastmc.shared.instancing.tileentity.info.IChestInfo
import net.minecraft.block.BlockChest
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.EnumFacing

interface ChestInfo : HDirectionalTileEntityInfo<TileEntityChest>, IChestInfo<TileEntityChest> {
    override val property: PropertyEnum<EnumFacing>
        get() = BlockChest.FACING

    override val isTrap: Boolean
        get() = entity.chestType == BlockChest.Type.TRAP

    override val prevLidAngle: Float
        get() = entity.prevLidAngle

    override val lidAngle: Float
        get() = entity.lidAngle
}