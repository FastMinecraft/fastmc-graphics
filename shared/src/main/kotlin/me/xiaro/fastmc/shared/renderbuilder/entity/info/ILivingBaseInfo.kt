package me.xiaro.fastmc.shared.renderbuilder.entity.info

interface ILivingBaseInfo<E> : IEntityInfo<E> {
    val rotationYawHead: Float
    val prevRotationYawHead: Float

    val renderYawOffset: Float
    val prevRenderYawOffset: Float
}