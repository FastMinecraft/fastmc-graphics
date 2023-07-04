package dev.fastmc.graphics.shared.renderer

import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.luna5ama.glwrapper.impl.BufferObject
import org.joml.FrustumIntersection
import org.joml.Matrix4f

interface IRenderer {
    val resourceManager: IResourceManager

    val renderPosX: Double
    val renderPosY: Double
    val renderPosZ: Double

    val cameraYaw: Float
    val cameraPitch: Float

    val cameraX: Double
    val cameraY: Double
    val cameraZ: Double

    val cameraBlockX: Int
    val cameraBlockY: Int
    val cameraBlockZ: Int

    val screenWidth: Int
    val screenHeight: Int

    val projectionMatrix: Matrix4f
    val modelViewMatrix: Matrix4f
    val inverseProjectMatrix: Matrix4f
    val inverseModelViewMatrix: Matrix4f

    val globalUBO: BufferObject

    val frustum: FrustumIntersection
    val matrixHash: Long
    val matrixPosHash: Long
}

inline val IRenderer.cameraChunkX get() = cameraBlockX shr 4
inline val IRenderer.cameraChunkY get() = cameraBlockY shr 4
inline val IRenderer.cameraChunkZ get() = cameraBlockZ shr 4

inline val IRenderer.cameraChunkOriginX get() = cameraChunkX shl 4
inline val IRenderer.cameraChunkOriginY get() = cameraChunkY shl 4
inline val IRenderer.cameraChunkOriginZ get() = cameraChunkZ shl 4