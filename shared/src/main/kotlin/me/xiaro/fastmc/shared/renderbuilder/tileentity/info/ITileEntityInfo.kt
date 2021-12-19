package me.xiaro.fastmc.shared.renderbuilder.tileentity.info

import me.xiaro.fastmc.shared.renderbuilder.IInfo
import me.xiaro.fastmc.shared.util.ClassIDRegistry
import me.xiaro.fastmc.shared.util.ITypeID

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