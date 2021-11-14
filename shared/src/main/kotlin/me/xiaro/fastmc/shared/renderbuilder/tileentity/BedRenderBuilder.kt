package me.xiaro.fastmc.shared.renderbuilder.tileentity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.*
import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IBedInfo
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture
import java.nio.ByteBuffer

class BedRenderBuilder : TileEntityRenderBuilder<IBedInfo<*>>(20) {
    override fun add(info: IBedInfo<*>) {
        putPos(info)
        putLightMapUV(info)
        putHDirection((info.hDirection - 2 + 2) % 4 + 2)

        buffer.put(info.color.toByte())
        buffer.put(if (info.isHead) 1 else 0)
        buffer.position(buffer.position() + 3)
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<Shader> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun setupAttribute() {
        glEnableVertexAttribArray(4)
        glEnableVertexAttribArray(5)
        glEnableVertexAttribArray(6)
        glEnableVertexAttribArray(7)
        glEnableVertexAttribArray(8)

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
    }

    private companion object {
        val model = model("Bed")
        val shader = shader("Bed")
        val texture = texture("Bed")
    }
}