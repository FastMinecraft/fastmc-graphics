package me.luna.fastmc.entity

import me.luna.fastmc.shared.renderbuilder.entity.info.ILivingBaseInfo
import net.minecraft.entity.LivingEntity

interface LivingBaseInfo<E : LivingEntity> : EntityInfo<E>, ILivingBaseInfo<E> {
    override val deathTime: Int
        get() = entity.deathTime

    override val limbSwing: Float
        get() = entity.limbAngle
    override val limbSwingAmount: Float
        get() = entity.limbDistance
    override val prevLimbSwingAmount: Float
        get() = entity.lastLimbDistance

    override val rotationYawHead: Float
        get() = entity.headYaw
    override val prevRotationYawHead: Float
        get() = entity.prevHeadYaw
    override val renderYawOffset: Float
        get() = entity.bodyYaw
    override val prevRenderYawOffset: Float
        get() = entity.prevBodyYaw
}