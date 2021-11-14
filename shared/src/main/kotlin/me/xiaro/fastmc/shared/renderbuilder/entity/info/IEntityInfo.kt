package me.xiaro.fastmc.shared.renderbuilder.entity.info

import me.xiaro.fastmc.shared.renderbuilder.IInfo

interface IEntityInfo<E> : IInfo<E> {

    val lightMapUV: Int

    val x: Double
    val y: Double
    val z: Double

    val prevX: Double
    val prevY: Double
    val prevZ: Double

    val rotationYaw: Float
    val rotationPitch: Float

    val prevRotationYaw: Float
    val prevRotationPitch: Float
}