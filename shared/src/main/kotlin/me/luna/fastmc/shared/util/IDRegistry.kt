package me.luna.fastmc.shared.util

import me.luna.fastmc.shared.util.collection.DynamicBitSet

class IDRegistry {
    private val bitSet = DynamicBitSet()

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