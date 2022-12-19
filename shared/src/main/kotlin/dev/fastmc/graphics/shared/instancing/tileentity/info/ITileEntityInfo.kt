package dev.fastmc.graphics.shared.instancing.tileentity.info

import dev.fastmc.graphics.shared.instancing.IInfo
import dev.fastmc.graphics.shared.util.ClassIDRegistry
import dev.fastmc.graphics.shared.util.ITypeID

interface ITileEntityInfo<E : Any> : IInfo<E>, ITypeID {
    override val typeID: Int
        get() = registry.get(entity.javaClass)

    val posX: Int
    val posY: Int
    val posZ: Int
    val lightMapUV: Int

    companion object {
        val registry = ClassIDRegistry<Any>()
    }
}