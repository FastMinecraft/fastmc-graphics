package dev.fastmc.graphics.shared.instancing.entity

import dev.fastmc.graphics.shared.instancing.AbstractInstancingBuilder
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture

abstract class LayerInstancingBuilder {
    protected companion object {
        fun model(name: String): ResourceEntry<Model> {
            return ResourceEntry("entity/$name") {
                it.model
            }
        }

        fun shader(name: String): ResourceEntry<AbstractInstancingBuilder.InstancingShaderProgram> {
            return ResourceEntry("entity/$name") {
                it.entityShader
            }
        }

        fun texture(name: String): ResourceEntry<ITexture> {
            return ResourceEntry("entity/$name") {
                it.texture
            }
        }
    }
}