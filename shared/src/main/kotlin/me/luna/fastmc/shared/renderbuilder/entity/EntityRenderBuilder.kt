package me.luna.fastmc.shared.renderbuilder.entity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.luna.fastmc.shared.renderbuilder.entity.info.IEntityInfo
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture
import java.nio.ByteBuffer

abstract class EntityRenderBuilder<T : IEntityInfo<*>>(vertexSize: Int) : AbstractRenderBuilder<T>(vertexSize) {
    protected fun ByteBuffer.putPos(info: T) {
        putFloat((info.prevX - builtPosX).toFloat())
        putFloat((info.prevY - builtPosY).toFloat())
        putFloat((info.prevZ - builtPosZ).toFloat())
        putFloat((info.x - builtPosX).toFloat())
        putFloat((info.y - builtPosY).toFloat())
        putFloat((info.z - builtPosZ).toFloat())
    }

    protected fun ByteBuffer.putRotations(info: T) {
        putFloat(info.rotationYaw)
        putFloat(info.rotationPitch)
        putFloat(info.prevRotationYaw)
        putFloat(info.prevRotationPitch)
    }

    protected fun ByteBuffer.putLightMapUV(info: T) {
        val lightMapUV = info.lightMapUV
        put((lightMapUV and 0xFF).toByte())
        put((lightMapUV shr 16 and 0xFF).toByte())
    }

    protected companion object {
        fun model(name: String): ResourceEntry<Model> {
            return ResourceEntry("entity/$name") {
                it.model
            }
        }

        fun shader(name: String): ResourceEntry<InstancingShaderProgram> {
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