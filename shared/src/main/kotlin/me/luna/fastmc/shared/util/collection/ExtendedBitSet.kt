package me.luna.fastmc.shared.util.collection

import it.unimi.dsi.fastutil.ints.IntCollection
import kotlin.math.max
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
class ExtendedBitSet : MutableSet<Int> {
    var bitArray: LongArray; private set

    constructor() {
        bitArray = EMPTY_LONG_ARRAY
    }

    constructor(capacity: Int) {
        bitArray = LongArray((capacity + 63) shr 6)
    }

    constructor(other: ExtendedBitSet) {
        bitArray = other.bitArray.copyOf()
    }

    constructor(array: LongArray) {
        bitArray = array.copyOf()
    }

    override val size: Int
        get() {
            var sum = 0
            for (bit in bitArray) {
                sum += bit.countOneBits()
            }
            return sum
        }

    fun put(element: Int, state: Boolean): Boolean {
        val bit = 1L shl (element and 0x3F)
        val index = element shr 6

        val prev = getBit(index)
        val result = if (state) prev or bit else prev xor bit
        putBit(index, result)

        return result != prev
    }

    override fun add(element: Int): Boolean {
        val bit = 1L shl (element and 0x3F)
        val index = element shr 6

        val prev = getBit(index)
        val result = prev or bit
        putBit(index, result)

        return result != prev
    }

    fun addFast(element: Int) {
        val index = element shr 6
        bitArray[index] = bitArray[index] or (1L shl (element and 0x3F))
    }

    fun addFastCheck(element: Int): Boolean {
        val index = element shr 6
        val prev = bitArray[index]
        val result = prev or (1L shl (element and 0x3F))
        bitArray[index] = result
        return result != prev
    }

    override fun addAll(elements: Collection<Int>): Boolean {
        return addAll(toLongArrayVariableSize(elements))
    }

    fun addAll(elements: IntCollection): Boolean {
        return addAll(toLongArrayVariableSize(elements))
    }

    fun addAll(elements: IntArray): Boolean {
        return addAll(toLongArrayVariableSize(elements))
    }

    fun addAll(elements: Array<Int>): Boolean {
        return addAll(toLongArrayVariableSize(elements))
    }

    fun addAll(elements: ExtendedBitSet): Boolean {
        return addAll(elements.bitArray)
    }

    fun addAll(longArray: LongArray): Boolean {
        var modified = false
        val a = getMaxSize(bitArray)
        val b = getMaxSize(longArray)
        ensureArrayLength(max(a, b))

        for (i in 0 until b) {
            val prev = bitArray[i]
            val result = prev or longArray[i]
            bitArray[i] = result
            modified = modified || result != prev
        }

        return modified
    }

