package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.IRenderer
import me.xiaro.fastmc.opengl.*
import me.xiaro.fastmc.resource.IResourceManager
import me.xiaro.fastmc.tileentity.info.IShulkerBoxInfo
import java.nio.ByteBuffer

class ShulkerBoxRenderBuilder : TileEntityRenderBuilder<IShulkerBoxInfo<*>>(20) {
    override fun add(info: IShulkerBoxInfo<*>) {
        val posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        val posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        buffer.putFloat(posX)
        buffer.putFloat(posY)
        buffer.putFloat(posZ)

        putLightMapUV(info.lightMapUV)

        when (info.direction) {
            0 -> {
                buffer.put(0)
                buffer.put(2)
            }
            1 -> {
                buffer.put(0)
                buffer.put(0)
            }
            2 -> {
                buffer.put(2)
                buffer.put(-1)
            }
            3 -> {
                buffer.put(0)
                buffer.put(-1)
            }
            4 -> {
                buffer.put(-1)
                buffer.put(1)
            }
            5 -> {
                buffer.put(1)
                buffer.put(1)
            }
        }

        buffer.put(info.color.toByte())
        buffer.put((info.prevProgress * 255.0f).toInt().toByte())
        buffer.put((info.progress * 255.0f).toInt().toByte())
        buffer.position(buffer.position() + 1)
    }

    override fun uploadBuffer(buffer: ByteBuffer): TileEntityRenderBuilder.Renderer {
        val vaoID = glGenVertexArrays()
        val vboID = glGenBuffers()

        val model = model.get(resourceManager)
        val shader = shader.get(resourceManager)

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW)

        glBindVertexArray(vaoID)

        glVertexAttribPointer(4, 3, GL_FLOAT, false, 20, 0L) // 12
        glVertexAttribPointer(5, 2, GL_UNSIGNED_BYTE, true, 20, 12L) // 2

        glVertexAttribIPointer(6, 1, GL_BYTE, 20, 14L) // 1
        glVertexAttribIPointer(7, 1, GL_BYTE, 20, 15L) // 1
        glVertexAttribIPointer(8, 1, GL_UNSIGNED_BYTE, 20, 16L) // 1
        glVertexAttribPointer(9, 1, GL_UNSIGNED_BYTE, true, 20, 17L) // 1
        glVertexAttribPointer(10, 1, GL_UNSIGNED_BYTE, true, 20, 18L) // 1

        glVertexAttribDivisor(4, 1)
        glVertexAttribDivisor(5, 1)
        glVertexAttribDivisor(6, 1)
        glVertexAttribDivisor(7, 1)
        glVertexAttribDivisor(8, 1)
        glVertexAttribDivisor(9, 1)
        glVertexAttribDivisor(10, 1)

        model.attachVBO()

        glEnableVertexAttribArray(4)
        glEnableVertexAttribArray(5)
        glEnableVertexAttribArray(6)
        glEnableVertexAttribArray(7)
        glEnableVertexAttribArray(8)
        glEnableVertexAttribArray(9)
        glEnableVertexAttribArray(10)

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        return Renderer(renderInfo(shader, vaoID, vboID, model))
    }

    private class Renderer(
        renderInfo: RenderInfo
    ) : TileEntityRenderBuilder.Renderer(renderInfo) {
        override fun preRender() {
            texture.get(resourceManager).bind()
        }

        override fun postRender() {

        }
    }

    private companion object {
        val model = model("ShulkerBox")
        val shader = shader("ShulkerBox")
        val texture = texture("ShulkerBox")
    }
}