package me.luna.fastmc.tileentity

import me.luna.fastmc.shared.instancing.tileentity.info.IDirectionalTileEntityInfo
import me.luna.fastmc.util.TileEntity
import me.luna.fastmc.util.getPropertyOrDefault
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

interface DirectionalTileEntityInfo<E : TileEntity> : TileEntityInfo<E>, IDirectionalTileEntityInfo<E> {
    val property: EnumProperty<Direction>

    override val direction: Int
        get() = blockState.getPropertyOrDefault(property, Direction.SOUTH).id
}