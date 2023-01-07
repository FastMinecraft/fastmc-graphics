package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.opengl.*
import dev.fastmc.graphics.shared.opengl.impl.RenderBufferPool
import org.joml.FrustumIntersection

@Suppress("NOTHING_TO_INLINE")
class RenderRegion(
    private val renderer: TerrainRenderer,
    private val storage: RenderChunkStorage,
    @JvmField val index: Int
) {
    var originX = 0; private set
    val originY get() = storage.minChunkY shl 4
    var originZ = 0; private set

    @JvmField
    val frustumCull: FrustumCull = FrustumCullImpl()

    @JvmField
    val layerBatchArray = Array(renderer.layerCount) { LayerBatch(storage.regionChunkCount) }

    @JvmField
    val visibleRenderChunkList = FastObjectArrayList.wrap(arrayOfNulls<RenderChunk>(storage.regionChunkCount), 0)

    @JvmField
    val tempVisibleBits = IntArray(storage.regionChunkCount)

    @JvmField
    val vertexBufferPool = RenderBufferPool((4 * 1024 * 1024).countTrailingZeroBits())

    @JvmField
    val indexBufferPool = RenderBufferPool((4 * 1024 * 1024).countTrailingZeroBits())

    private var vbo = vertexBufferPool.bufferObject
    private var ibo = indexBufferPool.bufferObject

    @JvmField
    var vao = VertexArrayObject().apply {
        attachVbo(vbo, TerrainRenderer.VERTEX_ATTRIBUTE)
        attachIbo(ibo)
    }

    fun updateVao() {
        val newVbo = vertexBufferPool.bufferObject
        val newIbo = indexBufferPool.bufferObject
        if (vbo !== newVbo || ibo !== newIbo) {
            vbo = newVbo
            ibo = newIbo

            vao.destroyVao()
            vao = VertexArrayObject().apply {
                attachVbo(newVbo, TerrainRenderer.VERTEX_ATTRIBUTE)
                attachIbo(newIbo)
            }
        }
    }

    inline fun getLayer(index: Int): LayerBatch {
        return layerBatchArray[index]
    }

    fun setPos(x: Int, z: Int) {
        if (x != originX || z != originZ) {
            originX = x
            originZ = z
            frustumCull.reset()
        }
    }

    fun destroy() {
        vao.destroyVao()
        vertexBufferPool.destroy()
        indexBufferPool.destroy()
        for (layer in layerBatchArray) {
            layer.destroy()
        }
    }

    class LayerBatch(regionChunkCount: Int) {
        private var serverBuffer: BufferObject? = null
        private val cachedClientBuffer = CachedBuffer(regionChunkCount * 20)
        private var index = 0
        private var isDirty = false

        val bufferID get() = serverBuffer?.id ?: 0
        var count = 0; private set
        val isEmpty get() = count == 0

        fun update() {
            index = 0
            isDirty = true
            count = 0
        }

        fun put(vertexOffset: Int, indexOffset: Int, indexCount: Int, baseInstance: Int) {
            val clientBuffer = cachedClientBuffer.ensureCapacityByte(count * 20)
            val address = clientBuffer.address + index

            UNSAFE.putInt(address, indexCount / 4)
            UNSAFE.putInt(address + 4L, 1)
            UNSAFE.putInt(address + 8L, indexOffset / 4)
            UNSAFE.putInt(address + 12L, vertexOffset / 16)
            UNSAFE.putInt(address + 16L, baseInstance)

            index += 20
            this.count++
        }

        fun checkUpdate() {
            if (isDirty && count != 0) {
                val clientBuffer = cachedClientBuffer.getByte()
                clientBuffer.limit(index)
                var buffer = serverBuffer
                if (buffer == null || buffer.size < clientBuffer.remaining() || buffer.size - clientBuffer.remaining() > 1024 * 1024) {
                    buffer?.destroy()
                    buffer = BufferObject.Immutable().allocate(clientBuffer, GL_DYNAMIC_STORAGE_BIT)
                    serverBuffer = buffer
                } else {
                    glInvalidateBufferData(buffer.id)
                    glNamedBufferSubData(buffer.id, 0L, clientBuffer)
                }
            }
            isDirty = false
        }

        fun destroy() {
            serverBuffer?.destroy()
            cachedClientBuffer.free()
        }
    }

    private inner class FrustumCullImpl : FrustumCull(renderer) {
        override fun isInFrustum(frustum: FrustumIntersection): Boolean {
            val x = (originX - renderer.renderPosX).toFloat()
            val y = (originY - renderer.renderPosY).toFloat()
            val z = (originZ - renderer.renderPosZ).toFloat()
            return frustum.testAab(x, y, z, x + 256.0f, y + (storage.sizeY shl 4), z + 256.0f)
        }
    }
}