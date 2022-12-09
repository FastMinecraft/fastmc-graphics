package me.luna.fastmc.renderer

import com.mojang.blaze3d.systems.RenderSystem
import dev.fastmc.common.ColorARGB
import me.luna.fastmc.resource.toBufferedImage
import me.luna.fastmc.shared.font.FontRenderer
import me.luna.fastmc.shared.font.IFontRendererWrapper
import me.luna.fastmc.shared.opengl.glUseProgramForce
import me.luna.fastmc.util.ResourceLocation
import net.minecraft.resource.ResourceManager
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA

class FontRendererWrapper(resourceManager: ResourceManager) : IFontRendererWrapper {
    override val wrapped: FontRenderer

    init {
        val asciiFont = ResourceLocation("textures/font/ascii.png").toBufferedImage(resourceManager)

        val unicodeFonts = Array(256) {
            runCatching {
                ResourceLocation("textures/font/unicode_page_%02x.png".format(it)).toBufferedImage(resourceManager)
            }.getOrNull()
        }

        val glyphWidths = ByteArray(65536)
        resourceManager.getResource(ResourceLocation("font/glyph_sizes.bin")).use {
            it.inputStream.read(glyphWidths)
        }

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
        RenderSystem.enableBlend()
        RenderSystem.enableTexture()

        wrapped.drawString(projection, modelView, string, posX, posY, ColorARGB(adjustedColor), scale, drawShadow)

        glUseProgramForce(0)
    }
}