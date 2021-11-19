package me.xiaro.fastmc.shared.renderbuilder.tileentity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.*
import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IEnderChestInfo
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture
import me.xiaro.fastmc.shared.util.skip

class EnderChestRenderBuilder : TileEntityRenderBuilder<IEnderChestInfo<*>>(20) {
    override fun add(info: IEnderChestInfo<*>) {
        putPos(info)
        putLightMapUV(info)
        putHDirection(info.hDirection)

        buffer.putShort((info.prevLidAngle * 65535.0f).toInt().toShort())
        buffer.putShort((info.lidAngle * 65535.0f).toInt().toShort())
        buffer.skip(1)
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<Shader> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun VertexAttribute.Builder.setupAttribute() {
        float(4, 3, GLDataType.GL_FLOAT, false, 1) // 12
        float(5, 2, GLDataType.GL_UNSIGNED_BYTE, true, 1) // 2

        int(6, 1, GLDataType.GL_BYTE) // 1
        float(7, 1, GLDataType.GL_UNSIGNED_SHORT, true, 1) // 2
        float(8, 1, GLDataType.GL_UNSIGNED_SHORT, true, 1) // 2
    }

    private companion object {
        val model = model("SmallChest")
        val texture = texture("EnderChest")
        val shader = shader("EnderChest")
    }
}