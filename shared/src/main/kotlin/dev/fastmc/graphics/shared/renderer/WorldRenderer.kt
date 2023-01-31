package dev.fastmc.graphics.shared.renderer

import dev.fastmc.common.MemoryStack
import dev.fastmc.common.fastFloor
import dev.fastmc.common.skip
import dev.fastmc.graphics.shared.opengl.BufferObject
import dev.fastmc.graphics.shared.opengl.GL_DYNAMIC_STORAGE_BIT
import dev.fastmc.graphics.shared.opengl.glNamedBufferSubData
import dev.fastmc.graphics.shared.terrain.TerrainRenderer
import kotlinx.coroutines.CoroutineScope
import org.joml.FrustumIntersection
import org.joml.Matrix4f
import kotlin.coroutines.CoroutineContext
import kotlin.math.round

abstract class WorldRenderer : IRenderer {
    lateinit var tileEntityRenderer: TileEntityRenderer<*>; private set
    lateinit var entityRenderer: EntityRenderer<*>; private set
    lateinit var terrainRenderer: TerrainRenderer; private set

    final override var renderPosX = 0.0; private set
    final override var renderPosY = 0.0; private set
    final override var renderPosZ = 0.0; private set

    final override var cameraYaw = 0.0f; private set
    final override var cameraPitch = 0.0f; private set

    final override var cameraX = 0.0; private set
    final override var cameraY = 0.0; private set
    final override var cameraZ = 0.0; private set

    final override var cameraBlockX = 0; private set
    final override var cameraBlockY = 0; private set
    final override var cameraBlockZ = 0; private set

    final override var screenWidth = 854; private set
    final override var screenHeight = 480; private set

    final override var projectionMatrix = Matrix4f()
    final override var modelViewMatrix = Matrix4f()
    final override var inverseProjectMatrix = Matrix4f()
    final override var inverseModelViewMatrix = Matrix4f()

    final override val globalUBO = BufferObject.Immutable().allocate(268, GL_DYNAMIC_STORAGE_BIT)

    final override var frustum = FrustumIntersection()
    final override var matrixPosHash = 0L

    fun init(
        tileEntityRenderer: TileEntityRenderer<*>,
        entityRenderer: EntityRenderer<*>,
        terrainRenderer: TerrainRenderer
    ) {
        check(!this::tileEntityRenderer.isInitialized)
        check(!this::entityRenderer.isInitialized)
        check(!this::terrainRenderer.isInitialized)

        this.tileEntityRenderer = tileEntityRenderer
        this.entityRenderer = entityRenderer
        this.terrainRenderer = terrainRenderer
    }

    fun updateScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    fun updateCameraRotation(yaw: Float, pitch: Float) {
        cameraYaw = yaw
        cameraPitch = pitch
    }

    fun updateCameraPos(cameraX: Double, cameraY: Double, cameraZ: Double) {
        this.cameraX = cameraX
        this.cameraY = cameraY
        this.cameraZ = cameraZ

        cameraBlockX = cameraX.fastFloor()
        cameraBlockY = cameraY.fastFloor()
        cameraBlockZ = cameraZ.fastFloor()
    }

    fun updateRenderPos(renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
        this.renderPosX = renderPosX
        this.renderPosY = renderPosY
        this.renderPosZ = renderPosZ
    }

    fun updateMatrix(projection: Matrix4f, modelView: Matrix4f) {
        projectionMatrix = projection
        modelViewMatrix = modelView
        inverseProjectMatrix = projection.invert(Matrix4f())
        inverseModelViewMatrix = modelView.invert(Matrix4f())
    }

    fun updateGlobalUBO(partialTicks: Float) {
        MemoryStack.use {
            withMalloc(globalUBO.size) {
                projectionMatrix.get(0, it)
                modelViewMatrix.get(64, it)
                inverseProjectMatrix.get(128, it)
                inverseModelViewMatrix.get(192, it)
                it.skip(256)
                it.putFloat(screenWidth.toFloat())
                it.putFloat(screenHeight.toFloat())
                it.putFloat(partialTicks)
                it.flip()
                globalUBO.invalidate()
                glNamedBufferSubData(globalUBO.id, 0, it)
            }
        }
    }

    private val multiplied = Matrix4f()

    fun updateFrustum() {
        val multiplied = projectionMatrix.mul(modelViewMatrix, multiplied)
        frustum = FrustumIntersection(multiplied, false)

        var hash = roundEps(multiplied.m00()).toRawBits().toLong()
        hash = 31L * hash + roundEps(multiplied.m01()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m02()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m03()).toRawBits()

        hash = 31L * hash + roundEps(multiplied.m10()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m11()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m12()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m13()).toRawBits()

        hash = 31L * hash + roundEps(multiplied.m20()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m21()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m22()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m23()).toRawBits()

        hash = 31L * hash + roundEps(multiplied.m30()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m31()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m32()).toRawBits()
        hash = 31L * hash + roundEps(multiplied.m33()).toRawBits()

        hash = 31L * hash + roundEps(cameraX).toRawBits()
        hash = 31L * hash + roundEps(cameraY).toRawBits()
        hash = 31L * hash + roundEps(cameraZ).toRawBits()

        matrixPosHash = hash
    }

    private fun roundEps(x: Float): Float {
        return round(x * 1_000.0f) / 1_000.0f
    }

    private fun roundEps(x: Double): Double {
        return round(x * 1_000.0) / 1_000.0
    }

    abstract fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope)

    abstract fun preRender(partialTicks: Float)

    abstract fun postRender()

    open fun destroy() {
        globalUBO.destroy()
        tileEntityRenderer.destroy()
        entityRenderer.destroy()
        terrainRenderer.destroy()
    }
}