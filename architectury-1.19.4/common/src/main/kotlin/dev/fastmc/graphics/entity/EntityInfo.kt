package dev.fastmc.graphics.entity

import dev.fastmc.common.fastFloor
import dev.fastmc.graphics.shared.instancing.entity.info.IEntityInfo
import net.minecraft.client.render.WorldRenderer
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper

interface EntityInfo<E : Entity> : IEntityInfo<E> {
    override val lightMapUV: Int
        get() = if (entity.isOnFire) 0xF000F0 else entity.world?.let {

            val blockPos = BlockPos.Mutable(entity.x.fastFloor(), 0, entity.z.fastFloor())

            return if (it.chunkManager.isChunkLoaded(blockPos.x shr 4, blockPos.z shr 4)) {
                blockPos.y = MathHelper.floor(entity.y + entity.eyeY)
                WorldRenderer.getLightmapCoordinates(it, blockPos)
            } else {
                0
            }
        } ?: 0xF000F0

    override val x: Double
        get() = entity.x
    override val y: Double
        get() = entity.y
    override val z: Double
        get() = entity.z

    override val prevX: Double
        get() = entity.lastRenderX
    override val prevY: Double
        get() = entity.lastRenderY
    override val prevZ: Double
        get() = entity.lastRenderZ

    override val rotationYaw: Float
        get() = entity.yaw
    override val rotationPitch: Float
        get() = entity.pitch

    override val prevRotationYaw: Float
        get() = entity.prevYaw
    override val prevRotationPitch: Float
        get() = entity.prevPitch
}