package me.luna.fastmc.shared.renderer

import me.luna.fastmc.shared.opengl.BufferObject
import me.luna.fastmc.shared.resource.IResourceManager
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

    val projectionMatrix: Matrix4f
    val modelViewMatrix: Matrix4f
    val invertedProjectMatrix: Matrix4f
    val invertedModelViewMatrix: Matrix4f

    val matricesUBO: BufferObject

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