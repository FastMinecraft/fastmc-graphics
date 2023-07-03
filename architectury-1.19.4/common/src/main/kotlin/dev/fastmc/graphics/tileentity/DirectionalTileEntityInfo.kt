package dev.fastmc.graphics.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.IDirectionalTileEntityInfo
import dev.fastmc.graphics.util.TileEntity
import dev.fastmc.graphics.util.getPropertyOrDefault
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

interface DirectionalTileEntityInfo<E : TileEntity> : TileEntityInfo<E>, IDirectionalTileEntityInfo<E> {
    val property: EnumProperty<Direction>

    override val direction: Int
        get() = blockState.getPropertyOrDefault(property, Direction.SOUTH).id
}