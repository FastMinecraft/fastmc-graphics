package me.xiaro.fastmc

import com.mojang.blaze3d.systems.RenderSystem
import me.xiaro.fastmc.font.FontRenderer
import me.xiaro.fastmc.font.IFontRendererWrapper
import me.xiaro.fastmc.opengl.glUseProgramForce
import me.xiaro.fastmc.resource.toBufferedImage
import me.xiaro.fastmc.util.ColorARGB
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*

class FontRendererWrapper(mc: Minecraft) : IFontRendererWrapper {
    override val wrapped: FontRenderer

    init {
        val asciiFont = ResourceLocation("textures/font/ascii.png").toBufferedImage(mc)

        val unicodeFonts = Array(256) {
            runCatching {
                ResourceLocation("textures/font/unicode_page_%02x.png".format(it)).toBufferedImage(mc)
            }.getOrNull()
        }

        val glyphWidths = ByteArray(65536)
        val glyphSizes = mc.resourceManager.getResource(ResourceLocation("font/glyph_sizes.bin"))
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

        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        RenderSystem.disableAlphaTest()
        RenderSystem.enableBlend()
        RenderSystem.enableTexture()

        wrapped.drawString(projection, modelView, string, posX, posY, ColorARGB(adjustedColor), scale, drawShadow)

        RenderSystem.enableAlphaTest()
        glUseProgramForce(0)
    }
}