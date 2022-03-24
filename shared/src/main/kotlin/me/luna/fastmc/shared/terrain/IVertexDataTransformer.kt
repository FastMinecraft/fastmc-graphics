package me.luna.fastmc.shared.terrain

import java.nio.ByteBuffer

interface IVertexDataTransformer {
    val vertexSize: Int

    fun transformedSize(vertexCount: Int): Int {
        return vertexCount * vertexSize
    }

    fun transform(
        offsetX: Float,
        offsetY: Float,
        offsetZ: Float,
        vertexCount: Int,
        input: ByteBuffer,
        output: ByteBuffer
    )
}