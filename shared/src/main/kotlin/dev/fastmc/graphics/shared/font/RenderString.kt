package dev.fastmc.graphics.shared.font

import dev.fastmc.common.ColorARGB
import dev.fastmc.common.collection.FastIntMap
import dev.fastmc.graphics.structs.FontVertex
import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.glwrapper.impl.BufferObject
import dev.luna5ama.glwrapper.impl.GLDataType
import dev.luna5ama.glwrapper.impl.VertexArrayObject
import dev.luna5ama.glwrapper.impl.buildAttribute
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MemoryStack
import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import org.joml.Matrix4f

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
        shader: FontRenderer.FontRendererShaderProgram,
        projection: Matrix4f,
        modelView: Matrix4f,
        color: ColorARGB,
        drawShadow: Boolean
    ) {
        lastAccess = System.currentTimeMillis()

        shader.bind()
        shader.preRender(projection, modelView, color)

        renderInfos.forEach {
            it.render(drawShadow)
        }

        glBindTextureUnit(0, 0)
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
        private val vao: VertexArrayObject
    ) {
        fun render(drawShadow: Boolean) {
            texture.bind(0)
            vao.bind()

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
                val vbo = BufferObject.Immutable()
                MemoryStack {
                    val buffer = buildVboBuffer()
                    vbo.allocate(buffer.len, buffer.ptr, 0)
                }


                val ebo = BufferObject.Immutable()
                MemoryStack {
                    val buffer = buildEboBuffer()
                    ebo.allocate(buffer.len, buffer.ptr, 0)
                }

                val vao = VertexArrayObject()
                vao.attachVbo(vbo, VERTEX_ATTRIBUTE)
                vao.attachEbo(ebo)

                return RenderInfo(texture, size, vao)
            }

            private fun MemoryStack.buildVboBuffer(): Arr {
                val vboBuffer = calloc(size * 4 * 2 * 16L)

                var posIndex = 0
                var uvIndex = 0

                var struct = FontVertex(vboBuffer)

                for (i in colorList.indices) {
                    val color = colorList.getByte(i)
                    val overrideColor = (if (color.toInt() == 0) 1 else 0).toByte()

                    var posX = posList.getFloat(posIndex++)
                    var posY = posList.getFloat(posIndex++)
                    var u = uvList.getShort(uvIndex++)
                    var v = uvList.getShort(uvIndex++)

                    struct.position.x = posX + 1.0f
                    struct.position.y = posY + 1.0f
                    struct.vertUV.x = u
                    struct.vertUV.y = v
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = 1
                    struct++

                    struct.position.x = posX
                    struct.position.y = posY
                    struct.vertUV.x = u
                    struct.vertUV.y = v
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = 0
                    struct++

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    struct.position.x = posX + 1.0f
                    struct.position.y = posY + 1.0f
                    struct.vertUV.x = u
                    struct.vertUV.y = v
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = 1
                    struct++

                    struct.position.x = posX
                    struct.position.y = posY
                    struct.vertUV.x = u
                    struct.vertUV.y = v
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = 0
                    struct++

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    struct.position.x = posX + 1.0f
                    struct.position.y = posY + 1.0f
                    struct.vertUV.x = u
                    struct.vertUV.y = v
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = 1
                    struct++

                    struct.position.x = posX
                    struct.position.y = posY
                    struct.vertUV.x = u
                    struct.vertUV.y = v
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = 0
                    struct++

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    struct.position.x = posX + 1.0f
                    struct.position.y = posY + 1.0f
                    struct.vertUV.x = u
                    struct.vertUV.y = v
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = 1
                    struct++

                    struct.position.x = posX
                    struct.position.y = posY
                    struct.vertUV.x = u
                    struct.vertUV.y = v
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = 0
                    struct++
                }

                return vboBuffer
            }

            private fun MemoryStack.buildEboBuffer(): Arr {
                val iboBuffer = malloc(size * 2 * 6 * 2L)

                val indexSize = size * 2 * 4
                var index = 0
                var ptr = iboBuffer.ptr

                while (index < indexSize) {
                    ptr = ptr.setShortInc(index.toShort())
                        .setShortInc((index + 4).toShort())
                        .setShortInc((index + 2).toShort())
                        .setShortInc((index + 6).toShort())
                        .setShortInc((index + 2).toShort())
                        .setShortInc((index + 4).toShort())
                    index += 8
                }

                index = 0

                while (index < indexSize) {
                    ptr = ptr.setShortInc((index + 1).toShort())
                        .setShortInc((index + 5).toShort())
                        .setShortInc((index + 3).toShort())
                        .setShortInc((index + 7).toShort())
                        .setShortInc((index + 3).toShort())
                        .setShortInc((index + 5).toShort())
                    index += 8
                }

                return iboBuffer
            }

            private companion object {
                val VERTEX_ATTRIBUTE = buildAttribute(16) {
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