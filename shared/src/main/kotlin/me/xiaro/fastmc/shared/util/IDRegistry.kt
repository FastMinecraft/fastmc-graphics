package me.xiaro.fastmc.shared.util

import me.xiaro.fastmc.shared.util.collection.ExtendedBitSet

class IDRegistry {
    private val bitSet = ExtendedBitSet()

    fun register(): Int {
        var id = -1

        synchronized(this) {
            for (other in bitSet) {
                id = other
            }
            bitSet.add(++id)
        }

        return id
    }

    fun unregister(id: Int) {
        synchronized(this) {
            bitSet.remove(id)
        }
    }
}