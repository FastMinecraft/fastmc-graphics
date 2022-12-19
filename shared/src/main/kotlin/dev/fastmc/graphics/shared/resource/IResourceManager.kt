package dev.fastmc.graphics.shared.resource

import dev.fastmc.graphics.shared.instancing.AbstractInstancingBuilder
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.texture.ITexture

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