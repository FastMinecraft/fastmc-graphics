package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.tileentity.info.IDirectionalTileEntityInfo
import me.xiaro.fastmc.util.getPropertyOrDefault
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

open class DirectionalTileEntityInfo<E : TileEntity>(private val property: PropertyEnum<EnumFacing>) :
    TileEntityInfo<E>(),
    IDirectionalTileEntityInfo<E> {
    override val direction: Int
        get() = blockState.getPropertyOrDefault(property, EnumFacing.SOUTH).index
}