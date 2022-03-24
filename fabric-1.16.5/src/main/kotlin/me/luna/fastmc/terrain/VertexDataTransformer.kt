package me.luna.fastmc.terrain

import me.luna.fastmc.shared.terrain.IVertexDataTransformer
import me.luna.fastmc.shared.util.skip
import java.nio.ByteBuffer
import java.nio.ByteOrder

object VertexDataTransformer : IVertexDataTransformer {
    override val vertexSize: Int
        get() = 28

    override fun transform(offsetX: Float, offsetY: Float, offsetZ: Float, vertexCount: Int, input: ByteBuffer, output: ByteBuffer) {
        input.order(ByteOrder.nativeOrder())
        for (i in 0 until vertexCount) {
            output.putFloat(input.float + offsetX)
            output.putFloat(input.float + offsetY)
            output.putFloat(input.float + offsetZ)

            output.put(input.get())
            output.put(input.get())
            output.put(input.get())
            output.put(input.get())

            output.putFloat(input.float)
            output.putFloat(input.float)

            output.putShort(input.short)
            output.putShort(input.short)

            input.skip(4)
        }
        input.rewind()
        output.flip()
    }
}