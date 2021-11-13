package me.xiaro.fastmc.shared.tileentity

import me.xiaro.fastmc.shared.opengl.*
import me.xiaro.fastmc.shared.tileentity.info.IEnderChestInfo
import java.nio.ByteBuffer

class EnderChestRenderBuilder : TileEntityRenderBuilder<IEnderChestInfo<*>>(20) {
    override fun add(info: IEnderChestInfo<*>) {
        putPos(info)
        putLightMapUV(info)
        putHDirection(info.hDirection)

        buffer.putShort((info.prevLidAngle * 65535.0f).toInt().toShort())
        buffer.putShort((info.lidAngle * 65535.0f).toInt().toShort())
        buffer.position(buffer.position() + 1)
    }

    override fun uploadBuffer(buffer: ByteBuffer): TileEntityRenderBuilder.Renderer {
        val model = model.get(resourceManager)
        val shader = shader.get(resourceManager)

        val vaoID = glGenVertexArrays()
        val vboID = glGenBuffers()

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW)

        glBindVertexArray(vaoID)

        glVertexAttribPointer(4, 3, GL_FLOAT, false, 20, 0L) // 12
        glVertexAttribPointer(5, 2, GL_UNSIGNED_BYTE, true, 20, 12L) // 2

        glVertexAttribIPointer(6, 1, GL_BYTE, 20, 14L) // 1
        glVertexAttribPointer(7, 1, GL_UNSIGNED_SHORT, true, 20, 15L) // 2
        glVertexAttribPointer(8, 1, GL_UNSIGNED_SHORT, true, 20, 17L) // 2

        glVertexAttribDivisor(4, 1)
        glVertexAttribDivisor(5, 1)
        glVertexAttribDivisor(6, 1)
        glVertexAttribDivisor(7, 1)
        glVertexAttribDivisor(8, 1)

        model.attachVBO()

        glEnableVertexAttribArray(4)
        glEnableVertexAttribArray(5)
        glEnableVertexAttribArray(6)
        glEnableVertexAttribArray(7)
        glEnableVertexAttribArray(8)

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
        val model = model("SmallChest")
        val texture = texture("EnderChest")
        val shader = shader("EnderChest")
    }
}