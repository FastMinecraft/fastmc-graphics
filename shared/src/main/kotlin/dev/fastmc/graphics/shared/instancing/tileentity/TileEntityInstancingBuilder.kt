package dev.fastmc.graphics.shared.instancing.tileentity

import dev.fastmc.graphics.shared.instancing.AbstractInstancingBuilder
import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import dev.luna5ama.kmogus.Ptr

abstract class TileEntityInstancingBuilder<T : ITileEntityInfo<*>>(vertexSize: Int) :
    AbstractInstancingBuilder<T>(vertexSize) {
    protected fun Ptr.putPos(info: T): Ptr {
        return setFloatInc((info.posX + 0.5 - builtPosX).toFloat())
            .setFloatInc((info.posY - builtPosY).toFloat())
            .setFloatInc((info.posZ + 0.5 - builtPosZ).toFloat())
    }

    protected fun Ptr.putLightMapUV(info: T): Ptr {
        val lightMapUV = info.lightMapUV
        return setByteInc((lightMapUV and 0xFF).toByte())
            .setByteInc((lightMapUV shr 16 and 0xFF).toByte())
    }

    protected fun Ptr.putHDirection(hDirection: Int): Ptr {
        return setByteInc(hDirection.toByte())
    }

    protected companion object {
        fun model(name: String): ResourceEntry<Model> {
            return ResourceEntry("tileEntity/$name") {
                it.model
            }
        }

        fun shader(name: String): ResourceEntry<InstancingShaderProgram> {
            return ResourceEntry("tileEntity/$name") {
                it.entityShader
            }
        }

        fun texture(name: String): ResourceEntry<ITexture> {
            return ResourceEntry("tileEntity/$name") {
                it.texture
            }
        }
    }
}