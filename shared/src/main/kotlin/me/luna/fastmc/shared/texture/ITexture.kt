package me.luna.fastmc.shared.texture

import me.luna.fastmc.shared.resource.Resource

interface ITexture : Resource {
    fun bind()
}