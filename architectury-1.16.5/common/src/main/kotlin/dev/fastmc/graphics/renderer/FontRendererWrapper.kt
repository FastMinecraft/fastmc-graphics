package dev.fastmc.graphics.renderer

import com.mojang.blaze3d.systems.RenderSystem
import dev.fastmc.common.ColorARGB
import dev.fastmc.graphics.resource.toBufferedImage
import dev.fastmc.graphics.shared.font.FontRenderer
import dev.fastmc.graphics.shared.font.IFontRendererWrapper
import dev.fastmc.graphics.shared.opengl.*
import dev.fastmc.graphics.util.ResourceLocation
import org.joml.Matrix4f

class FontRendererWrapper(resourceManager: net.minecraft.resource.ResourceManager) : IFontRendererWrapper {
    override val wrapped: FontRenderer

    init {
        val asciiFont = ResourceLocation("textures/font/ascii.png").toBufferedImage(resourceManager)

        val unicodeFonts = Array(256) {
            runCatching {
                ResourceLocation("textures/font/unicode_page_%02x.png".format(it)).toBufferedImage(resourceManager)
            }.getOrNull()
        }

        val glyphWidths = ByteArray(65536)
        val glyphSizes = resourceManager.getResource(ResourceLocation("font/glyph_sizes.bin"))
        glyphSizes.inputStream.read(glyphWidths)

        wrapped = FontRenderer(asciiFont, unicodeFonts, glyphWidths, RenderSystem.maxSupportedTextureSize())
    }

    override fun drawString(
        projection: Matrix4f,
        modelView: Matrix4f,
        string: String,
        posX: Float,
        posY: Float,
        color: Int,
        scale: Float,
        drawShadow: Boolean
    ) {
        var adjustedColor = color
        if (adjustedColor and -67108864 == 0) adjustedColor = color or -16777216

        glEnable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        wrapped.drawString(projection, modelView, string, posX, posY, ColorARGB(adjustedColor), scale, drawShadow)

        glUseProgramForce(0)
    }
}