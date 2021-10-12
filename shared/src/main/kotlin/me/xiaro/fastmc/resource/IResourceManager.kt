package me.xiaro.fastmc.resource

import me.xiaro.fastmc.model.Model
import me.xiaro.fastmc.opengl.ITexture
import me.xiaro.fastmc.tileentity.TileEntityRenderBuilder

interface IResourceManager {
    val model: ResourceProvider<Model>
    val tileEntityShader: ResourceProvider<TileEntityRenderBuilder.Shader>
    val texture: ResourceProvider<ITexture>

    fun destroy() {
        model.destroy()
        tileEntityShader.destroy()
        texture.destroy()
    }
}