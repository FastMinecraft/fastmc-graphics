package dev.fastmc.graphics.shared.mixin

import dev.fastmc.common.Direction
import org.joml.Vector3f

interface IPatchedBakedQuad {
    val faceBit: Int get() = 0b111111
    val normal: Vector3f get() = Vector3f(0f, 0f, 0f)

    companion object {
        @JvmStatic
        fun calcFaceBit(
            dx1: Float,
            dy1: Float,
            dz1: Float,
            dx2: Float,
            dy2: Float,
            dz2: Float
        ): Int {
            val nX = dy1 * dz2 - dz1 * dy2
            val nY = dx2 * dz1 - dz2 * dx1
            val nZ = dx1 * dy2 - dy1 * dx2

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

            if (bit == 0b00_00_00) {
                bit = 0b11_11_11
            }

            return bit
        }

        @Suppress("KotlinConstantConditions")
        @JvmStatic
        fun calcFaceBit(vertexData: IntArray, stride: Int): Int {
            val x1 = Float.fromBits(vertexData[0])
            val y1 = Float.fromBits(vertexData[1])
            val z1 = Float.fromBits(vertexData[2])

            val x2 = Float.fromBits(vertexData[0 + stride])
            val y2 = Float.fromBits(vertexData[1 + stride])
            val z2 = Float.fromBits(vertexData[2 + stride])

            val x3 = Float.fromBits(vertexData[0 + stride * 3])
            val y3 = Float.fromBits(vertexData[1 + stride * 3])
            val z3 = Float.fromBits(vertexData[2 + stride * 3])

            val dx1 = x2 - x1
            val dy1 = y2 - y1
            val dz1 = z2 - z1

            val dx2 = x3 - x1
            val dy2 = y3 - y1
            val dz2 = z3 - z1

            val nX = dy1 * dz2 - dz1 * dy2
            val nY = dx2 * dz1 - dz2 * dx1
            val nZ = dx1 * dy2 - dy1 * dx2

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

            if (bit == 0b00_00_00) {
                bit = 0b11_11_11
            }

            return bit
        }
    }
}