package me.luna.fastmc.shared.terrain

import kotlin.math.max
import kotlin.math.min

abstract class BlockRenderer<T_BlockState, T_FluidState> {
    abstract val context: RebuildContext
    abstract val worldSnapshot: WorldSnapshot112<*, T_BlockState, *>

    abstract fun renderBlock(state: T_BlockState)
    abstract fun renderFluid(state: T_FluidState, blockState: T_BlockState)

    fun getQuadDimensions(
        face: Int,
        state: T_BlockState,
        vertexData: IntArray,
        vertexSize: Int
    ) {
        var minX = 32.0f
        var minY = 32.0f
        var minZ = 32.0f
        var maxX = -32.0f
        var maxY = -32.0f
        var maxZ = -32.0f

        for (i in 0..3) {
            val vx = Float.fromBits(vertexData[i * vertexSize])
            val vy = Float.fromBits(vertexData[i * vertexSize + 1])
            val vz = Float.fromBits(vertexData[i * vertexSize + 2])
            minX = min(minX, vx)
            minY = min(minY, vy)
            minZ = min(minZ, vz)
            maxX = max(maxX, vx)
            maxY = max(maxY, vy)
            maxZ = max(maxZ, vz)
        }

        context.boxDimension[4] = minX
        context.boxDimension[5] = maxX
        context.boxDimension[0] = minY
        context.boxDimension[1] = maxY
        context.boxDimension[2] = minZ
        context.boxDimension[3] = maxZ
        context.boxDimension[10] = 1.0f - minX
        context.boxDimension[11] = 1.0f - maxX
        context.boxDimension[6] = 1.0f - minY
        context.boxDimension[7] = 1.0f - maxY
        context.boxDimension[8] = 1.0f - minZ
        context.boxDimension[9] = 1.0f - maxZ

        when (face) {
            0 -> {
                context.flags[1] = minX >= 1.0E-4f || minZ >= 1.0E-4f || maxX <= 0.9999f || maxZ <= 0.9999f
                context.flags[0] = minY == maxY && (minY < 1.0E-4f || isFullCube(state))
            }
            1 -> {
                context.flags[1] = minX >= 1.0E-4f || minZ >= 1.0E-4f || maxX <= 0.9999f || maxZ <= 0.9999f
                context.flags[0] = minY == maxY && (maxY > 0.9999f || isFullCube(state))
            }
            2 -> {
                context.flags[1] = minX >= 1.0E-4f || minY >= 1.0E-4f || maxX <= 0.9999f || maxY <= 0.9999f
                context.flags[0] = minZ == maxZ && (minZ < 1.0E-4f || isFullCube(state))
            }
            3 -> {
                context.flags[1] = minX >= 1.0E-4f || minY >= 1.0E-4f || maxX <= 0.9999f || maxY <= 0.9999f
                context.flags[0] = minZ == maxZ && (maxZ > 0.9999f || isFullCube(state))
            }
            4 -> {
                context.flags[1] = minY >= 1.0E-4f || minZ >= 1.0E-4f || maxY <= 0.9999f || maxZ <= 0.9999f
                context.flags[0] = minX == maxX && (minX < 1.0E-4f || isFullCube(state))
            }
            else -> {
                context.flags[1] = minY >= 1.0E-4f || minZ >= 1.0E-4f || maxY <= 0.9999f || maxZ <= 0.9999f
                context.flags[0] = minX == maxX && (maxX > 0.9999f || isFullCube(state))
            }
        }
    }

    abstract fun isFullCube(state: T_BlockState): Boolean
}