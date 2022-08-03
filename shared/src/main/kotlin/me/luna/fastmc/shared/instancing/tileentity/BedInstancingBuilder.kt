package me.luna.fastmc.shared.instancing.tileentity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.GLDataType
import me.luna.fastmc.shared.opengl.impl.VertexAttribute
import me.luna.fastmc.shared.instancing.tileentity.info.IBedInfo
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture
import me.luna.fastmc.shared.util.skip
import java.nio.ByteBuffer

class BedInstancingBuilder : TileEntityInstancingBuilder<IBedInfo<*>>(20) {
    override fun add(buffer: ByteBuffer, info: IBedInfo<*>) {
        buffer.putPos(info)
        buffer.putLightMapUV(info)
        buffer.putHDirection((info.hDirection - 2 + 2) % 4 + 2)

        buffer.put(info.color.toByte())
        buffer.put(if (info.isHead) 1 else 0)
        buffer.skip(3)
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<InstancingShaderProgram> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun VertexAttribute.Builder.setupAttribute() {
        float(4, 3, GLDataType.GL_FLOAT, false) // 12
        float(5, 2, GLDataType.GL_UNSIGNED_BYTE, true) // 2

        int(6, 1, GLDataType.GL_BYTE) // 1
        int(7, 1, GLDataType.GL_UNSIGNED_BYTE) // 1
        int(8, 1, GLDataType.GL_UNSIGNED_BYTE) // 1
    }

    private companion object {
        val model = model("Bed")
        val shader = shader("Bed")
        val texture = texture("Bed")
    }
}