    private inline fun toLongArrayVariableSize(elements: Collection<Int>): LongArray {
        var longArray = LongArray(0)
        for (element in elements) {
            val index = element shr 6
            if (index >= longArray.size) {
                longArray = longArray.copyOf((index + 1) * 2)
            }

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArrayVariableSize(elements: IntCollection): LongArray {
        var longArray = LongArray(0)
        val iterator = elements.iterator()
        while (iterator.hasNext()) {
            val element = iterator.nextInt()
            val index = element shr 6
            if (index >= longArray.size) {
                longArray = longArray.copyOf((index + 1) * 2)
            }

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArrayVariableSize(elements: Array<Int>): LongArray {
        var longArray = LongArray(0)
        for (i in elements.indices) {
            val element = elements[i]
            val index = element shr 6
            if (index >= longArray.size) {
                longArray = longArray.copyOf((index + 1) * 2)
            }

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArrayVariableSize(elements: IntArray): LongArray {
        var longArray = LongArray(0)
        for (i in elements.indices) {
            val element = elements[i]
            val index = element shr 6
            if (index >= longArray.size) {
                longArray = longArray.copyOf((index + 1) * 2)
            }

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    override fun clear() {
        for (i in bitArray.indices) {
            bitArray[i] = 0L
        }
    }

    fun clearFast() {
        bitArray = EMPTY_LONG_ARRAY
    }

    override fun remove(element: Int): Boolean {
        val bit = 1L shl (element and 0x3F)
        val index = element shr 6

        val prev = getBit(index)
        val result = prev and bit.inv()
        putBit(index, result)

        return result != prev
    }

    override fun removeAll(elements: Collection<Int>): Boolean {
        return removeAll(toLongArray(elements))
    }

    fun removeAll(elements: IntCollection): Boolean {
        return removeAll(toLongArray(elements))
    }

    fun removeAll(elements: ExtendedBitSet): Boolean {
        return removeAll(elements.bitArray)
    }

    fun removeAll(longArray: LongArray): Boolean {
        var modified = false
        val size = min(getMaxSize(bitArray), getMaxSize(longArray))
        for (i in 0 until size) {
            val prev = bitArray[i]
            val result = prev and longArray[i].inv()
            bitArray[i] = result
            modified = modified || result != prev
        }
        return modified
    }

    override fun retainAll(elements: Collection<Int>): Boolean {
        return retainAll(toLongArray(elements))
    }

    fun retainAll(elements: IntCollection): Boolean {
        return retainAll(toLongArray(elements))
    }

    fun retainAll(longArray: LongArray): Boolean {
        var modified = false
        for (i in bitArray.indices) {
            val prev = bitArray[i]
            val result = longArray[i] and prev
            bitArray[i] = result
            modified = modified || result != prev
        }
        return modified
    }

    private inline fun toLongArray(elements: Collection<Int>): LongArray {
        val longArray = LongArray(bitArray.size)
        for (element in elements) {
            val index = element shr 6
            if (index >= longArray.size) continue

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArray(elements: IntCollection): LongArray {
        val longArray = LongArray(bitArray.size)
        val iterator = elements.iterator()
        while (iterator.hasNext()) {
            val element = iterator.nextInt()
            val index = element shr 6
            if (index >= longArray.size) continue

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArray(elements: Array<Int>): LongArray {
        val longArray = LongArray(bitArray.size)
        for (i in elements.indices) {
            val element = elements[i]
            val index = element shr 6
            if (index >= longArray.size) continue

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArray(elements: IntArray): LongArray {
        val longArray = LongArray(bitArray.size)
        for (i in elements.indices) {
            val element = elements[i]
            val index = element shr 6
            if (index >= longArray.size) continue

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun getMaxSize(longArray: LongArray): Int {
        for (i in longArray.size - 1 downTo 0) {
            if (longArray[i] != 0L) return i + 1
        }
        return 0
    }

    fun containsFast(element: Int): Boolean {
        return bitArray[element shr 6] and (1L shl (element and 0x3F)) != 0L
    }

    override fun contains(element: Int): Boolean {
        return containsInt(element)
    }

    fun containsInt(element: Int): Boolean {
        val index = element shr 6
        if (index >= bitArray.size) return false
        return bitArray[index] and (1L shl (element and 0x3F)) != 0L
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        val longArray = toLongArrayNullable(elements)
        return longArray != null && containsAll(longArray)
    }

    fun containsAll(elements: IntCollection): Boolean {
        val longArray = toLongArrayNullable(elements)
        return longArray != null && containsAll(longArray)
    }

    fun containsAll(elements: IntArray): Boolean {
        val longArray = toLongArrayNullable(elements)
        return longArray != null && containsAll(longArray)
    }

    fun containsAll(elements: Array<Int>): Boolean {
        val longArray = toLongArrayNullable(elements)
        return longArray != null && containsAll(longArray)
    }

    fun containsAll(elements: ExtendedBitSet): Boolean {
        return containsAll(elements.bitArray)
    }

    fun containsAll(longArray: LongArray): Boolean {
        val a = getMaxSize(bitArray)
        val b = getMaxSize(longArray)
        if (a < b) return false
        val minSize = min(a, b)

        for (i in 0 until minSize) {
            val prev = bitArray[i]
            val other = longArray[i]
            val result = prev or other
            if (result != prev) return false
        }

        return true
    }

    private inline fun toLongArrayNullable(elements: Collection<Int>): LongArray? {
        val longArray = LongArray(bitArray.size)
        for (element in elements) {
            val index = element shr 6
            if (index >= longArray.size) return null

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArrayNullable(elements: IntCollection): LongArray? {
        val longArray = LongArray(bitArray.size)
        val iterator = elements.iterator()
        while (iterator.hasNext()) {
            val element = iterator.nextInt()
            val index = element shr 6
            if (index >= longArray.size) return null

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArrayNullable(elements: Array<Int>): LongArray? {
        val longArray = LongArray(bitArray.size)
        for (i in elements.indices) {
            val element = elements[i]
            val index = element shr 6
            if (index >= longArray.size) return null

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    private inline fun toLongArrayNullable(elements: IntArray): LongArray? {
        val longArray = LongArray(bitArray.size)
        for (i in elements.indices) {
            val element = elements[i]
            val index = element shr 6
            if (index >= longArray.size) return null

            val bit = 1L shl (element and 0x3F)
            val prev = longArray[index]
            longArray[index] = prev or bit
        }
        return longArray
    }

    override fun isEmpty(): Boolean {
        for (bit in bitArray) {
            if (bit != 0L) return false
        }

        return true
    }

    fun ensureCapacity(capacity: Int) {
        ensureArrayLength((capacity + 63) shr 6)
    }

    fun ensureArrayLength(length: Int) {
        if (bitArray.size < length) {
            bitArray = bitArray.copyOf(length)
        }
    }

    private inline fun getBit(index: Int): Long {
        return if (index >= bitArray.size) {
            0L
        } else {
            bitArray[index]
        }
    }

    private inline fun putBit(index: Int, bit: Long) {
        ensureArrayLength(index + 1)
        bitArray[index] = bit
    }

    override fun iterator(): MutableIntIterator {
        return ExtendedBitSetIterator()
    }

    override fun toString(): String {
        return joinToString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtendedBitSet

        return bitArray.contentEquals(other.bitArray)
    }

    override fun hashCode(): Int {
        return bitArray.contentHashCode()
    }

    private inner class ExtendedBitSetIterator : MutableIntIterator {
        private var arrayIndex = 0
        private var bitIndex = 0

        private var prevArrayIndex = -1
        private var prevBitIndex = -1

        init {
            skipAhead()
        }

        override fun nextInt(): Int {
            prevArrayIndex = arrayIndex
            prevBitIndex = bitIndex++

            if (bitIndex >= 64) {
                arrayIndex++
                bitIndex = 0
            }

            val result = (prevArrayIndex shl 6) + prevBitIndex
            skipAhead()
            return result
        }

        override fun hasNext(): Boolean {
            return arrayIndex < bitArray.size
        }

        override fun remove() {
            if (prevBitIndex == -1) {
                throw IllegalStateException()
            }

            val bitMask = 1L shl prevBitIndex
            bitArray[prevArrayIndex] = bitArray[prevArrayIndex] and bitMask.inv()
        }

        fun skipAhead() {
            loop@ while (arrayIndex < bitArray.size) {
                val bits = bitArray[arrayIndex]

                while (bitIndex < 64) {
                    val bitMask = 1L shl bitIndex
                    if (bits and bitMask != 0L) break@loop
                    bitIndex++
                }

                arrayIndex++
                bitIndex = 0
            }
        }
    }

    companion object {
        @JvmStatic
        private val EMPTY_LONG_ARRAY = longArrayOf()
    }
}