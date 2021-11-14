package me.xiaro.fastmc.shared.renderbuilder.tileentity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.*
import me.xiaro.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture

abstract class TileEntityRenderBuilder<T : ITileEntityInfo<*>>(vertexSize: Int) : AbstractRenderBuilder<T>(vertexSize) {
    protected fun putPos(info: T) {
        buffer.putFloat((info.posX + 0.5 - builtPosX).toFloat())
        buffer.putFloat((info.posY - builtPosY).toFloat())
        buffer.putFloat((info.posZ + 0.5 - builtPosZ).toFloat())
    }

    protected fun putLightMapUV(info: T) {
        val lightMapUV = info.lightMapUV
        buffer.put((lightMapUV and 0xFF).toByte())
        buffer.put((lightMapUV shr 16 and 0xFF).toByte())
    }

    protected fun putHDirection(hDirection: Int) {
        buffer.put(hDirection.toByte())
    }

    protected companion object {
        fun model(name: String): ResourceEntry<Model> {
            return ResourceEntry("tileEntity/$name") {
                it.model
            }
        }

        fun shader(name: String): ResourceEntry<Shader> {
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