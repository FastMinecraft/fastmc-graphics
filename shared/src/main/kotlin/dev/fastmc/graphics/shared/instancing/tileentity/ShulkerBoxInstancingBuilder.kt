package dev.fastmc.graphics.shared.instancing.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.IShulkerBoxInfo
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import dev.luna5ama.glwrapper.impl.GLDataType
import dev.luna5ama.glwrapper.impl.VertexAttribute
import dev.luna5ama.kmogus.Ptr

class ShulkerBoxInstancingBuilder : TileEntityInstancingBuilder<IShulkerBoxInfo<*>>(20) {
    override fun add(ptr: Ptr, info: IShulkerBoxInfo<*>) {
        val posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        val posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        ptr.setFloatInc(posX)
            .setFloatInc(posY)
            .setFloatInc(posZ)

            .putLightMapUV(info)

            .run {
                when (info.direction) {
                    0 -> {
                        setByteInc(0)
                            .setByteInc(2)
                    }
                    1 -> {
                        setByteInc(0)
                            .setByteInc(0)
                    }
                    2 -> {
                        setByteInc(2)
                            .setByteInc(-1)
                    }
                    3 -> {
                        setByteInc(0)
                            .setByteInc(-1)
                    }
                    4 -> {
                        setByteInc(-1)
                            .setByteInc(1)
                    }
                    5 -> {
                        setByteInc(1)
                            .setByteInc(1)
                    }
                    else -> {
                        this
                    }
                }
            }

            .setByteInc(info.color.toByte())
            .setByteInc((info.prevProgress * 255.0f).toInt().toByte())
            .setByteInc((info.progress * 255.0f).toInt().toByte())
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