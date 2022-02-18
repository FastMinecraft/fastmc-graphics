package me.luna.fastmc.shared.resource

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.Shader
import me.luna.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.luna.fastmc.shared.texture.ITexture

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