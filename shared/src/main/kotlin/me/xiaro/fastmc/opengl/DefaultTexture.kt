package me.xiaro.fastmc.opengl

import me.xiaro.fastmc.utils.BufferUtils
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.nio.ByteBuffer

class DefaultTexture(override val resourceName: String, bufferedImage: BufferedImage) : ITexture {
    private val id = glGenTextures()

    init {
        glBindTexture(id)

        val width = bufferedImage.width
        val height = bufferedImage.height
        val buffer = BufferUtils.byte(width * height * 4)

        bufferedImage.getRGBA(buffer)
        buffer.flip()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)

        glBindTexture(0)
    }

    fun BufferedImage.getRGBA(buffer: ByteBuffer) {
        val numBands = raster.numBands

        val data = when (val dataType = raster.dataBuffer.dataType) {
            DataBuffer.TYPE_BYTE -> ByteArray(numBands)
            DataBuffer.TYPE_USHORT -> ShortArray(numBands)
            DataBuffer.TYPE_INT -> IntArray(numBands)
            DataBuffer.TYPE_FLOAT -> FloatArray(numBands)
            DataBuffer.TYPE_DOUBLE -> DoubleArray(numBands)
            else -> throw IllegalArgumentException("Unknown data buffer type: $dataType")
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                buffer.putInt(colorModel.getRGB(raster.getDataElements(x, y, data)))
            }
        }
    }


    override fun bind() {
        glBindTexture(id)
    }

    override fun destroy() {
        glDeleteTextures(id)
    }
}