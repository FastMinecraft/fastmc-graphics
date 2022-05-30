package me.luna.fastmc.shared.util

object BlockPosUtil {

    private fun isPowerOfTwo(value: Int): Boolean {
        return value != 0 && value and value - 1 == 0
    }

    private fun log2DeBruijn(input: Int): Int {
        var value = input
        value = if (isPowerOfTwo(value)) value else smallestEncompassingPowerOfTwo(value)
        return intArrayOf(
            0,
            1,
            28,
            2,
            29,
            14,
            24,
            3,
            30,
            22,
            20,
            15,
            25,
            17,
            4,
            8,
            31,
            27,
            13,
            23,
            21,
            19,
            16,
            7,
            26,
            12,
            18,
            6,
            11,
            5,
            10,
            9
        )[(value.toLong() * 125613361L shr 27).toInt() and 31]
    }

    private fun log2(value: Int): Int {
        return log2DeBruijn(value) - if (isPowerOfTwo(value)) 0 else 1
    }

    private fun smallestEncompassingPowerOfTwo(value: Int): Int {
        var i = value - 1
        i = i or i shr 1
        i = i or i shr 2
        i = i or i shr 4
        i = i or i shr 8
        i = i or i shr 16
        return i + 1
    }

    private val NUM_X_BITS: Int = 1 + log2(smallestEncompassingPowerOfTwo(30000000))

    private val NUM_Z_BITS = NUM_X_BITS
    private val NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS
    private val Y_SHIFT = 0 + NUM_Z_BITS
    private val X_SHIFT = Y_SHIFT + NUM_Y_BITS
    private val X_MASK = (1L shl NUM_X_BITS) - 1L
    private val Y_MASK = (1L shl NUM_Y_BITS) - 1L
    private val Z_MASK = (1L shl NUM_Z_BITS) - 1L

    fun xFromLong(packedPos: Long): Int {
        return (packedPos shl 64 - X_SHIFT - NUM_X_BITS shr 64 - NUM_X_BITS).toInt()
    }

    fun yFromLong(packedPos: Long): Int {
        return (packedPos shl 64 - Y_SHIFT - NUM_Y_BITS shr 64 - NUM_Y_BITS).toInt()
    }

    fun zFromLong(packedPos: Long): Int {
        return (packedPos shl 64 - NUM_Z_BITS shr 64 - NUM_Z_BITS).toInt()
    }

    fun toLong(x: Int, y: Int, z: Int): Long {
        return x.toLong() and X_MASK shl X_SHIFT or (y.toLong() and Y_MASK shl Y_SHIFT) or (z.toLong() and Z_MASK shl 0)
    }
}