package me.luna.fastmc.tileentity

import me.luna.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo
import me.luna.fastmc.util.TileEntity
import me.luna.fastmc.util.blockState
import net.minecraft.block.BlockState
import net.minecraft.client.render.WorldRenderer

interface TileEntityInfo<E : TileEntity> : ITileEntityInfo<E> {
    override val posX: Int
        get() = entity.pos.x

    override val posY: Int
        get() = entity.pos.y

    override val posZ: Int
        get() = entity.pos.z

    @Suppress("UNNECESSARY_SAFE_CALL")
    override val lightMapUV: Int
        get() = entity.world?.let {
            WorldRenderer.getLightmapCoordinates(it, entity.pos)
        } ?: 0xF000F0

    val blockState: BlockState?
        get() = entity.blockState
}