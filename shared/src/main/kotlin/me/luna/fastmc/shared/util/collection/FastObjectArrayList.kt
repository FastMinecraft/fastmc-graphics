package me.luna.fastmc.shared.util.collection

import it.unimi.dsi.fastutil.objects.ObjectArrayList

class FastObjectArrayList<E> : ObjectArrayList<E> {
    private constructor(array: Array<E>, dummy: Boolean) : super(array, dummy)
    constructor() : super(20)
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

    fun addAll(other: Array<E>) {
        if (other.isEmpty()) return
        val newSize = this.size + other.size
        this.ensureCapacity(newSize)
        System.arraycopy(other, 0, this.a, this.size, other.size)
        this.size = newSize
    }

    fun clearAndTrim() {
        size = 0
        @Suppress("UNCHECKED_CAST")
        a = emptyArray<Any?>() as Array<out E>
    }

    fun clearFast() {
        size = 0
    }

    fun setSize(i: Int) {
        size = i
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <K> wrap(a: Array<K?>, length: Int): FastObjectArrayList<K> {
            require(length <= a.size) { "The specified length (" + length + ") is greater than the array size (" + a.size + ")" }
            val l = FastObjectArrayList(a, false)
            l.size = length
            return l as FastObjectArrayList<K>
        }

        @JvmStatic
        fun <K> wrap(a: Array<K?>): FastObjectArrayList<K> {
            return wrap(a, a.size)
        }
    }
}