package me.xiaro.fastmc.shared.font

import me.xiaro.fastmc.shared.texture.ITexture
import me.xiaro.fastmc.shared.opengl.glBindTexture
import me.xiaro.fastmc.shared.opengl.glDeleteTextures

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