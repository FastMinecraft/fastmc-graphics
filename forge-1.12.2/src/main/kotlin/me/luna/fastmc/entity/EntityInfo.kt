package me.luna.fastmc.entity

import me.luna.fastmc.shared.renderbuilder.entity.info.IEntityInfo
import net.minecraft.entity.Entity

interface EntityInfo<E : Entity> : IEntityInfo<E> {
    override val lightMapUV: Int
        get() = if (entity.isBurning) 0xF000F0 else entity.brightnessForRender

    override val x: Double
        get() = entity.posX
    override val y: Double
        get() = entity.posY
    override val z: Double
        get() = entity.posZ

    override val prevX: Double
        get() = entity.lastTickPosX
    override val prevY: Double
        get() = entity.lastTickPosY
    override val prevZ: Double
        get() = entity.lastTickPosZ

    override val rotationYaw: Float
        get() = entity.rotationYaw
    override val rotationPitch: Float
        get() = entity.rotationPitch

    override val prevRotationYaw: Float
        get() = entity.prevRotationYaw
    override val prevRotationPitch: Float
        get() = entity.prevRotationPitch
}