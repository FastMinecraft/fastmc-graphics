package dev.fastmc.graphics.shared.renderer

import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.luna5ama.glwrapper.impl.BufferObject

interface IRenderer {
    val resourceManager: IResourceManager

    val camera: Camera
}