package me.xiaro.fastmc

import me.xiaro.fastmc.resource.IResourceManager
import org.joml.Matrix4f

abstract class AbstractEntityRenderer(protected val resourceManager: IResourceManager) {
    protected var renderPosX = 0.0
    protected var renderPosY = 0.0
    protected var renderPosZ = 0.0

    protected var projectionMatrix = Matrix4f()
    protected var modelViewMatrix = Matrix4f()

    fun setupCamera(projection: Matrix4f, modelView: Matrix4f) {
        projectionMatrix = projection
        modelViewMatrix = modelView
    }

    abstract fun onPostTick()

    abstract fun preRender()

    abstract fun renderTileEntities()

    abstract fun renderEntities()

    abstract fun postRender()
}