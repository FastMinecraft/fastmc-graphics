package dev.fastmc.graphics.shared.instancing.tileentity

import dev.fastmc.graphics.shared.instancing.tileentity.info.IBedInfo
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import dev.luna5ama.glwrapper.impl.GLDataType
import dev.luna5ama.glwrapper.impl.VertexAttribute
import dev.luna5ama.kmogus.Ptr

class BedInstancingBuilder : TileEntityInstancingBuilder<IBedInfo<*>>(20) {
    override fun add(ptr: Ptr, info: IBedInfo<*>) {
        ptr.putPos(info)
            .putLightMapUV(info)
            .putHDirection((info.hDirection - 2 + 2) % 4 + 2)
            .setByteInc(info.color.toByte())
            .setByteInc(if (info.isHead) 1 else 0)
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