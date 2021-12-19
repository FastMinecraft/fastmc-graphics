package me.xiaro.fastmc.shared.util

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

class ClassIDRegistry<T> {
    private val map = Object2IntOpenHashMap<Class<T>>().apply {
        defaultReturnValue(-1)
    }
    private val registry = IDRegistry()

    fun get(clazz: Class<T>): Int {
        var id = map.getInt(clazz)

        if (id == -1) {
            synchronized(registry) {
                id = registry.register()
                map[clazz] = id
            }
        }

        return id
    }
}