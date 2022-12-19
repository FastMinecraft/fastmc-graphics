package dev.fastmc.graphics.shared.terrain

import kotlin.math.max
import kotlin.math.min

abstract class BlockRenderer<T_BlockState, T_FluidState> {
    abstract val context: RebuildContext
    abstract val worldSnapshot: WorldSnapshot112<*, T_BlockState, *>

    abstract fun renderBlock(state: T_BlockState)
    abstract fun renderFluid(state: T_FluidState, blockState: T_BlockState)

    @Suppress("UNCHECKED_CAST")
    protected fun renderQuadSmooth(
        state: T_BlockState,
        vertexData: IntArray,
        vertexSize: Int,
        face: Int,
        faceBit: Int,
        diffuseLighting: Boolean,
        hasColor: Boolean
    ) {
        getQuadDimensions(face, state, vertexData, vertexSize)
        context.calculateAO(context.blockX, context.blockY, context.blockZ, face, diffuseLighting)

        val rMul: Int
        val gMul: Int
        val bMul: Int

        if (hasColor) {
            val color = (context.worldSnapshot as WorldSnapshot112<*, T_BlockState, *>).getBlockColor(
                context.blockX,
                context.blockY,
                context.blockZ,
                state
            )
            rMul = color shr 16 and 255
            gMul = color shr 8 and 255
            bMul = color and 255
        } else {
            rMul = 255
            gMul = 255
            bMul = 255
        }

        var color = vertexData[3]
        var brightness = context.brightnessArray[0]
        context.activeVertexBuilder.putVertex(
            Float.fromBits(vertexData[0]) + context.renderPosX,
            Float.fromBits(vertexData[1]) + context.renderPosY,
            Float.fromBits(vertexData[2]) + context.renderPosZ,
            (color shr 16 and 255) * rMul * brightness shr 16,
            (color shr 8 and 255) * gMul * brightness shr 16,
            (color and 255) * bMul * brightness shr 16,
            Float.fromBits(vertexData[4]),
            Float.fromBits(vertexData[5]),
            context.lightMapUVArray[0],
            faceBit
        )

        color = vertexData[3 + vertexSize]
        brightness = context.brightnessArray[1]
        context.activeVertexBuilder.putVertex(
            Float.fromBits(vertexData[0 + vertexSize]) + context.renderPosX,
            Float.fromBits(vertexData[1 + vertexSize]) + context.renderPosY,
            Float.fromBits(vertexData[2 + vertexSize]) + context.renderPosZ,
            (color shr 16 and 255) * rMul * brightness shr 16,
            (color shr 8 and 255) * gMul * brightness shr 16,
            (color and 255) * bMul * brightness shr 16,
            Float.fromBits(vertexData[4 + vertexSize]),
            Float.fromBits(vertexData[5 + vertexSize]),
            context.lightMapUVArray[1],
            faceBit
        )

        color = vertexData[3 + vertexSize * 2]
        brightness = context.brightnessArray[2]
        context.activeVertexBuilder.putVertex(
            Float.fromBits(vertexData[0 + vertexSize * 2]) + context.renderPosX,
            Float.fromBits(vertexData[1 + vertexSize * 2]) + context.renderPosY,
            Float.fromBits(vertexData[2 + vertexSize * 2]) + context.renderPosZ,
            (color shr 16 and 255) * rMul * brightness shr 16,
            (color shr 8 and 255) * gMul * brightness shr 16,
            (color and 255) * bMul * brightness shr 16,
            Float.fromBits(vertexData[4 + vertexSize * 2]),
            Float.fromBits(vertexData[5 + vertexSize * 2]),
            context.lightMapUVArray[2],
            faceBit
        )

        color = vertexData[3 + vertexSize * 3]
        brightness = context.brightnessArray[3]
        context.activeVertexBuilder.putVertex(
            Float.fromBits(vertexData[0 + vertexSize * 3]) + context.renderPosX,
            Float.fromBits(vertexData[1 + vertexSize * 3]) + context.renderPosY,
            Float.fromBits(vertexData[2 + vertexSize * 3]) + context.renderPosZ,
            (color shr 16 and 255) * rMul * brightness shr 16,
            (color shr 8 and 255) * gMul * brightness shr 16,
            (color and 255) * bMul * brightness shr 16,
            Float.fromBits(vertexData[4 + vertexSize * 3]),
            Float.fromBits(vertexData[5 + vertexSize * 3]),
            context.lightMapUVArray[3],
            faceBit
        )
        context.activeVertexBuilder.putQuad(faceBit)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun renderQuadFlat(
        state: T_BlockState,
        vertexData: IntArray,
        vertexSize: Int,
        face: Int,
        faceBit: Int,
        diffuseLighting: Boolean,
        hasColor: Boolean,
        light: Int
    ) {
        val brightness = context.worldSnapshot.getWorldBrightness(face, diffuseLighting)
        val rMul: Int
        val gMul: Int
        val bMul: Int

        if (hasColor) {
            val color = (context.worldSnapshot as WorldSnapshot112<*, T_BlockState, *>).getBlockColor(
                context.blockX,
                context.blockY,
                context.blockZ,
                state
            )
            rMul = color shr 16 and 255
            gMul = color shr 8 and 255
            bMul = color and 255
        } else {
            rMul = 255
            gMul = 255
            bMul = 255
        }

        var color = vertexData[3]
        context.activeVertexBuilder.putVertex(
            Float.fromBits(vertexData[0]) + context.renderPosX,
            Float.fromBits(vertexData[1]) + context.renderPosY,
            Float.fromBits(vertexData[2]) + context.renderPosZ,
            (color shr 16 and 255) * rMul * brightness shr 16,
            (color shr 8 and 255) * gMul * brightness shr 16,
            (color and 255) * bMul * brightness shr 16,
            Float.fromBits(vertexData[4]),
            Float.fromBits(vertexData[5]),
            light,
            faceBit
        )

        color = vertexData[3 + vertexSize]
        context.activeVertexBuilder.putVertex(
            Float.fromBits(vertexData[0 + vertexSize]) + context.renderPosX,
            Float.fromBits(vertexData[1 + vertexSize]) + context.renderPosY,
            Float.fromBits(vertexData[2 + vertexSize]) + context.renderPosZ,
            (color shr 16 and 255) * rMul * brightness shr 16,
            (color shr 8 and 255) * gMul * brightness shr 16,
            (color and 255) * bMul * brightness shr 16,
            Float.fromBits(vertexData[4 + vertexSize]),
            Float.fromBits(vertexData[5 + vertexSize]),
            light,
            faceBit
        )

        color = vertexData[3 + vertexSize * 2]
        context.activeVertexBuilder.putVertex(
            Float.fromBits(vertexData[0 + vertexSize * 2]) + context.renderPosX,
            Float.fromBits(vertexData[1 + vertexSize * 2]) + context.renderPosY,
            Float.fromBits(vertexData[2 + vertexSize * 2]) + context.renderPosZ,
            (color shr 16 and 255) * rMul * brightness shr 16,
            (color shr 8 and 255) * gMul * brightness shr 16,
            (color and 255) * bMul * brightness shr 16,
            Float.fromBits(vertexData[4 + vertexSize * 2]),
            Float.fromBits(vertexData[5 + vertexSize * 2]),
            light,
            faceBit
        )

        color = vertexData[3 + vertexSize * 3]
        context.activeVertexBuilder.putVertex(
            Float.fromBits(vertexData[0 + vertexSize * 3]) + context.renderPosX,
            Float.fromBits(vertexData[1 + vertexSize * 3]) + context.renderPosY,
            Float.fromBits(vertexData[2 + vertexSize * 3]) + context.renderPosZ,
            (color shr 16 and 255) * rMul * brightness shr 16,
            (color shr 8 and 255) * gMul * brightness shr 16,
            (color and 255) * bMul * brightness shr 16,
            Float.fromBits(vertexData[4 + vertexSize * 3]),
            Float.fromBits(vertexData[5 + vertexSize * 3]),
            light,
            faceBit
        )

        context.activeVertexBuilder.putQuad(faceBit)
    }

    protected fun getQuadDimensions(
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