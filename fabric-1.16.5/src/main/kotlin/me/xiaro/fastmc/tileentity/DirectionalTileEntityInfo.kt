package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntity
import me.xiaro.fastmc.tileentity.info.IDirectionalTileEntityInfo
import net.minecraft.block.ChestBlock
import net.minecraft.util.math.Direction

open class DirectionalTileEntityInfo<E: TileEntity> : TileEntityInfo<E>(), IDirectionalTileEntityInfo<E> {
    override val direction: Int
        get() = (blockState?.getOrEmpty(ChestBlock.FACING)?.orElse(null) ?: Direction.SOUTH).id
}