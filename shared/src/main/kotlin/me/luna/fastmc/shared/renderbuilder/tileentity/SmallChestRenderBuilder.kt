package me.luna.fastmc.shared.renderbuilder.tileentity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IChestInfo
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture
import java.nio.ByteBuffer
import java.util.*

open class SmallChestRenderBuilder : TileEntityRenderBuilder<IChestInfo<*>>(20) {
    override fun add(buffer: ByteBuffer, info: IChestInfo<*>) {
        val posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        val posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        buffer.putFloat(posX)
        buffer.putFloat(posY)
        buffer.putFloat(posZ)

        buffer.putLightMapUV(info)
        buffer.putHDirection(info.hDirection)

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

    override fun VertexAttribute.Builder.setupAttribute() {
        float(4, 3, GLDataType.GL_FLOAT, false, 1) // 12
        float(5, 2, GLDataType.GL_UNSIGNED_BYTE, true, 1) // 2

        int(6, 1, GLDataType.GL_BYTE, 1) // 1
        int(7, 1, GLDataType.GL_UNSIGNED_BYTE, 1) // 1
        float(8, 1, GLDataType.GL_UNSIGNED_SHORT, true, 1) // 2
        float(9, 1, GLDataType.GL_UNSIGNED_SHORT, true, 1) // 2
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