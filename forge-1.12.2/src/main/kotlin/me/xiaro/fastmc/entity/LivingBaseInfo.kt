package me.xiaro.fastmc.entity

import me.xiaro.fastmc.shared.renderbuilder.entity.info.ILivingBaseInfo
import net.minecraft.entity.EntityLivingBase

open class LivingBaseInfo<E : EntityLivingBase> : EntityInfo<E>(), ILivingBaseInfo<E> {
    override val rotationYawHead: Float
        get() = entity.rotationYawHead
    override val prevRotationYawHead: Float
        get() = entity.prevRotationYawHead
    override val renderYawOffset: Float
        get() = entity.renderYawOffset
    override val prevRenderYawOffset: Float
        get() = entity.prevRenderYawOffset
}