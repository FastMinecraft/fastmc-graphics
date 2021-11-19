package me.xiaro.fastmc.shared.renderbuilder.entity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.*
import me.xiaro.fastmc.shared.renderbuilder.entity.info.ICowInfo
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture
import me.xiaro.fastmc.shared.util.skip

class CowRenderBuilder : EntityRenderBuilder<ICowInfo<*>>(68) {
    override fun add(info: ICowInfo<*>) {
        buffer.putFloat((info.prevX + 2.0 - builtPosX).toFloat())
        buffer.putFloat((info.prevY - builtPosY).toFloat())
        buffer.putFloat((info.prevZ - builtPosZ).toFloat())
        buffer.putFloat((info.x + 2.0 - builtPosX).toFloat())
        buffer.putFloat((info.y - builtPosY).toFloat())
        buffer.putFloat((info.z - builtPosZ).toFloat())

        putLightMapUV(info)

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
        float(4, 3, GLDataType.GL_FLOAT, false, 1)
        float(4, 2, GLDataType.GL_UNSIGNED_BYTE, true, 1)

        float(4, 3, GLDataType.GL_FLOAT, false, 1)
        float(4, 3, GLDataType.GL_FLOAT, false, 1)

        float(4, 3, GLDataType.GL_FLOAT, false, 1)
        float(4, 3, GLDataType.GL_FLOAT, false, 1)
    }

    private companion object {
        val model = model("Cow")
        val texture = texture("Cow")
        val shader = shader("Cow")
    }
}