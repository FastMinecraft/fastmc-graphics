package me.luna.fastmc.tileentity

import me.luna.fastmc.shared.instancing.tileentity.info.IHDirectionalTileEntityInfo
import me.luna.fastmc.util.TileEntity
import me.luna.fastmc.util.getPropertyOrDefault
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.math.Direction

interface HDirectionalTileEntityInfo<E : TileEntity> : TileEntityInfo<E>, IHDirectionalTileEntityInfo<E> {
    val property: EnumProperty<Direction>

    override val hDirection: Int
        get() = blockState.getPropertyOrDefault(property, Direction.SOUTH).horizontal
}