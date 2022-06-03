package me.luna.fastmc.shared.renderer

import kotlinx.coroutines.CoroutineScope
import me.luna.fastmc.shared.opengl.impl.UniformBufferObject
import me.luna.fastmc.shared.terrain.TerrainRenderer
import me.luna.fastmc.shared.util.fastFloor
import me.luna.fastmc.shared.util.skip
import org.joml.FrustumIntersection
import org.joml.Matrix4f
import kotlin.coroutines.CoroutineContext

abstract class WorldRenderer : IRenderer {
    lateinit var tileEntityRenderer: AbstractTileEntityRenderer<*>; private set
    lateinit var entityRenderer: AbstractEntityRenderer<*>; private set
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

    final override var projectionMatrix = Matrix4f()
    final override var modelViewMatrix = Matrix4f()
    final override var invertedProjectMatrix = Matrix4f()
    final override var invertedModelViewMatrix = Matrix4f()

    final override val globalUBO = UniformBufferObject("Global", 132)

    final override val frustum = FrustumIntersection(projectionMatrix, false)
    final override var matrixHash = 0L
    final override var matrixPosHash = 0L

    fun init(
        tileEntityRenderer: AbstractTileEntityRenderer<*>,
        entityRenderer: AbstractEntityRenderer<*>,
        terrainRenderer: TerrainRenderer
    ) {
        check(!this::tileEntityRenderer.isInitialized)
        check(!this::entityRenderer.isInitialized)
        check(!this::terrainRenderer.isInitialized)

        this.tileEntityRenderer = tileEntityRenderer
        this.entityRenderer = entityRenderer
        this.terrainRenderer = terrainRenderer
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

    fun updateMatrix(projection: Matrix4f, modelView: Matrix4f, partialTicks: Float) {
        projectionMatrix = projection
        modelViewMatrix = modelView
        invertedProjectMatrix = projection.invert(Matrix4f())
        invertedModelViewMatrix = modelView.invert(Matrix4f())

        globalUBO.update {
            projection.get(0, it)
            modelView.get(64, it)
            it.skip(128)
            it.putFloat(partialTicks)
        }
    }

    fun updateFrustum() {
        val multiplied = projectionMatrix.mul(modelViewMatrix, Matrix4f())
        frustum.set(multiplied, false)

        var hash = 1L
        hash = 31L * hash + multiplied.m00().toRawBits()
        hash = 31L * hash + multiplied.m01().toRawBits()
        hash = 31L * hash + multiplied.m02().toRawBits()
        hash = 31L * hash + multiplied.m03().toRawBits()
        hash = 31L * hash + multiplied.m10().toRawBits()
        hash = 31L * hash + multiplied.m11().toRawBits()
        hash = 31L * hash + multiplied.m12().toRawBits()
        hash = 31L * hash + multiplied.m13().toRawBits()
        hash = 31L * hash + multiplied.m20().toRawBits()
        hash = 31L * hash + multiplied.m21().toRawBits()
        hash = 31L * hash + multiplied.m22().toRawBits()
        hash = 31L * hash + multiplied.m23().toRawBits()
        hash = 31L * hash + multiplied.m30().toRawBits()
        hash = 31L * hash + multiplied.m31().toRawBits()
        hash = 31L * hash + multiplied.m32().toRawBits()
        hash = 31L * hash + multiplied.m33().toRawBits()
        matrixHash = hash

        hash = 31L * hash + cameraX.toRawBits()
        hash = 31L * hash + cameraY.toRawBits()
        hash = 31L * hash + cameraZ.toRawBits()
        matrixPosHash = hash
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