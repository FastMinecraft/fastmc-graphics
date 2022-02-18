package me.luna.fastmc.tileentity

import me.luna.fastmc.TileEntity
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IDirectionalTileEntityInfo
import me.luna.fastmc.util.getPropertyOrDefault
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

open class DirectionalTileEntityInfo<E : TileEntity>(private val property: EnumProperty<Direction>) :
    TileEntityInfo<E>(), IDirectionalTileEntityInfo<E> {
    override val direction: Int
        get() = blockState.getPropertyOrDefault(property, Direction.SOUTH).id
}