package me.xiaro.fastmc.shared.resource

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.opengl.Shader
import me.xiaro.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.xiaro.fastmc.shared.texture.ITexture

interface IResourceManager {
    val model: ResourceProvider<Model>
    val entityShader: ResourceProvider<AbstractRenderBuilder.Shader>
    val texture: ResourceProvider<ITexture>
    val shader: ResourceProvider<Shader>

    fun destroy() {
        model.destroy()
        entityShader.destroy()
        texture.destroy()
        shader.destroy()
    }
}