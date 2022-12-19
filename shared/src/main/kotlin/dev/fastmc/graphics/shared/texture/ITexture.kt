package dev.fastmc.graphics.shared.texture

import dev.fastmc.graphics.shared.opengl.IGLBinding
import dev.fastmc.graphics.shared.opengl.IGLObject
import dev.fastmc.graphics.shared.opengl.glBindTexture
import dev.fastmc.graphics.shared.resource.Resource

interface ITexture : Resource, IGLObject, IGLBinding {
    override fun bind() {
        glBindTexture(id)
    }

    override fun unbind() {
        glBindTexture(0)
    }
}