package me.xiaro.fastmc.shared.resource

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.texture.ITexture
import me.xiaro.fastmc.shared.tileentity.TileEntityRenderBuilder

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