package me.luna.fastmc.shared.util.collection

import it.unimi.dsi.fastutil.objects.ObjectArrayList

class FastObjectArrayList<E> : ObjectArrayList<E>() {
    fun addAll(other: ObjectArrayList<E>) {
        val newSize = this.size + other.size
        this.ensureCapacity(newSize)
        System.arraycopy(other.elements(), 0, this.elements(), this.size, other.size)
        this.size = newSize
    }
}