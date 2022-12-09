package me.luna.fastmc.shared.instancing.entity.info

import dev.fastmc.common.ClassIDRegistry
import dev.fastmc.common.ITypeID
import me.luna.fastmc.shared.instancing.IInfo

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