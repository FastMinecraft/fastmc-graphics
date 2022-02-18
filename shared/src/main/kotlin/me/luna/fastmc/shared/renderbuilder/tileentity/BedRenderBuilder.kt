package me.luna.fastmc.shared.renderbuilder.tileentity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IBedInfo
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture
import me.luna.fastmc.shared.util.skip

class BedRenderBuilder : TileEntityRenderBuilder<IBedInfo<*>>(20) {
    override fun add(info: IBedInfo<*>) {
        putPos(info)
        putLightMapUV(info)
        putHDirection((info.hDirection - 2 + 2) % 4 + 2)

        buffer.put(info.color.toByte())
        buffer.put(if (info.isHead) 1 else 0)
        buffer.skip(3)
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<Shader> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun VertexAttribute.Builder.setupAttribute() {
        float(4, 3, GLDataType.GL_FLOAT, false, 1) // 12
        float(5, 2, GLDataType.GL_UNSIGNED_BYTE, true, 1) // 2

        int(6, 1, GLDataType.GL_BYTE, 1) // 1
        int(7, 1, GLDataType.GL_UNSIGNED_BYTE, 1) // 1
        int(8, 1, GLDataType.GL_UNSIGNED_BYTE, 1) // 1
    }

    private companion object {
        val model = model("Bed")
        val shader = shader("Bed")
        val texture = texture("Bed")
    }
}