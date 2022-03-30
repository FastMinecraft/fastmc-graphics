package me.luna.fastmc.terrain

import me.luna.fastmc.shared.terrain.IVertexDataTransformer
import me.luna.fastmc.shared.util.skip
import java.nio.ByteBuffer
import java.nio.ByteOrder

object VertexDataTransformer : IVertexDataTransformer {
    override val vertexSize: Int
        get() = 16

    @Suppress("FloatingPointLiteralPrecision")
    override fun transform(
        offsetX: Float,
        offsetY: Float,
        offsetZ: Float,
        vertexCount: Int,
        input: ByteBuffer,
        output: ByteBuffer
    ) {
        input.order(ByteOrder.nativeOrder())
        for (i in 0 until vertexCount) {
            output.putShort(((input.float + offsetX + 16) * 227.5555572509765625f).toInt().toShort())
            output.putShort(((input.float + offsetY + 16) * 227.5555572509765625f).toInt().toShort())
            output.putShort(((input.float + offsetZ + 16) * 227.5555572509765625f).toInt().toShort())

            val r = input.get()
            val g = input.get()
            val b = input.get()
            val a = input.get()

            val u = input.float
            val v = input.float

            output.put(input.short.toByte())
            output.put(input.short.toByte())

            output.putShort((u * 65535.0f).toInt().toShort())
            output.putShort((v * 65535.0f).toInt().toShort())

            output.put(r)
            output.put(g)
            output.put(b)
            output.put(a)

            input.skip(4)
        }
        input.rewind()
        output.flip()
    }
}