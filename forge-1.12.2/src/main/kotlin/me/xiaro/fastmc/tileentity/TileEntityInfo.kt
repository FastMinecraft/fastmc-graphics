package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo
import me.xiaro.fastmc.util.blockState
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity

abstract class TileEntityInfo<E : TileEntity> : ITileEntityInfo<E> {
    override lateinit var entity: E

    override val posX: Int
        get() = entity.pos.x

    override val posY: Int
        get() = entity.pos.y

    override val posZ: Int
        get() = entity.pos.z

    @Suppress("UNNECESSARY_SAFE_CALL")
    override val lightMapUV: Int
        get() = entity.world?.getCombinedLight(entity.pos, 0) ?: 0xF000F0

    @Suppress("UNNECESSARY_SAFE_CALL")
    val blockState: IBlockState?
        get() = entity.blockState
}