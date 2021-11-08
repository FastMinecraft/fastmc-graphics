package me.xiaro.fastmc.shared.texture

import me.xiaro.fastmc.shared.resource.Resource

interface ITexture : Resource {
    fun bind()
}