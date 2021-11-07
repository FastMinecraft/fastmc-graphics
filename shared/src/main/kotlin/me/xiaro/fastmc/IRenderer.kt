package me.xiaro.fastmc

import me.xiaro.fastmc.resource.IResourceManager
import org.joml.Matrix4f

interface IRenderer {
    val resourceManager: IResourceManager

    var renderPosX: Double
    var renderPosY: Double
    var renderPosZ: Double

    var projectionMatrix: Matrix4f
    var modelViewMatrix: Matrix4f
}