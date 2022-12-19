package dev.fastmc.graphics.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.IHDirectionalTileEntityInfo
import dev.fastmc.graphics.util.getPropertyOrDefault
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

interface HDirectionalTileEntityInfo<E : TileEntity> : TileEntityInfo<E>, IHDirectionalTileEntityInfo<E> {
    val property: PropertyEnum<EnumFacing>

    override val hDirection: Int
        get() = blockState.getPropertyOrDefault(property, EnumFacing.SOUTH).horizontalIndex
}