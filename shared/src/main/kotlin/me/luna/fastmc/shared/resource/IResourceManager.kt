package me.luna.fastmc.shared.resource

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.instancing.AbstractInstancingBuilder
import me.luna.fastmc.shared.texture.ITexture

interface IResourceManager {
    val model: ResourceProvider<Model>
    val entityShader: ResourceProvider<AbstractInstancingBuilder.InstancingShaderProgram>
    val texture: ResourceProvider<ITexture>

    fun destroy() {
        model.destroy()
        entityShader.destroy()
        texture.destroy()
    }
}