package me.xiaro.fastmc.shared.renderbuilder.entity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.*
import me.xiaro.fastmc.shared.renderbuilder.entity.info.ICowInfo
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture
import java.nio.ByteBuffer

class CowRenderBuilder : EntityRenderBuilder<ICowInfo<*>>(24) {
    override fun add(info: ICowInfo<*>) {
        buffer.putFloat((info.prevX + 2.0 - builtPosX).toFloat())
        buffer.putFloat((info.prevY - builtPosY).toFloat())
        buffer.putFloat((info.prevZ - builtPosZ).toFloat())
        buffer.putFloat((info.x + 2.0 - builtPosX).toFloat())
        buffer.putFloat((info.y - builtPosY).toFloat())
        buffer.putFloat((info.z - builtPosZ).toFloat())
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<Shader> get() = Companion.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    override fun setupAttribute() {
        glEnableVertexAttribArray(4)
        glEnableVertexAttribArray(5)

        glVertexAttribPointer(4, 3, GL_FLOAT, false, 24, 0L) // 12
        glVertexAttribPointer(5, 3, GL_FLOAT, false, 24, 12L) // 2

        glVertexAttribDivisor(4, 1)
        glVertexAttribDivisor(5, 1)
    }

    private companion object {
        val model = model("Cow")
        val texture = texture("Cow")
        val shader = shader("Cow")
    }
}