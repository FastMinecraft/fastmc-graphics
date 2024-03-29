package dev.fastmc.graphics.shared.instancing.tileentity

import dev.fastmc.graphics.shared.instancing.AbstractInstancingBuilder
import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import java.nio.ByteBuffer

abstract class TileEntityInstancingBuilder<T : ITileEntityInfo<*>>(vertexSize: Int) :
    AbstractInstancingBuilder<T>(vertexSize) {
    protected fun ByteBuffer.putPos(info: T) {
        putFloat((info.posX + 0.5 - builtPosX).toFloat())
        putFloat((info.posY - builtPosY).toFloat())
        putFloat((info.posZ + 0.5 - builtPosZ).toFloat())
    }

    protected fun ByteBuffer.putLightMapUV(info: T) {
        val lightMapUV = info.lightMapUV
        put((lightMapUV and 0xFF).toByte())
        put((lightMapUV shr 16 and 0xFF).toByte())
    }

    protected fun ByteBuffer.putHDirection(hDirection: Int) {
        put(hDirection.toByte())
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