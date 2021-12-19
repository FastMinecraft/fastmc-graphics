package me.xiaro.fastmc.entity

import me.xiaro.fastmc.shared.renderbuilder.entity.info.ILivingBaseInfo
import net.minecraft.entity.EntityLivingBase

interface LivingBaseInfo<E : EntityLivingBase> : EntityInfo<E>, ILivingBaseInfo<E> {
    override val deathTime: Int
        get() = entity.deathTime

    override val limbSwing: Float
        get() = entity.limbSwing
    override val limbSwingAmount: Float
        get() = entity.limbSwingAmount
    override val prevLimbSwingAmount: Float
        get() = entity.prevLimbSwingAmount

    override val rotationYawHead: Float
        get() = entity.rotationYawHead
    override val prevRotationYawHead: Float
        get() = entity.prevRotationYawHead
    override val renderYawOffset: Float
        get() = entity.renderYawOffset
    override val prevRenderYawOffset: Float
        get() = entity.prevRenderYawOffset
}