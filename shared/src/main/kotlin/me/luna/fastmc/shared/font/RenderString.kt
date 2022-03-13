package me.luna.fastmc.shared.font

import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.util.BufferUtils
import me.luna.fastmc.shared.util.ColorARGB
import me.luna.fastmc.shared.util.collection.FastIntMap
import me.luna.fastmc.shared.util.skip
import org.joml.Matrix4f
import java.nio.ByteBuffer

class RenderString(fontRenderer: FontRenderer, private val string: CharSequence) {
    private val renderInfos: Array<RenderInfo>
    private var lastAccess = System.currentTimeMillis()

    init {
        val builders = FastIntMap<RenderInfo.Builder>()

        var posX = 0.0f
        var posY = 0.0f

        val context = FontRenderContext()

        for ((index, char) in string.withIndex()) {
            if (context.checkFormatCode(string, index)) continue

            if (char == '\n') {
                posY += fontRenderer.fontHeight
                posX = 0.0f
            } else {
                val charCode = char.code
                val blockID = charCode shr 8
                val glyphBlock = fontRenderer.getBlock(blockID) ?: continue

                var renderInfo = builders[glyphBlock.texture.internalID]
                if (renderInfo == null) {
                    renderInfo = RenderInfo.Builder(glyphBlock.texture)
                    builders[glyphBlock.texture.internalID] = renderInfo
                }

                val charInfo = glyphBlock.glyphs[charCode - (blockID shl 8)]

                if (context.bold) {
                    val offset = if (glyphBlock.unicode) 0.5f else 1.0f
                    renderInfo.put(posX + offset, posY, charInfo, context)
                }
                renderInfo.put(posX, posY, charInfo, context)

                posX += charInfo.width
            }
        }

        val list = ArrayList<RenderInfo>()

        builders.forEach {
            list.add(it.value.build())
        }

        renderInfos = list.toTypedArray()
    }

