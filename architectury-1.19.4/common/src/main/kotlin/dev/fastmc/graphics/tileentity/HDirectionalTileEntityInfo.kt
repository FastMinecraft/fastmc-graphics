package dev.fastmc.graphics.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.IHDirectionalTileEntityInfo
import dev.fastmc.graphics.util.TileEntity
import dev.fastmc.graphics.util.getPropertyOrDefault
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

interface HDirectionalTileEntityInfo<E : TileEntity> : TileEntityInfo<E>, IHDirectionalTileEntityInfo<E> {
    val property: EnumProperty<Direction>

    override val hDirection: Int
        get() = blockState.getPropertyOrDefault(property, Direction.SOUTH).horizontal
}