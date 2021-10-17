package me.xiaro.fastmc.font

import me.xiaro.fastmc.opengl.ITexture
import me.xiaro.fastmc.opengl.glBindTexture
import me.xiaro.fastmc.opengl.glDeleteTextures

class GlyphTexture(
    val id: Int,
    val internalID: Int
) : ITexture {
    override val resourceName = "fontRenderer/$internalID"

    override fun bind() {
        glBindTexture(id)
    }

    override fun destroy() {
        glDeleteTextures(id)
    }
}