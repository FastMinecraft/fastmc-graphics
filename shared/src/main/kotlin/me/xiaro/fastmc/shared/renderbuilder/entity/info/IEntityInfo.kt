package me.xiaro.fastmc.shared.renderbuilder.entity.info

import me.xiaro.fastmc.shared.renderbuilder.IInfo
import me.xiaro.fastmc.shared.util.ClassIDRegistry
import me.xiaro.fastmc.shared.util.ITypeID

interface IEntityInfo<E : Any> : IInfo<E>, ITypeID {
    override val typeID: Int
        get() = registry.get(entity.javaClass)

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

    companion object {
        val registry = ClassIDRegistry<Any>()
    }
}