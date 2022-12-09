package me.luna.fastmc.shared.instancing.tileentity.info

import dev.fastmc.common.ClassIDRegistry
import dev.fastmc.common.ITypeID
import me.luna.fastmc.shared.instancing.IInfo

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