package dev.fastmc.graphics.shared.font

import dev.fastmc.graphics.shared.opengl.glBindTexture
import dev.fastmc.graphics.shared.opengl.glDeleteTextures
import dev.fastmc.graphics.shared.texture.ITexture

class GlyphTexture(
    override val id: Int,
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