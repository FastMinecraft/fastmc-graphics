package dev.fastmc.graphics.shared.instancing.entity

import dev.fastmc.graphics.shared.instancing.entity.info.ICowInfo
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import dev.luna5ama.glwrapper.impl.GLDataType
import dev.luna5ama.glwrapper.impl.VertexAttribute
import dev.luna5ama.kmogus.Ptr

class CowInstancingBuilder : EntityInstancingBuilder<ICowInfo<*>>(68) {
    override fun add(ptr: Ptr, info: ICowInfo<*>) {
        ptr.setFloatInc((info.prevX - builtPosX).toFloat())
            .setFloatInc((info.prevY - builtPosY).toFloat())
            .setFloatInc((info.prevZ - builtPosZ).toFloat())
            .setFloatInc((info.x - builtPosX).toFloat())
            .setFloatInc((info.y - builtPosY).toFloat())
            .setFloatInc((info.z - builtPosZ).toFloat())
            .putLightMapUV(info)
            .setFloatInc(info.prevRenderYawOffset)
            .setFloatInc(info.prevRotationYawHead)
            .setFloatInc(info.prevRotationPitch)
            .setFloatInc(info.renderYawOffset)
            .setFloatInc(info.rotationYawHead)
            .setFloatInc(info.rotationPitch)
            .setFloatInc(info.limbSwing - info.limbSwingAmount)
            .setFloatInc(info.prevLimbSwingAmount)
            .setFloatInc(info.limbSwing)
            .setFloatInc(info.limbSwingAmount)
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