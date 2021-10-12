package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.model.Model
import me.xiaro.fastmc.opengl.*
import me.xiaro.fastmc.resource.IResourceManager
import me.xiaro.fastmc.resource.ResourceEntry
import me.xiaro.fastmc.tileentity.info.IChestInfo
import java.nio.ByteBuffer
import java.util.*

open class SmallChestRenderBuilder(
    resourceManager: IResourceManager,
    builtPosX: Double,
    builtPosY: Double,
    builtPosZ: Double,
    size: Int
) : TileEntityRenderBuilder<IChestInfo>(resourceManager, builtPosX, builtPosY, builtPosZ, size, 20) {
    override fun add(info: IChestInfo) {
        val posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        val posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        buffer.putFloat(posX)
        buffer.putFloat(posY)
        buffer.putFloat(posZ)

        putLightMapUV(info.lightMapUV)

        when (info.direction) {
            2 -> {
                buffer.put(2)
            }
            4 -> {
                buffer.put(-1)
            }
            5 -> {
                buffer.put(1)
            }
            else -> {
                buffer.put(0)
            }
        }

        when {
            isChristmas -> {
                buffer.put(2)
            }
            info.isTrap -> {
                buffer.put(1)
            }
            else -> {
                buffer.put(0)
            }
        }

        buffer.putShort((info.prevLidAngle * 65535.0f).toInt().toShort())
        buffer.putShort((info.lidAngle * 65535.0f).toInt().toShort())
    }

    override fun uploadBuffer(buffer: ByteBuffer): TileEntityRenderBuilder.Renderer {
        return upload(buffer, model.get(resourceManager), texture)
    }

    protected fun upload(buffer: ByteBuffer, model: Model, texture: ResourceEntry<ITexture>): TileEntityRenderBuilder.Renderer {
        val shader = shader.get(resourceManager)

        val vaoID = glGenVertexArrays()
        val vboID = glGenBuffers()

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW)

        glBindVertexArray(vaoID)

        glVertexAttribPointer(4, 3, GL_FLOAT, false, 20, 0L) // 12
        glVertexAttribPointer(5, 2, GL_UNSIGNED_BYTE, true, 20, 12L) // 2

        glVertexAttribIPointer(6, 1, GL_BYTE, 20, 14L) // 1
        glVertexAttribIPointer(7, 1, GL_UNSIGNED_BYTE, 20, 15L) // 1
        glVertexAttribPointer(8, 1, GL_UNSIGNED_SHORT, true, 20, 16L) // 2
        glVertexAttribPointer(9, 1, GL_UNSIGNED_SHORT, true, 20, 18L) // 2

        glVertexAttribDivisor(4, 1)
        glVertexAttribDivisor(5, 1)
        glVertexAttribDivisor(6, 1)
        glVertexAttribDivisor(7, 1)
        glVertexAttribDivisor(8, 1)
        glVertexAttribDivisor(9, 1)

        model.attachVBO()

        glEnableVertexAttribArray(4)
        glEnableVertexAttribArray(5)
        glEnableVertexAttribArray(6)
        glEnableVertexAttribArray(7)
        glEnableVertexAttribArray(8)
        glEnableVertexAttribArray(9)

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        return Renderer(renderInfo(shader, vaoID, vboID, model), texture)
    }

    private class Renderer(
        renderInfo: RenderInfo,
        private val texture: ResourceEntry<ITexture>
    ) : TileEntityRenderBuilder.Renderer(renderInfo) {
        override fun preRender() {
            texture.get(resourceManager).bind()
        }

        override fun postRender() {

        }
    }

    protected companion object {
        val model = model("SmallChest")
        val texture = texture("SmallChest")
        val shader = shader("Chest")

        val isChristmas = Calendar.getInstance().let { calendar ->
            calendar[2] + 1 == 12 && calendar[5] >= 24 && calendar[5] <= 26
        }
    }
}