package me.luna.fastmc.shared.font

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.util.BufferUtils
import me.luna.fastmc.shared.util.ColorARGB
import me.luna.fastmc.shared.util.sq
import org.joml.Matrix4f
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.nio.ByteBuffer
import kotlin.math.min

class FontRenderer(
    asciiFont: BufferedImage,
    unicodeFonts: Array<BufferedImage?>,
    glyphWidths: ByteArray,
    maxTextureSize: Int
) {
    private val asciiBlock: GlyphBlock
    private val glyphBlocks = arrayOfNulls<GlyphBlock>(256)
    private val textures: Array<GlyphTexture>

    private val renderStringMap = Object2ObjectOpenHashMap<CharSequence, RenderString>()
    private var cleanTimer = System.currentTimeMillis()

    val shader = Shader()

    val fontHeight = 8
    var unicode = false

    init {
        check(unicodeFonts.size == 256)
        check(glyphWidths.size == 65536)

        val textureSize = min(maxTextureSize, 4096)
        val fontTextureSize = unicodeFonts.first()!!.width
        val uvScale = 65536 / textureSize

        var glyphID = 0
        var dirty = true

        var texture = GlyphTexture(glCreateTextures(GL_TEXTURE_2D), glyphID++)
        var image = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB)
        var graphics2D = image.createGraphics()

        val buffer = BufferUtils.byte(textureSize.sq)
        val textureList = ArrayList<GlyphTexture>()

        asciiBlock = GlyphBlock(texture, asciiBlock(graphics2D, asciiFont, uvScale), true)

        fun upload() {
            graphics2D.dispose()

            buffer.clear()
            image.getAlpha(buffer)
            buffer.flip()

            glTextureStorage2D(texture.id, 1, GL_COMPRESSED_RED_RGTC1, textureSize, textureSize)
            glTextureSubImage2D(
                texture.id,
                0,
                0,
                0,
                textureSize,
                textureSize,
                GL_RED,
                GL_UNSIGNED_BYTE,
                buffer
            )
            glTextureParameteri(texture.id, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTextureParameteri(texture.id, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            textureList.add(texture)
        }

        var posX = fontTextureSize
        var posY = 0
        for (i in unicodeFonts.indices) {
            val unicodeFont = unicodeFonts[i] ?: continue

            val block = GlyphBlock(
                texture,
                unicodeBlock(graphics2D, unicodeFont, glyphWidths, uvScale, i, posX, posY),
                false
            )
            glyphBlocks[i] = block
            dirty = true

            posX += fontTextureSize
            if (posX >= textureSize) {
                posX = 0
                posY += fontTextureSize
            }

            if (posY >= textureSize) {
                upload()

                dirty = false

                texture = GlyphTexture(glCreateTextures(GL_TEXTURE_2D), glyphID)
                image = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB)
                graphics2D = image.createGraphics()
            }
        }

        if (dirty) {
            upload()
        }

        textures = textureList.toTypedArray()

        glBindTexture(0)
    }

    private fun asciiBlock(graphics: Graphics2D, asciiFont: BufferedImage, uvScale: Int): Array<CharGlyph> {
        graphics.drawImage(asciiFont, 0, 0, null)

        val fontTextureSize = asciiFont.width
        val charSize = fontTextureSize / 16
        val alphaArray = asciiFont.getAlpha()
        val byte0 = 0.toByte()

        val array = Array(256) {
            val x = it % 16
            val y = it / 16

            val width = run {
                // Space
                when (it) {
                    32, 160 -> 4
                    167 -> -1
                    else -> {
                        var maxY = charSize - 1
                        while (maxY >= 0) {
                            val x1 = x * charSize + maxY
                            var transparent = true
                            var maxX = 0
                            while (maxX < charSize && transparent) {
                                val y1 = (y * charSize + maxX) * fontTextureSize

                                if (alphaArray[x1 + y1] != byte0) {
                                    transparent = false
                                }

                                ++maxX
                            }
                            if (!transparent) {
                                break
                            }
                            --maxY
                        }
                        ++maxY

                        (0.5f + (maxY * fontHeight / charSize.toFloat())).toInt() + 1
                    }
                }
            }

            val u = x * charSize
            val v = y * charSize

            CharGlyph(
                width.toFloat(),
                8.0f,
                width - 1.01f,
                7.99f,
                shortArrayOf(
                    (u * uvScale).toShort(),
                    (v * uvScale).toShort(),
                    ((u + width - 1.01f) * uvScale).toInt().toShort(),
                    ((v + 7.99f) * uvScale).toInt().toShort(),
                )
            )
        }

        return array
    }

    private fun BufferedImage.getAlpha(): ByteArray {
        val numBands = raster.numBands

        val data = when (val dataType = raster.dataBuffer.dataType) {
            DataBuffer.TYPE_BYTE -> ByteArray(numBands)
            DataBuffer.TYPE_USHORT -> ShortArray(numBands)
            DataBuffer.TYPE_INT -> IntArray(numBands)
            DataBuffer.TYPE_FLOAT -> FloatArray(numBands)
            DataBuffer.TYPE_DOUBLE -> DoubleArray(numBands)
            else -> throw IllegalArgumentException("Unknown data buffer type: $dataType")
        }

        val alphaArray = ByteArray(width * height)
        var index = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                alphaArray[index++] = colorModel.getAlpha(raster.getDataElements(x, y, data)).toByte()
            }
        }

        return alphaArray
    }

    private fun unicodeBlock(
        graphics: Graphics2D,
        unicodeFont: BufferedImage,
        glyphWidths: ByteArray,
        uvScale: Int,
        index: Int,
        posX: Int,
        posY: Int
    ): Array<CharGlyph> {
        val charSize = unicodeFont.width / 16
        val scale = charSize / fontHeight

        graphics.drawImage(unicodeFont, posX, posY, null)

        val begin = index * 256
        val array = Array(256) {
            val x = it % 16
            val y = it / 16
            val i = glyphWidths[begin + it].toInt() and 255

            val x1 = i ushr 4
            val i2 = i and 15
            val x2 = i2 + 1
            val renderWidth = (x2 - x1 - 0.02f)
            val width = (x2 - x1) / 2.0f + 1.0f

            val x3 = x * charSize + x1
            val y3 = y * charSize

            CharGlyph(
                width,
                8.0f,
                renderWidth / 2.0f,
                7.99f,
                shortArrayOf(
                    ((posX + x3) * uvScale).toShort(),
                    ((posY + y3) * uvScale).toShort(),
                    ((posX + x3 + renderWidth) * uvScale).toInt().toShort(),
                    ((posY + y3 + 7.99f * scale) * uvScale).toInt().toShort()
                )
            )
        }

        return array
    }

    private fun BufferedImage.getAlpha(byteBuffer: ByteBuffer) {
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
                byteBuffer.put(colorModel.getAlpha(raster.getDataElements(x, y, data)).toByte())
            }
        }
    }

    fun getBlock(blockID: Int): GlyphBlock? {
        return if (blockID == 0 && !unicode) {
            asciiBlock
        } else {
            glyphBlocks[blockID]
        }
    }

    private fun getCharGlyph(char: Char): CharGlyph? {
        val charCode = char.code
        val blockID = charCode shr 8
        return getBlock(blockID)?.glyphs?.get(charCode - (blockID shl 8))
    }

    fun getWidth(text: CharSequence): Float {
        var maxLineWidth = 0.0f
        var width = 0.0f
        val context = FontRenderContext()

        for ((index, char) in text.withIndex()) {
            if (char == '\n') {
                if (width > maxLineWidth) maxLineWidth = width
                width = 0.0f
            }
            if (context.checkFormatCode(text, index)) continue
            getCharGlyph(char)?.let {
                width += it.width
            }
        }

        return width
    }

    fun getWidth(char: Char): Float {
        return getCharGlyph(char)?.width ?: 0.0f
    }

    fun drawString(
        projection: Matrix4f,
        modelView: Matrix4f,
        charSequence: CharSequence,
        posX: Float,
        posY: Float,
        color: ColorARGB,
        scale: Float,
        drawShadow: Boolean
    ) {
        val current = System.currentTimeMillis()
        if (current - cleanTimer > 1000L) {
            renderStringMap.values.removeIf {
                it.tryClean(current)
            }
            cleanTimer = current
        }

        val string = charSequence.toString()
        val stringCache = renderStringMap.computeIfAbsent(string) {
            RenderString(this, it)
        }

        modelView
            .translate(posX, posY, 0.0f)
            .scale(scale, scale, 1.0f)

        stringCache.render(shader, projection, modelView, color, drawShadow)
    }

    fun destroy() {
        clearStringCache()
        textures.forEach {
            it.destroy()
        }
        shader.destroy()
    }

    private fun clearStringCache() {
        renderStringMap.values.forEach {
            it.destroy()
        }
        renderStringMap.clear()
    }

    class Shader : DrawShader(
        "fontRenderer",
        "/assets/shaders/FontRenderer.vsh",
        "/assets/shaders/FontRenderer.fsh"
    ) {
        private val defaultColorUniform = glGetUniformLocation(id, "defaultColor")
        private var lastColor = ColorARGB(0)

        init {
            glProgramUniform1i(id, glGetUniformLocation(id, "texture"), 0)
        }

        fun preRender(projection: Matrix4f, modelView: Matrix4f, color: ColorARGB) {
            updateProjectionMatrix(projection)
            updateModelViewMatrix(modelView)
            if (color != lastColor) {
                glProgramUniform4f(id, defaultColorUniform, color.rFloat, color.gFloat, color.bFloat, color.aFloat)
                lastColor = color
            }
        }
    }
}