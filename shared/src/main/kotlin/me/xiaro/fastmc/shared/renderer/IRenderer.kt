package me.xiaro.fastmc.shared.renderer

import me.xiaro.fastmc.shared.resource.IResourceManager
import org.joml.Matrix4f

interface IRenderer {
    val resourceManager: IResourceManager

    val renderPosX: Double
    val renderPosY: Double
    val renderPosZ: Double

    val projectionMatrix: Matrix4f
    val modelViewMatrix: Matrix4f
}