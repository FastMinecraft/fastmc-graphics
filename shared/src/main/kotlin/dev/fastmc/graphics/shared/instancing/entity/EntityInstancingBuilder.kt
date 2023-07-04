package dev.fastmc.graphics.shared.instancing.entity

import dev.fastmc.graphics.shared.instancing.AbstractInstancingBuilder
import dev.fastmc.graphics.shared.instancing.entity.info.IEntityInfo
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import dev.luna5ama.kmogus.Ptr

abstract class EntityInstancingBuilder<T : IEntityInfo<*>>(vertexSize: Int) : AbstractInstancingBuilder<T>(vertexSize) {
    protected fun Ptr.putPos(info: T): Ptr {
        return setFloatInc((info.prevX - builtPosX).toFloat())
            .setFloatInc((info.prevY - builtPosY).toFloat())
            .setFloatInc((info.prevZ - builtPosZ).toFloat())
            .setFloatInc((info.x - builtPosX).toFloat())
            .setFloatInc((info.y - builtPosY).toFloat())
            .setFloatInc((info.z - builtPosZ).toFloat())
    }

    protected fun Ptr.putRotations(info: T): Ptr {
        return setFloatInc(info.rotationYaw)
            .setFloatInc(info.rotationPitch)
            .setFloatInc(info.prevRotationYaw)
            .setFloatInc(info.prevRotationPitch)
    }

    protected fun Ptr.putLightMapUV(info: T): Ptr {
        val lightMapUV = info.lightMapUV
        return setByteInc((lightMapUV and 0xFF).toByte())
            .setByteInc((lightMapUV shr 16 and 0xFF).toByte())
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