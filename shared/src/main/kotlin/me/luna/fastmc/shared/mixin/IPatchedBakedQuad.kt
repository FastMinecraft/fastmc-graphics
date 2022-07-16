package me.luna.fastmc.shared.mixin

import me.luna.fastmc.shared.util.Direction

interface IPatchedBakedQuad {
    val faceBit: Int get() = 0b111111

    companion object {
        @Suppress("KotlinConstantConditions")
        @JvmStatic
        fun calcFaceBit(vertexData: IntArray, stride: Int): Int {
            val aX = Float.fromBits(vertexData[0])
            val aY = Float.fromBits(vertexData[1])
            val aZ = Float.fromBits(vertexData[2])

            val bX = Float.fromBits(vertexData[0 + stride])
            val bY = Float.fromBits(vertexData[1 + stride])
            val bZ = Float.fromBits(vertexData[2 + stride])

            val cX = Float.fromBits(vertexData[0 + stride + stride])
            val cY = Float.fromBits(vertexData[1 + stride + stride])
            val cZ = Float.fromBits(vertexData[2 + stride + stride])

            val baX = bX - aX
            val baY = bY - aY
            val baZ = bZ - aZ

            val caX = cX - aX
            val caY = cY - aY
            val caZ = cZ - aZ

            val nX = baY * caZ - baZ * caY
            val nY = caX * baZ - caZ * baX
            val nZ = baX * caY - baY * caX

            var bit = 0b00_00_00
            val eps = 0.01f

            if (nX - eps > 0.0f) {
                bit = bit or Direction.B_EAST
            } else if (nX + eps < 0.0f) {
                bit = bit or Direction.B_WEST
            }

            if (nY - eps > 0.0f) {
                bit = bit or Direction.B_UP
            } else if (nY + eps < 0.0f) {
                bit = bit or Direction.B_DOWN
            }

            if (nZ - eps > 0.0f) {
                bit = bit or Direction.B_SOUTH
            } else if (nZ + eps < 0.0f) {
                bit = bit or Direction.B_NORTH
            }

            return bit
        }
    }
}