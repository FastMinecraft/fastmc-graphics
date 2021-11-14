package me.xiaro.fastmc.shared.renderbuilder.tileentity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.*
import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IChestInfo
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture
import java.util.*

open class SmallChestRenderBuilder : TileEntityRenderBuilder<IChestInfo<*>>(20) {
    override fun add(info: IChestInfo<*>) {
        val posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        val posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        buffer.putFloat(posX)
        buffer.putFloat(posY)
        buffer.putFloat(posZ)

        putLightMapUV(info)
        putHDirection(info.hDirection)

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

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<Shader> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun setupAttribute() {
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

        glEnableVertexAttribArray(4)
        glEnableVertexAttribArray(5)
        glEnableVertexAttribArray(6)
        glEnableVertexAttribArray(7)
        glEnableVertexAttribArray(8)
        glEnableVertexAttribArray(9)
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