package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntity
import me.xiaro.fastmc.tileentity.info.ITileEntityInfo
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity

open class TileEntityInfo<E : TileEntity> : ITileEntityInfo<E> {
    override lateinit var tileEntity: E

    override val posX: Int
        get() = tileEntity.pos.x

    override val posY: Int
        get() = tileEntity.pos.y

    override val posZ: Int
        get() = tileEntity.pos.z

    @Suppress("UNNECESSARY_SAFE_CALL")
    override val lightMapUV: Int
        get() = tileEntity.world?.getLightLevel(tileEntity.pos, 0) ?: 0xF000F0

    val blockState: BlockState?
        get() = if (tileEntity.hasWorld()) tileEntity.cachedState else null
}