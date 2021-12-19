package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IEnderChestInfo
import net.minecraft.block.BlockEnderChest
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntityEnderChest
import net.minecraft.util.EnumFacing

interface EnderChestInfo : HDirectionalTileEntityInfo<TileEntityEnderChest>, IEnderChestInfo<TileEntityEnderChest> {
    override val property: PropertyEnum<EnumFacing>
        get() = BlockEnderChest.FACING

    override val prevLidAngle: Float
        get() = entity.prevLidAngle

    override val lidAngle: Float
        get() = entity.lidAngle
}