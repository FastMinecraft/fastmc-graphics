package me.xiaro.fastmc.shared.resource

import me.xiaro.fastmc.shared.entity.EntityRenderBuilder
import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.Shader
import me.xiaro.fastmc.shared.texture.ITexture
import me.xiaro.fastmc.shared.tileentity.TileEntityRenderBuilder

interface IResourceManager {
    val model: ResourceProvider<Model>
    val tileEntityShader: ResourceProvider<TileEntityRenderBuilder.Shader>
    val entityShader: ResourceProvider<EntityRenderBuilder.Shader>
    val texture: ResourceProvider<ITexture>
    val shader: ResourceProvider<Shader>

    fun destroy() {
        model.destroy()
        tileEntityShader.destroy()
        entityShader.destroy()
        texture.destroy()
        shader.destroy()
    }
}