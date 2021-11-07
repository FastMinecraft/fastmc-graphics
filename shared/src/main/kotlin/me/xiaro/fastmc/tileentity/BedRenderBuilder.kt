package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.opengl.*
import me.xiaro.fastmc.resource.IResourceManager
import me.xiaro.fastmc.tileentity.info.IBedInfo
import java.nio.ByteBuffer

class BedRenderBuilder(
    resourceManager: IResourceManager,
    builtPosX: Double,
    builtPosY: Double,
    builtPosZ: Double,
    size: Int
) : TileEntityRenderBuilder<IBedInfo<*>>(resourceManager, builtPosX, builtPosY, builtPosZ, size, 20) {
    override fun add(info: IBedInfo<*>) {
        putPos(info)
        putLightMapUV(info.lightMapUV)

        when (info.direction) {
            1 -> {
                buffer.put(-1)
            }
            2 -> {
                buffer.put(0)
            }
            3 -> {
                buffer.put(1)
            }
            else -> {
                buffer.put(2)
            }
        }

        buffer.put(info.color.toByte())
        buffer.put(if (info.isHead) 1 else 0)
        buffer.position(buffer.position() + 3)
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
        glVertexAttribIPointer(7, 1, GL_UNSIGNED_BYTE, 20, 15L) // 1
        glVertexAttribIPointer(8, 1, GL_UNSIGNED_BYTE, 20, 16L) // 1

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
        val model = model("Bed")
        val shader = shader("Bed")
        val texture = texture("Bed")
    }
}