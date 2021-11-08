package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.tileentity.info.ITileEntityInfo
import me.xiaro.fastmc.util.blockState
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityChest

abstract class TileEntityInfo<E : TileEntity> : ITileEntityInfo<E> {
    override lateinit var tileEntity: E

    override val posX: Int
        get() = tileEntity.pos.x

    override val posY: Int
        get() = tileEntity.pos.y

    override val posZ: Int
        get() = tileEntity.pos.z

    @Suppress("UNNECESSARY_SAFE_CALL")
    override val lightMapUV: Int
        get() = tileEntity.world?.getCombinedLight(tileEntity.pos, 0) ?: 0xF000F0

    @Suppress("UNNECESSARY_SAFE_CALL")
    val blockState: IBlockState?
        get() = tileEntity.blockState
}