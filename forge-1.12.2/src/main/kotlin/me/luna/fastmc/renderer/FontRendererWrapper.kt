package me.luna.fastmc.renderer

import me.luna.fastmc.resource.readImage
import me.luna.fastmc.shared.font.FontRenderer
import me.luna.fastmc.shared.font.IFontRendererWrapper
import me.luna.fastmc.shared.opengl.glUseProgramForce
import me.luna.fastmc.shared.util.ColorARGB
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA

class FontRendererWrapper(mc: Minecraft) : IFontRendererWrapper {
    override val wrapped: FontRenderer

    init {
        val asciiFont = ResourceLocation("textures/font/ascii.png").readImage(mc)

        val unicodeFonts = Array(256) {
            runCatching {
                ResourceLocation("textures/font/unicode_page_%02x.png".format(it)).readImage(mc)
            }.getOrNull()
        }

        val glyphWidths = ByteArray(65536)
        val glyphSizes = mc.resourceManager.getResource(ResourceLocation("font/glyph_sizes.bin"))
        glyphSizes.inputStream.read(glyphWidths)

        wrapped = FontRenderer(asciiFont, unicodeFonts, glyphWidths, Minecraft.getGLMaximumTextureSize())
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

        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.enableTexture2D()

        wrapped.drawString(projection, modelView, string, posX, posY, ColorARGB(adjustedColor), scale, drawShadow)

        GlStateManager.enableAlpha()
        glUseProgramForce(0)
    }
}