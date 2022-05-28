package me.luna.fastmc.shared.terrain

import me.luna.fastmc.shared.opengl.VertexArrayObject
import me.luna.fastmc.shared.opengl.impl.RenderBufferPool
import me.luna.fastmc.shared.util.allocateInt
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import org.joml.FrustumIntersection

@Suppress("NOTHING_TO_INLINE")
class RenderRegion(
    private val renderer: TerrainRenderer,
    @JvmField val index: Int
) {
    var originX = 0; private set
    inline val originY get() = 0
    var originZ = 0; private set

    @JvmField
    val frustumCull: FrustumCull = FrustumCullImpl()

    @JvmField
    val regionLayerArray = Array(renderer.layerCount) { Layer() }

    @JvmField
    val sortSuppArray = arrayOfNulls<RenderChunk>(renderer.renderRegionChunkCount)

    @Suppress("UNCHECKED_CAST")
    @JvmField
    val visibleRenderChunkList = FastObjectArrayList.wrap(arrayOfNulls<RenderChunk>(renderer.renderRegionChunkCount), 0)

    @JvmField
    val bufferPool = RenderBufferPool(TerrainRenderer.VERTEX_ATTRIBUTE, (4 * 1024 * 1024).countTrailingZeroBits())

    private var vbo = bufferPool.vbo

    @JvmField
    val vao = VertexArrayObject().apply {
        attachVbo(vbo)
    }

    fun updateVao() {
        val newVbo = bufferPool.vbo
        if (vbo !== newVbo) {
            vao.clear()
            vao.attachVbo(newVbo)
            vbo = newVbo
        }
    }

    inline fun getLayer(index: Int): Layer {
        return regionLayerArray[index]
    }

    fun setOrigin(x: Int, z: Int) {
        if (x != originX || z != originZ) {
            originX = x
            originZ = z
            frustumCull.reset()
        }
    }

    fun destroy() {
        vao.destroy()
    }

    class Layer {
        @JvmField
        val firstBuffer = allocateInt(4069).apply {
            limit(0)
        }

        @JvmField
        val countBuffer = allocateInt(4069).apply {
            limit(0)
        }
    }

    private inner class FrustumCullImpl : FrustumCull(renderer) {
        override fun isInFrustum(frustum: FrustumIntersection): Boolean {
            val x = (originX - renderer.renderPosX).toFloat()
            val y = (-renderer.renderPosY).toFloat()
            val z = (originZ - renderer.renderPosZ).toFloat()
            return frustum.testAab(x, y, z, x + 256.0f, y + 256.0f, z + 256.0f)
        }
    }
}