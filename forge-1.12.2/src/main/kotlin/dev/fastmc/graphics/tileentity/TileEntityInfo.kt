package dev.fastmc.graphics.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo
import dev.fastmc.graphics.util.blockState
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity

interface TileEntityInfo<E : TileEntity> : ITileEntityInfo<E> {
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