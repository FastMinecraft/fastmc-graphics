package me.luna.fastmc.shared.resource

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.ShaderProgram
import me.luna.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.luna.fastmc.shared.texture.ITexture

interface IResourceManager {
    val model: ResourceProvider<Model>
    val entityShader: ResourceProvider<AbstractRenderBuilder.ShaderProgram>
    val texture: ResourceProvider<ITexture>
    val shaderProgram: ResourceProvider<ShaderProgram>

    fun destroy() {
        model.destroy()
        entityShader.destroy()
        texture.destroy()
        shaderProgram.destroy()
    }
}