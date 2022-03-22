package me.luna.fastmc.shared.util.collection

import it.unimi.dsi.fastutil.objects.ObjectArrayList

class FastObjectArrayList<E> : ObjectArrayList<E> {
    constructor() : super()
    constructor(c: Collection<E>) : super(c)
    constructor(capacity: Int) : super(capacity)

    val capacity: Int
        get() = this.a.size

    fun addAll(other: ObjectArrayList<E>) {
        if (other.isEmpty) return
        val newSize = this.size + other.size
        this.ensureCapacity(newSize)
        System.arraycopy(other.elements(), 0, this.a, this.size, other.size)
        this.size = newSize
    }

    @Suppress("UNCHECKED_CAST")
    override fun ensureCapacity(capacity: Int) {
        if (capacity > a.size) {
            val t = arrayOfNulls<Any>(capacity)
            System.arraycopy(a, 0, t, 0, size)
            a = t as Array<out E>
        }
    }

    fun clearAndTrim() {
        size = 0
        @Suppress("UNCHECKED_CAST")
        a = emptyArray<Any?>() as Array<out E>
    }
}