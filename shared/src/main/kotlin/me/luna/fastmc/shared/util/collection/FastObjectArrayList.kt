package me.luna.fastmc.shared.util.collection

import it.unimi.dsi.fastutil.objects.ObjectArrayList

class FastObjectArrayList<E> : ObjectArrayList<E>() {
    val capacity: Int
        get() = this.a.size

    fun addAll(other: ObjectArrayList<E>) {
        val newSize = this.size + other.size
        this.ensureCapacity(newSize)
        System.arraycopy(other.elements(), 0, this.a, this.size, other.size)
        this.size = newSize
    }

    fun clearFast() {
        size = 0
    }
}