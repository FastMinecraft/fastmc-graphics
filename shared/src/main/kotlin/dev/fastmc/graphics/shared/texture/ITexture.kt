package dev.fastmc.graphics.shared.texture

import dev.fastmc.graphics.shared.resource.Resource
import dev.luna5ama.glwrapper.api.glBindTextureUnit
import dev.luna5ama.glwrapper.impl.IGLObject
import dev.luna5ama.glwrapper.impl.IGLTargetBinding

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
interface ITexture : Resource, IGLObject, IGLTargetBinding {
    override fun bind(unit: Int) {
        glBindTextureUnit(unit, id)
    }

    override fun unbind(unit: Int) {
        glBindTextureUnit(unit, 0)
    }
}