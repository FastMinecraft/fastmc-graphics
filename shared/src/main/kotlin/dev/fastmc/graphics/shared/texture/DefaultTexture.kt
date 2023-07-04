package dev.fastmc.graphics.shared.texture

import dev.fastmc.common.allocateByte
import dev.fastmc.common.free
import dev.fastmc.graphics.shared.opengl.*
import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.kmogus.Arr
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.nio.ByteBuffer

class DefaultTexture(override val resourceName: String, bufferedImage: BufferedImage) : ITexture {
    override val id = glCreateTextures(GL_TEXTURE_2D)

    init {
        val width = bufferedImage.width
        val height = bufferedImage.height
        val buffer = Arr.malloc(width * height * 4L)

        bufferedImage.getRGBA(buffer)
        glTextureStorage2D(id, 1, GL_RGBA8, width, height)
        glTextureSubImage2D(id, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer.ptr)
        buffer.free()

        glTextureParameteri(id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTextureParameteri(id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    }

    private fun BufferedImage.getRGBA(buffer: Arr) {
        val numBands = raster.numBands

        val data = when (val dataType = raster.dataBuffer.dataType) {
            DataBuffer.TYPE_BYTE -> ByteArray(numBands)
            DataBuffer.TYPE_USHORT -> ShortArray(numBands)
            DataBuffer.TYPE_INT -> IntArray(numBands)
            DataBuffer.TYPE_FLOAT -> FloatArray(numBands)
            DataBuffer.TYPE_DOUBLE -> DoubleArray(numBands)
            else -> throw IllegalArgumentException("Unknown data buffer type: $dataType")
        }

        var ptr = buffer.ptr

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dataElement = raster.getDataElements(x, y, data)
                ptr = ptr.setByteInc(colorModel.getRed(dataElement).toByte())
                ptr = ptr.setByteInc(colorModel.getGreen(dataElement).toByte())
                ptr = ptr.setByteInc(colorModel.getBlue(dataElement).toByte())
                ptr = ptr.setByteInc(colorModel.getAlpha(dataElement).toByte())
            }
        }
    }

    override fun destroy() {
        glDeleteTextures(id)
    }
}