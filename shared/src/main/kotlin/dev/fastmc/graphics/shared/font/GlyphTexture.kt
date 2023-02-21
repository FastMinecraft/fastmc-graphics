package dev.fastmc.graphics.shared.font

import dev.fastmc.graphics.shared.opengl.glDeleteTextures
import dev.fastmc.graphics.shared.texture.ITexture

class GlyphTexture(
    override val id: Int,
    val internalID: Int
) : ITexture {
    override val resourceName = "fontRenderer/$internalID"

    override fun destroy() {
        glDeleteTextures(id)
    }
}