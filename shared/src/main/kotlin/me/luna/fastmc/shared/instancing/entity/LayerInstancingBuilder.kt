package me.luna.fastmc.shared.instancing.entity

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.instancing.AbstractInstancingBuilder
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture

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