package dev.fastmc.graphics.entity

import dev.fastmc.graphics.shared.instancing.entity.info.ILivingBaseInfo
import net.minecraft.entity.LivingEntity

interface LivingBaseInfo<E : LivingEntity> : EntityInfo<E>, ILivingBaseInfo<E> {
    override val deathTime: Int
        get() = entity.deathTime

    override val limbSwing: Float
        get() = entity.limbAnimator.pos
    override val limbSwingAmount: Float
        get() = entity.limbAnimator.getSpeed(0.0f)
    override val prevLimbSwingAmount: Float
        get() = entity.limbAnimator.getSpeed(1.0f)

    override val rotationYawHead: Float
        get() = entity.headYaw
    override val prevRotationYawHead: Float
        get() = entity.prevHeadYaw
    override val renderYawOffset: Float
        get() = entity.bodyYaw
    override val prevRenderYawOffset: Float
        get() = entity.prevBodyYaw
}