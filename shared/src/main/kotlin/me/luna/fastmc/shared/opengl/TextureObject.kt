package me.luna.fastmc.shared.opengl

import java.nio.ByteBuffer

sealed class TextureObject(val target: Int) : IGLObject, IGLTargetBinding {
    override val id = glCreateTextures(target)

    override fun bind(target: Int) {
        glBindTextureUnit(target, id)
    }

    override fun unbind(target: Int) {
        glBindTextureUnit(target, 0)
    }

    override fun destroy() {
        glDeleteTextures(id)
    }

    class Texture2D : TextureObject(GL_TEXTURE_2D) {
        var sizeX = 0; private set
        var sizeY = 0; private set

        fun allocate(levels: Int, internalformat: Int, width: Int, height: Int) {
            sizeX = width
            sizeY = height
            glTextureStorage2D(id, levels, internalformat, width, height)
        }

        fun upload(
            level: Int,
            xoffset: Int,
            yoffset: Int,
            width: Int,
            height: Int,
            format: Int,
            type: Int,
            pixels: ByteBuffer
        ) {
            glTextureSubImage2D(id, level, xoffset, yoffset, width, height, format, type, pixels)
        }
    }
}