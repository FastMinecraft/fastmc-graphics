package me.luna.fastmc.tileentity

import me.luna.fastmc.TileEntity
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IHDirectionalTileEntityInfo
import me.luna.fastmc.util.getPropertyOrDefault
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

open class HDirectionalTileEntityInfo<E : TileEntity>(private val property: EnumProperty<Direction>) :
    TileEntityInfo<E>(), IHDirectionalTileEntityInfo<E> {
    override val hDirection: Int
        get() = blockState.getPropertyOrDefault(property, Direction.SOUTH).horizontal
}