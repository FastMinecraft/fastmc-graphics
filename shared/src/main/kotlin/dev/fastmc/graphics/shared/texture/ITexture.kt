package dev.fastmc.graphics.shared.texture

import dev.fastmc.graphics.shared.opengl.IGLObject
import dev.fastmc.graphics.shared.opengl.IGLTargetBinding
import dev.fastmc.graphics.shared.opengl.glBindTextureUnit
import dev.fastmc.graphics.shared.resource.Resource

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
interface ITexture : Resource, IGLObject, IGLTargetBinding {
    override fun bind(unit: Int) {
        glBindTextureUnit(unit, id)
    }

    override fun unbind(unit: Int) {
        glBindTextureUnit(unit, 0)
    }
}