    fun render(
        shader: FontRenderer.Shader,
        projection: Matrix4f,
        modelView: Matrix4f,
        color: ColorARGB,
        drawShadow: Boolean
    ) {
        lastAccess = System.currentTimeMillis()

        glUseProgramForce(shader.id)
        shader.preRender(projection, modelView, color)

        renderInfos.forEach {
            it.render(drawShadow)
        }

        glBindTexture(0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        shader.unbind()
    }

    fun tryClean(current: Long): Boolean {
        return if (current - lastAccess >= 5000L) {
            destroy()
            true
        } else {
            false
        }
    }

    fun destroy() {
        renderInfos.forEach {
            it.destroy()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RenderString

        if (string != other.string) return false

        return true
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }

    private class RenderInfo private constructor(
        private val texture: GlyphTexture,
        private val size: Int,
        private val vao: VertexArrayObject,
        private val ibo: IndexBufferObject
    ) {
        fun render(drawShadow: Boolean) {
            texture.bind()

            vao.bind()
            ibo.bind()

            if (drawShadow) {
                glDrawElements(GL_TRIANGLES, size * 2 * 6, GL_UNSIGNED_SHORT, 0L)
            } else {
                glDrawElements(GL_TRIANGLES, size * 6, GL_UNSIGNED_SHORT, size * 6L * 2L)
            }
        }

        fun destroy() {
            vao.destroy()
        }

        class Builder(private val texture: GlyphTexture) {
            private var size = 0

            private val posList = FloatArrayList()
            private val uvList = ShortArrayList()
            private val colorList = ByteArrayList()

            fun put(posX: Float, posY: Float, charGlyph: CharGlyph, context: FontRenderContext) {
                val uv = charGlyph.uv
                val color = context.color + 1
                val offset = if (context.italic) 1.0f else 0.0f

                posList.add(posX + offset)
                posList.add(posY)
                uvList.add(uv[0])
                uvList.add(uv[1])

                posList.add(posX + offset + charGlyph.renderWidth)
                posList.add(posY)
                uvList.add(uv[2])
                uvList.add(uv[1])

                posList.add(posX - offset)
                posList.add(posY + charGlyph.renderHeight)
                uvList.add(uv[0])
                uvList.add(uv[3])

                posList.add(posX - offset + charGlyph.renderWidth)
                posList.add(posY + charGlyph.renderHeight)
                uvList.add(uv[2])
                uvList.add(uv[3])

                colorList.add(color.toByte())

                size++
            }

            fun build(): RenderInfo {
                val vboBuffer = buildVboBuffer()
                val iboBuffer = buildIboBuffer()

                val vao = VertexArrayObject()
                val vbo = VertexBufferObject()
                val ibo = IndexBufferObject()

                glNamedBufferStorage(vbo.id, vboBuffer, 0)
                glNamedBufferStorage(ibo.id, iboBuffer, 0)

                vertexAttribute.apply(vao, vbo)

                return RenderInfo(texture, size, vao, ibo)
            }

            private fun buildVboBuffer(): ByteBuffer {
                val vboBuffer = BufferUtils.byte(size * 4 * 2 * 16)

                var posIndex = 0
                var uvIndex = 0

                for (i in colorList.indices) {
                    val color = colorList.getByte(i)
                    val overrideColor = (if (color.toInt() == 0) 1 else 0).toByte()

                    var posX = posList.getFloat(posIndex++)
                    var posY = posList.getFloat(posIndex++)
                    var u = uvList.getShort(uvIndex++)
                    var v = uvList.getShort(uvIndex++)

                    vboBuffer.putFloat(posX + 1.0f)
                    vboBuffer.putFloat(posY + 1.0f)
                    vboBuffer.putShort(u)
                    vboBuffer.putShort(v)
                    vboBuffer.put(color)
                    vboBuffer.put(overrideColor)
                    vboBuffer.put(1)
                    vboBuffer.skip(1)

                    vboBuffer.putFloat(posX)
                    vboBuffer.putFloat(posY)
                    vboBuffer.putShort(u)
                    vboBuffer.putShort(v)
                    vboBuffer.put(color)
                    vboBuffer.put(overrideColor)
                    vboBuffer.put(0)
                    vboBuffer.skip(1)

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    vboBuffer.putFloat(posX + 1.0f)
                    vboBuffer.putFloat(posY + 1.0f)
                    vboBuffer.putShort(u)
                    vboBuffer.putShort(v)
                    vboBuffer.put(color)
                    vboBuffer.put(overrideColor)
                    vboBuffer.put(1)
                    vboBuffer.skip(1)

                    vboBuffer.putFloat(posX)
                    vboBuffer.putFloat(posY)
                    vboBuffer.putShort(u)
                    vboBuffer.putShort(v)
                    vboBuffer.put(color)
                    vboBuffer.put(overrideColor)
                    vboBuffer.put(0)
                    vboBuffer.skip(1)

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    vboBuffer.putFloat(posX + 1.0f)
                    vboBuffer.putFloat(posY + 1.0f)
                    vboBuffer.putShort(u)
                    vboBuffer.putShort(v)
                    vboBuffer.put(color)
                    vboBuffer.put(overrideColor)
                    vboBuffer.put(1)
                    vboBuffer.skip(1)

                    vboBuffer.putFloat(posX)
                    vboBuffer.putFloat(posY)
                    vboBuffer.putShort(u)
                    vboBuffer.putShort(v)
                    vboBuffer.put(color)
                    vboBuffer.put(overrideColor)
                    vboBuffer.put(0)
                    vboBuffer.skip(1)

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    vboBuffer.putFloat(posX + 1.0f)
                    vboBuffer.putFloat(posY + 1.0f)
                    vboBuffer.putShort(u)
                    vboBuffer.putShort(v)
                    vboBuffer.put(color)
                    vboBuffer.put(overrideColor)
                    vboBuffer.put(1)
                    vboBuffer.skip(1)

                    vboBuffer.putFloat(posX)
                    vboBuffer.putFloat(posY)
                    vboBuffer.putShort(u)
                    vboBuffer.putShort(v)
                    vboBuffer.put(color)
                    vboBuffer.put(overrideColor)
                    vboBuffer.put(0)
                    vboBuffer.skip(1)
                }

                vboBuffer.flip()
                return vboBuffer
            }

            private fun buildIboBuffer(): ByteBuffer {
                val iboBuffer = BufferUtils.byte(size * 2 * 6 * 2)

                val indexSize = size * 2 * 4
                var index = 0

                while (index < indexSize) {
                    iboBuffer.putShort(index.toShort())
                    iboBuffer.putShort((index + 4).toShort())
                    iboBuffer.putShort((index + 2).toShort())
                    iboBuffer.putShort((index + 6).toShort())
                    iboBuffer.putShort((index + 2).toShort())
                    iboBuffer.putShort((index + 4).toShort())
                    index += 8
                }

                index = 0

                while (index < indexSize) {
                    iboBuffer.putShort((index + 1).toShort())
                    iboBuffer.putShort((index + 5).toShort())
                    iboBuffer.putShort((index + 3).toShort())
                    iboBuffer.putShort((index + 7).toShort())
                    iboBuffer.putShort((index + 3).toShort())
                    iboBuffer.putShort((index + 5).toShort())
                    index += 8
                }

                iboBuffer.flip()
                return iboBuffer
            }

            private companion object {
                val vertexAttribute = buildAttribute(16) {
                    float(0, 2, GLDataType.GL_FLOAT, false)
                    float(1, 2, GLDataType.GL_UNSIGNED_SHORT, true)
                    int(2, 1, GLDataType.GL_BYTE)
                    float(3, 1, GLDataType.GL_UNSIGNED_BYTE, false)
                    float(4, 1, GLDataType.GL_UNSIGNED_BYTE, false)
                }
            }
        }
    }
}