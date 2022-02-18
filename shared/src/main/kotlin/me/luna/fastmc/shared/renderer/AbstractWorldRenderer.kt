package me.luna.fastmc.shared.renderer

import org.joml.Matrix4f

abstract class AbstractWorldRenderer : IRenderer {
    lateinit var tileEntityRenderer: AbstractTileEntityRenderer<*>; private set
    lateinit var entityRenderer: AbstractEntityRenderer<*>; private set

    override var renderPosX = 0.0
    override var renderPosY = 0.0
    override var renderPosZ = 0.0

    override var projectionMatrix = Matrix4f()
    override var modelViewMatrix = Matrix4f()

    fun init(tileEntityRenderer: AbstractTileEntityRenderer<*>, entityRenderer: AbstractEntityRenderer<*>) {
        check(!this::tileEntityRenderer.isInitialized)
        check(!this::entityRenderer.isInitialized)

        this.tileEntityRenderer = tileEntityRenderer
        this.entityRenderer = entityRenderer
    }

    fun setupCamera(projection: Matrix4f, modelView: Matrix4f) {
        projectionMatrix = projection
        modelViewMatrix = modelView
    }

    abstract fun onPostTick()

    abstract fun preRender(partialTicks: Float)

    abstract fun postRender()
}