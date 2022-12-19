package dev.fastmc.graphics.shared.instancing.tileentity

import dev.fastmc.common.skip
import dev.fastmc.graphics.shared.instancing.tileentity.info.IEnderChestInfo
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.opengl.GLDataType
import dev.fastmc.graphics.shared.opengl.impl.VertexAttribute
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import java.nio.ByteBuffer

class EnderChestInstancingBuilder : TileEntityInstancingBuilder<IEnderChestInfo<*>>(20) {
    override fun add(buffer: ByteBuffer, info: IEnderChestInfo<*>) {
        buffer.putPos(info)
        buffer.putLightMapUV(info)
        buffer.putHDirection(info.hDirection)

        buffer.putShort((info.prevLidAngle * 65535.0f).toInt().toShort())
        buffer.putShort((info.lidAngle * 65535.0f).toInt().toShort())
        buffer.skip(1)
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<InstancingShaderProgram> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun VertexAttribute.Builder.setupAttribute() {
        float(4, 3, GLDataType.GL_FLOAT, false) // 12
        float(5, 2, GLDataType.GL_UNSIGNED_BYTE, true) // 2

        int(6, 1, GLDataType.GL_BYTE) // 1
        float(7, 1, GLDataType.GL_UNSIGNED_SHORT, true) // 2
        float(8, 1, GLDataType.GL_UNSIGNED_SHORT, true) // 2
    }

    private companion object {
        val model = model("SmallChest")
        val texture = texture("EnderChest")
        val shader = shader("EnderChest")
    }
}