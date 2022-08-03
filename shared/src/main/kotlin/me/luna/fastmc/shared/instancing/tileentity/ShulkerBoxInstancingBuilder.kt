package me.luna.fastmc.shared.instancing.tileentity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.GLDataType
import me.luna.fastmc.shared.opengl.impl.VertexAttribute
import me.luna.fastmc.shared.instancing.tileentity.info.IShulkerBoxInfo
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture
import me.luna.fastmc.shared.util.skip
import java.nio.ByteBuffer

class ShulkerBoxInstancingBuilder : TileEntityInstancingBuilder<IShulkerBoxInfo<*>>(20) {
    override fun add(buffer: ByteBuffer, info: IShulkerBoxInfo<*>) {
        val posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        val posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        buffer.putFloat(posX)
        buffer.putFloat(posY)
        buffer.putFloat(posZ)

        buffer.putLightMapUV(info)

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
        buffer.skip(1)
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<InstancingShaderProgram> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun VertexAttribute.Builder.setupAttribute() {
        float(4, 3, GLDataType.GL_FLOAT, false) // 12
        float(5, 2, GLDataType.GL_UNSIGNED_BYTE, true) // 2

        int(6, 1, GLDataType.GL_BYTE) // 1
        int(7, 1, GLDataType.GL_BYTE) // 1
        int(8, 1, GLDataType.GL_UNSIGNED_BYTE) // 1
        float(9, 1, GLDataType.GL_UNSIGNED_BYTE, true) // 1
        float(10, 1, GLDataType.GL_UNSIGNED_BYTE, true) // 1
    }

    private companion object {
        val model = model("ShulkerBox")
        val shader = shader("ShulkerBox")
        val texture = texture("ShulkerBox")
    }
}