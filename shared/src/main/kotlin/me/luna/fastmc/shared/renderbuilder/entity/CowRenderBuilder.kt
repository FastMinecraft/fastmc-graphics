package me.luna.fastmc.shared.renderbuilder.entity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.renderbuilder.entity.info.ICowInfo
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture
import me.luna.fastmc.shared.util.skip
import java.nio.ByteBuffer

class CowRenderBuilder : EntityRenderBuilder<ICowInfo<*>>(68) {
    override fun add(buffer: ByteBuffer, info: ICowInfo<*>) {
        buffer.putFloat((info.prevX - builtPosX).toFloat())
        buffer.putFloat((info.prevY - builtPosY).toFloat())
        buffer.putFloat((info.prevZ - builtPosZ).toFloat())
        buffer.putFloat((info.x - builtPosX).toFloat())
        buffer.putFloat((info.y - builtPosY).toFloat())
        buffer.putFloat((info.z - builtPosZ).toFloat())

        buffer.putLightMapUV(info)

        buffer.putFloat(info.prevRenderYawOffset)
        buffer.putFloat(info.prevRotationYawHead)
        buffer.putFloat(info.prevRotationPitch)

        buffer.putFloat(info.renderYawOffset)
        buffer.putFloat(info.rotationYawHead)
        buffer.putFloat(info.rotationPitch)

        buffer.putFloat(info.limbSwing - info.limbSwingAmount)
        buffer.putFloat(info.prevLimbSwingAmount)

        buffer.putFloat(info.limbSwing)
        buffer.putFloat(info.limbSwingAmount)

        buffer.skip(2)
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<Shader> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun VertexAttribute.Builder.setupAttribute() {
        float(4, 3, GLDataType.GL_FLOAT, false, 1)
        float(5, 3, GLDataType.GL_FLOAT, false, 1)
        float(6, 2, GLDataType.GL_UNSIGNED_BYTE, true, 1)

        float(7, 3, GLDataType.GL_FLOAT, false, 1)
        float(8, 3, GLDataType.GL_FLOAT, false, 1)

        float(9, 2, GLDataType.GL_FLOAT, false, 1)
        float(10, 2, GLDataType.GL_FLOAT, false, 1)
    }

    private companion object {
        val model = model("Cow")
        val texture = texture("Cow")
        val shader = shader("Cow")
    }
}