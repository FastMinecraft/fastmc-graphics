package me.xiaro.fastmc.shared.entity.info

interface IEntityInfo<E> {
    var entity: E

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