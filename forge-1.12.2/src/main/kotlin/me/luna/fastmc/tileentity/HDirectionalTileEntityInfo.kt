package me.luna.fastmc.tileentity

import me.luna.fastmc.shared.renderbuilder.tileentity.info.IHDirectionalTileEntityInfo
import me.luna.fastmc.util.getPropertyOrDefault
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

interface HDirectionalTileEntityInfo<E : TileEntity> : TileEntityInfo<E>, IHDirectionalTileEntityInfo<E> {
    val property: PropertyEnum<EnumFacing>

    override val hDirection: Int
        get() = blockState.getPropertyOrDefault(property, EnumFacing.SOUTH).horizontalIndex
}