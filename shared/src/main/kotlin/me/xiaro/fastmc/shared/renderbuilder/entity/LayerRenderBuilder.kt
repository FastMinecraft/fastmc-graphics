package me.xiaro.fastmc.shared.renderbuilder.entity

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.xiaro.fastmc.shared.resource.ResourceEntry
import me.xiaro.fastmc.shared.texture.ITexture

abstract class LayerRenderBuilder {
    protected companion object {
        fun model(name: String): ResourceEntry<Model> {
            return ResourceEntry("entity/$name") {
                it.model
            }
        }

        fun shader(name: String): ResourceEntry<AbstractRenderBuilder.Shader> {
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