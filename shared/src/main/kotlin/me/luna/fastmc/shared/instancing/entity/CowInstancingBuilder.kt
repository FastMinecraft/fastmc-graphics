package me.luna.fastmc.shared.instancing.entity

import dev.fastmc.common.skip
import me.luna.fastmc.shared.instancing.entity.info.ICowInfo
import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.GLDataType
import me.luna.fastmc.shared.opengl.impl.VertexAttribute
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture
import java.nio.ByteBuffer

class CowInstancingBuilder : EntityInstancingBuilder<ICowInfo<*>>(68) {
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
    override val shader: ResourceEntry<InstancingShaderProgram> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun VertexAttribute.Builder.setupAttribute() {
        float(4, 3, GLDataType.GL_FLOAT, false)
        float(5, 3, GLDataType.GL_FLOAT, false)
        float(6, 2, GLDataType.GL_UNSIGNED_BYTE, true)

        float(7, 3, GLDataType.GL_FLOAT, false)
        float(8, 3, GLDataType.GL_FLOAT, false)

        float(9, 2, GLDataType.GL_FLOAT, false)
        float(10, 2, GLDataType.GL_FLOAT, false)
    }

    private companion object {
        val model = model("Cow")
        val texture = texture("Cow")
        val shader = shader("Cow")
    }
}