package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IDirectionalTileEntityInfo
import me.xiaro.fastmc.util.getPropertyOrDefault
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

interface DirectionalTileEntityInfo<E : TileEntity> : TileEntityInfo<E>, IDirectionalTileEntityInfo<E> {
    val property: PropertyEnum<EnumFacing>

    override val direction: Int
        get() = blockState.getPropertyOrDefault(property, EnumFacing.SOUTH).index
}