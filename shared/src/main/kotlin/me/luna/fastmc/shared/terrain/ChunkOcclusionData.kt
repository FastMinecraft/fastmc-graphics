package me.luna.fastmc.shared.terrain

import dev.fastmc.common.Direction
import dev.fastmc.common.collection.IntArrayFIFOQueueNoShrink
import dev.fastmc.common.collection.StaticBitSet

@Suppress("NOTHING_TO_INLINE")
abstract class ChunkOcclusionData private constructor(private val hash: Int) {
    abstract fun isVisible(from: Direction, to: Direction): Boolean

    abstract fun isVisible(to: Direction): Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkOcclusionData) return false
        if (hash != other.hash) return false
        return true
    }

    override fun hashCode(): Int {
        return hash
    }

    private class Impl(private val visibilitySet: StaticBitSet) : ChunkOcclusionData(visibilitySet.hashCode()) {
        override fun isVisible(from: Direction, to: Direction): Boolean {
            return visibilitySet.containsFast(from.ordinal + to.ordinal * DIRECTION_SIZE)
        }

        override fun isVisible(to: Direction): Boolean {
            return visibilitySet.containsFast(to.ordinal + DIRECTION_SIZE * 2)
        }
    }

    companion object {
        @JvmStatic
        private val DIRECTION_SIZE = Direction.VALUES.size

        @JvmField
        val EMPTY = object : ChunkOcclusionData(0) {
            override fun isVisible(from: Direction, to: Direction): Boolean {
                return true
            }

            override fun isVisible(to: Direction): Boolean {
                return true
            }
        }

        @JvmField
        val FULL = object : ChunkOcclusionData(0) {
            override fun isVisible(from: Direction, to: Direction): Boolean {
                return false
            }

            override fun isVisible(to: Direction): Boolean {
                return false
            }
        }
    }

    class Builder {
        private val blockedBitSet = StaticBitSet(4096)
        private val floodFillQueue = IntArrayFIFOQueueNoShrink(4096)
        private var blockedCount = 0
        private var directionBits = 0

        fun clear() {
            blockedBitSet.clear()
            blockedCount = 0
            directionBits = 0
        }

        fun markBlocked(x: Int, y: Int, z: Int) {
            blockedBitSet.add(pos2Index(x, y, z))
            blockedCount++
        }

        fun build(): ChunkOcclusionData {
            return when {
                blockedCount < 256 -> {
                    EMPTY
                }
                blockedCount == 4096 -> {
                    FULL
                }
                else -> {
                    val visibilityBitSet = StaticBitSet(DIRECTION_SIZE * DIRECTION_SIZE)

                    for (i in EDGE_DIRECTION_BIT.indices) {
                        if (EDGE_DIRECTION_BIT[i] == NO_DIRECTION || blockedBitSet.containsFast(i)) continue
                        floodFill(i)

                        for (i1 in 0 until DIRECTION_SIZE) {
                            if (directionBits and (1 shl i1) == 0) continue
                            visibilityBitSet.addFast(i1 + DIRECTION_SIZE * 2)
                            for (i2 in 0 until DIRECTION_SIZE) {
                                if (directionBits and (1 shl i2) == 0) continue
                                visibilityBitSet.addFast(i1 + i2 * DIRECTION_SIZE)
                                visibilityBitSet.addFast(i2 + i1 * DIRECTION_SIZE)
                            }
                        }
                    }

                    Impl(visibilityBitSet)
                }
            }
        }

        private fun floodFill(startPos: Int) {
            directionBits = 0
            blockedBitSet.addFast(startPos)
            floodFillQueue.enqueue(startPos)

            while (!floodFillQueue.isEmpty) {
                val pos = floodFillQueue.dequeueInt()
                directionBits = directionBits or EDGE_DIRECTION_BIT[pos].toInt()
                for (direction in Direction.VALUES) {
                    val nextX = ((pos shr 4) and 15) + direction.offsetX
                    val nextY = (pos and 15) + direction.offsetY
                    val nextZ = ((pos shr 8) and 15) + direction.offsetZ
                    if ((nextX and -16) or (nextY and -16) or (nextZ and -16) != 0) continue

                    val nextPos = pos2Index(nextX, nextY, nextZ)
                    if (blockedBitSet.containsFast(nextPos)) continue
                    blockedBitSet.addFast(nextPos)
                    floodFillQueue.enqueue(nextPos)
                }
            }
        }

        fun reset() {
            blockedBitSet.clear()
            blockedCount = 0
        }

        private companion object {
            const val NO_DIRECTION: Byte = 0

            @JvmField
            val EDGE_DIRECTION_BIT = ByteArray(4096)

            init {
                for (i in EDGE_DIRECTION_BIT.indices) {
                    val x = (i shr 4) and 15
                    val y = i and 15
                    val z = (i shr 8) and 15
                    var bit = 0

                    if (y == 0) {
                        bit = bit or Direction.B_DOWN
                    } else if (y == 15) {
                        bit = bit or Direction.B_UP
                    }

                    if (z == 0) {
                        bit = bit or Direction.B_NORTH
                    } else if (z == 15) {
                        bit = bit or Direction.B_SOUTH
                    }

                    if (x == 0) {
                        bit = bit or Direction.B_WEST
                    } else if (x == 15) {
                        bit = bit or Direction.B_EAST
                    }

                    EDGE_DIRECTION_BIT[i] = bit.toByte()
                }
            }

            @JvmStatic
            inline fun pos2Index(x: Int, y: Int, z: Int): Int {
                return (x shl 4) or y or (z shl 8)
            }
        }
    }
}