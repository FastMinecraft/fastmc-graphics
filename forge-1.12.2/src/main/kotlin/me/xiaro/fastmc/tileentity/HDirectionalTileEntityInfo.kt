package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IHDirectionalTileEntityInfo
import me.xiaro.fastmc.util.getPropertyOrDefault
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

open class HDirectionalTileEntityInfo<E : TileEntity>(private val property: PropertyEnum<EnumFacing>) :
    TileEntityInfo<E>(),
    IHDirectionalTileEntityInfo<E> {
    override val hDirection: Int
        get() = blockState.getPropertyOrDefault(property, EnumFacing.SOUTH).horizontalIndex
}