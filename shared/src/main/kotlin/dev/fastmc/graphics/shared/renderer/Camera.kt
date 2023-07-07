package dev.fastmc.graphics.shared.renderer

import dev.fastmc.common.floorToInt
import dev.fastmc.graphics.shared.util.Frustum
import dev.luna5ama.glwrapper.api.GL_DYNAMIC_STORAGE_BIT
import dev.luna5ama.glwrapper.api.glInvalidateBufferData
import dev.luna5ama.glwrapper.api.glNamedBufferSubData
import dev.luna5ama.glwrapper.impl.BufferObject
import dev.luna5ama.kmogus.MemoryStack
import dev.luna5ama.kmogus.asMutable
import dev.luna5ama.kmogus.copyToMutableArr
import org.joml.FrustumIntersection
import org.joml.Matrix4f

class Camera {
    var screenWidth = 0; private set
    var screenHeight = 0; private set

    var posX = 0.0; private set
    var posY = 0.0; private set
    var posZ = 0.0; private set

    var yaw = 0.0f; private set
    var pitch = 0.0f; private set

    var blockX = 0; private set
    var blockY = 0; private set
    var blockZ = 0; private set

    var projection = Matrix4f(); private set
    var modelView = Matrix4f(); private set
    var combined = Matrix4f(); private set
    var inverseProject = Matrix4f(); private set
    var inverseModelView = Matrix4f(); private set
    var inverseCombined = Matrix4f(); private set

    var frustum = Frustum(inverseCombined); private set

    var frustumIntersection = FrustumIntersection(); private set
    var matrixHash = 0L; private set

    var matrixPosHash = 0L; private set

    val ubo = BufferObject.Immutable().allocate(UBO_SIZE, GL_DYNAMIC_STORAGE_BIT)

    fun update(
        screenWidth: Int,
        screenHeight: Int,
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double,
        cameraYaw: Float,
        cameraPitch: Float,
        projection: Matrix4f,
        modelView: Matrix4f,
        partialTicks: Float
    ) {
        updateScreenSize(screenWidth, screenHeight)
        updateCameraPos(cameraX, cameraY, cameraZ)
        updateCameraRotation(cameraYaw, cameraPitch)
        updateMatrix(projection, modelView)
        updateUBO(partialTicks)
    }

    private fun updateScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    private fun updateCameraRotation(yaw: Float, pitch: Float) {
        this.yaw = yaw
        this.pitch = pitch
    }

    fun updateCameraPos(cameraX: Double, cameraY: Double, cameraZ: Double) {
        this.posX = cameraX
        this.posY = cameraY
        this.posZ = cameraZ

        blockX = cameraX.floorToInt()
        blockY = cameraY.floorToInt()
        blockZ = cameraZ.floorToInt()
    }

    private fun updateMatrix(projection: Matrix4f, modelView: Matrix4f) {
        this.projection = projection
        this.modelView = modelView
        this.combined = projection.mul(modelView, combined)
        inverseProject = projection.invert(Matrix4f())
        inverseModelView = modelView.invert(Matrix4f())
        inverseCombined = combined.invert(Matrix4f())
        
        frustumIntersection = FrustumIntersection(combined, false)

        var hash = 1L
        hash = 31L * hash + combined.m00().toRawBits()
        hash = 31L * hash + combined.m01().toRawBits()
        hash = 31L * hash + combined.m02().toRawBits()
        hash = 31L * hash + combined.m03().toRawBits()
        hash = 31L * hash + combined.m10().toRawBits()
        hash = 31L * hash + combined.m11().toRawBits()
        hash = 31L * hash + combined.m12().toRawBits()
        hash = 31L * hash + combined.m13().toRawBits()
        hash = 31L * hash + combined.m20().toRawBits()
        hash = 31L * hash + combined.m21().toRawBits()
        hash = 31L * hash + combined.m22().toRawBits()
        hash = 31L * hash + combined.m23().toRawBits()
        hash = 31L * hash + combined.m30().toRawBits()
        hash = 31L * hash + combined.m31().toRawBits()
        hash = 31L * hash + combined.m32().toRawBits()
        hash = 31L * hash + combined.m33().toRawBits()
        matrixHash = hash

        hash = 31L * hash + this.posX.toRawBits()
        hash = 31L * hash + this.posY.toRawBits()
        hash = 31L * hash + this.posZ.toRawBits()
        matrixPosHash = hash

        frustum = Frustum(inverseCombined)
    }


    private fun updateUBO(partialTicks: Float) {
        MemoryStack {
            val arr = malloc(UBO_SIZE).asMutable()

            projection.copyToMutableArr(arr)
            modelView.copyToMutableArr(arr)
            combined.copyToMutableArr(arr)

            inverseProject.copyToMutableArr(arr)
            inverseModelView.copyToMutableArr(arr)
            inverseCombined.copyToMutableArr(arr)

            arr.ptr
                .setFloatInc(screenWidth.toFloat())
                .setFloatInc(screenHeight.toFloat())
                .setFloatInc(partialTicks)

            glInvalidateBufferData(ubo.id)
            glNamedBufferSubData(ubo.id, 0, UBO_SIZE, arr.basePtr)
        }
    }

    val chunkX get() = blockX shr 4
    val chunkY get() = blockY shr 4
    val chunkZ get() = blockZ shr 4

    val chunkOriginX get() = chunkX shl 4
    val chunkOriginY get() = chunkY shl 4
    val chunkOriginZ get() = chunkZ shl 4

    companion object {
        val UBO_SIZE = 400L
    }
}