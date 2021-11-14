package me.xiaro.fastmc.shared.renderbuilder.entity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.xiaro.fastmc.shared.renderbuilder.entity.info.IEntityInfo
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture

abstract class EntityRenderBuilder<T : IEntityInfo<*>>(vertexSize: Int) : AbstractRenderBuilder<T>(vertexSize) {
    protected fun putPos(info: T) {
        buffer.putFloat((info.prevX - builtPosX).toFloat())
        buffer.putFloat((info.prevY - builtPosY).toFloat())
        buffer.putFloat((info.prevZ - builtPosZ).toFloat())
        buffer.putFloat((info.x - builtPosX).toFloat())
        buffer.putFloat((info.y - builtPosY).toFloat())
        buffer.putFloat((info.z - builtPosZ).toFloat())
    }

    protected fun putRotations(info: T) {
        buffer.putFloat(info.rotationYaw)
        buffer.putFloat(info.rotationPitch)
        buffer.putFloat(info.prevRotationYaw)
        buffer.putFloat(info.prevRotationPitch)
    }

    protected fun putLightMapUV(info: T) {
        val lightMapUV = info.lightMapUV
        buffer.put((lightMapUV and 0xFF).toByte())
        buffer.put((lightMapUV shr 16 and 0xFF).toByte())
    }

    protected companion object {
        fun model(name: String): ResourceEntry<Model> {
            return ResourceEntry("entity/$name") {
                it.model
            }
        }

        fun shader(name: String): ResourceEntry<Shader> {
            return ResourceEntry("entity/$name") {
                it.entityShader
            }
        }

        fun texture(name: String): ResourceEntry<ITexture> {
            return ResourceEntry("entity/$name") {
                it.texture
            }
        }
    }
}