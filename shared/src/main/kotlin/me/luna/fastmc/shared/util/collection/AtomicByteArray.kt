package me.luna.fastmc.shared.util.collection

import java.util.concurrent.atomic.AtomicIntegerArray

class AtomicByteArray(val size: Int) {
    private val array: AtomicIntegerArray = AtomicIntegerArray((size + 3) shr 2)

    fun set(index: Int, newValue: Byte) {
        val idx = index ushr 2
        val shift = (index and 3) shl 3
        val mask = (0xFF shl shift).inv()
        val updateBits = newValue.toInt() shl shift

        while (true) {
            val old = array.get(idx)
            val new = (old and mask) or updateBits
            if (array.compareAndSet(idx, old, new)) return
        }
    }

    fun compareAndSet(index: Int, expect: Byte, update: Byte): Boolean {
        val idx = index ushr 2
        val shift = (index and 3) shl 3
        val mask = 0xFF shl shift
        val expectBits = expect.toInt() shl shift
        val updateBits = update.toInt() shl shift

        while (true) {
            val prev = array.get(idx)
            if (prev and mask != expectBits) return false
            val num2 = prev and mask.inv() or updateBits
            if (prev == num2 || array.compareAndSet(idx, prev, num2)) {
                return true
            }
        }
    }

    fun getAndIncrement(index: Int): Byte {
        return getAndAdd(index, 1)
    }

    fun getAndDecrement(index: Int): Byte {
        return getAndAdd(index, -1)
    }

    fun getAndAdd(index: Int, delta: Int): Byte {
        while (true) {
            val current = get(index)
            val next = (current + delta).toByte()
            if (compareAndSet(index, current, next)) return current
        }
    }

    fun incrementAndGet(index: Int): Byte {
        return addAndGet(index, 1)
    }

    fun decrementAndGet(index: Int): Byte {
        return addAndGet(index, -1)
    }

    fun addAndGet(index: Int, delta: Int): Byte {
        while (true) {
            val current = get(index)
            val next = (current + delta).toByte()
            if (compareAndSet(index, current, next)) return next
        }
    }

    fun get(index: Int): Byte {
        return (array.get(index ushr 2) shr (index and 3 shl 3)).toByte()
    }
}