package me.xiaro.fastmc.shared.opengl

import me.xiaro.fastmc.shared.resource.Resource

interface ITexture : Resource {
    fun bind()
}