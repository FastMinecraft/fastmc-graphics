package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntity
import me.xiaro.fastmc.shared.tileentity.info.ITileEntityInfo
import me.xiaro.fastmc.util.blockState
import net.minecraft.block.BlockState
import net.minecraft.client.render.WorldRenderer

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
        get() = tileEntity.world?.let {
            WorldRenderer.getLightmapCoordinates(it, tileEntity.pos)
        } ?: 0xF000F0

    val blockState: BlockState?
        get() = tileEntity.blockState
}