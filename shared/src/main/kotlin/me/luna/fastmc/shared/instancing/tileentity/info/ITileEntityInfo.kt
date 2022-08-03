package me.luna.fastmc.shared.instancing.tileentity.info

import me.luna.fastmc.shared.instancing.IInfo
import me.luna.fastmc.shared.util.ClassIDRegistry
import me.luna.fastmc.shared.util.ITypeID

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