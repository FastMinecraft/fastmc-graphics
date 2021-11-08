package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntity
import me.xiaro.fastmc.shared.tileentity.info.IHDirectionalTileEntityInfo
import me.xiaro.fastmc.util.getPropertyOrDefault
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

open class HDirectionalTileEntityInfo<E : TileEntity>(private val property: EnumProperty<Direction>) :
    TileEntityInfo<E>(), IHDirectionalTileEntityInfo<E> {
    override val hDirection: Int
        get() = blockState.getPropertyOrDefault(property, Direction.SOUTH).horizontal
}