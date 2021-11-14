package me.xiaro.fastmc.shared.renderbuilder.tileentity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.*
import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IEnderChestInfo
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture
import java.nio.ByteBuffer

class EnderChestRenderBuilder : TileEntityRenderBuilder<IEnderChestInfo<*>>(20) {
    override fun add(info: IEnderChestInfo<*>) {
        putPos(info)
        putLightMapUV(info)
        putHDirection(info.hDirection)

        buffer.putShort((info.prevLidAngle * 65535.0f).toInt().toShort())
        buffer.putShort((info.lidAngle * 65535.0f).toInt().toShort())
        buffer.position(buffer.position() + 1)
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
        glVertexAttribPointer(7, 1, GL_UNSIGNED_SHORT, true, 20, 15L) // 2
        glVertexAttribPointer(8, 1, GL_UNSIGNED_SHORT, true, 20, 17L) // 2

        glVertexAttribDivisor(4, 1)
        glVertexAttribDivisor(5, 1)
        glVertexAttribDivisor(6, 1)
        glVertexAttribDivisor(7, 1)
        glVertexAttribDivisor(8, 1)
    }

    private companion object {
        val model = model("SmallChest")
        val texture = texture("EnderChest")
        val shader = shader("EnderChest")
    }
}