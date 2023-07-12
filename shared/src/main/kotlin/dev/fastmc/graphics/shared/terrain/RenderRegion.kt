package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.opengl.impl.RenderBufferPool
import dev.luna5ama.glwrapper.api.GL_DYNAMIC_STORAGE_BIT
import dev.luna5ama.glwrapper.api.glInvalidateBufferData
import dev.luna5ama.glwrapper.api.glNamedBufferSubData
import dev.luna5ama.glwrapper.impl.BufferObject
import dev.luna5ama.glwrapper.impl.VertexArrayObject
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.asMutable
import dev.luna5ama.kmogus.ensureCapacity
import dev.luna5ama.kmogus.usePtr
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

    val originChunkX get() = originX shr 4
    val originChunkY get() = originY shr 4
    val originChunkZ get() = originZ shr 4

    @JvmField
    val frustumCull: FrustumCull = FrustumCullImpl()

    @JvmField
    val layerBatchArray = Array(renderer.layerCount) { LayerBatch(storage.regionChunkCount) }

    @JvmField
    val visibleRenderChunkList = FastObjectArrayList.wrap(arrayOfNulls<RenderChunk>(storage.regionChunkCount), 0)

    @JvmField
    val tempVisibleBits = ByteArray(storage.regionChunkCount)

    @JvmField
    val vertexBufferPool = RenderBufferPool((4 * 1024 * 1024).countTrailingZeroBits())

    @JvmField
    val indexBufferPool = RenderBufferPool((4 * 1024 * 1024).countTrailingZeroBits())

    private var vbo = vertexBufferPool.bufferObject
    private var ebo = indexBufferPool.bufferObject

    @JvmField
    var vao = VertexArrayObject().apply {
        attachVbo(vbo, TerrainRenderer.VERTEX_ATTRIBUTE_MAIN)
        attachVbo(storage.chunkOffsetBuffer, TerrainRenderer.VERTEX_ATTRIBUTE_CHUNK_OFFSET)
        attachEbo(ebo)
    }

    fun updateVao() {
        val newVbo = vertexBufferPool.bufferObject
        val newIbo = indexBufferPool.bufferObject
        if (vbo !== newVbo || ebo !== newIbo) {
            vbo = newVbo
            ebo = newIbo

            vao.destroyVao()
            vao = VertexArrayObject().apply {
                attachVbo(newVbo, TerrainRenderer.VERTEX_ATTRIBUTE_MAIN)
                attachVbo(storage.chunkOffsetBuffer, TerrainRenderer.VERTEX_ATTRIBUTE_CHUNK_OFFSET)
                attachEbo(newIbo)
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
        private val clientBuffer = Arr.malloc(regionChunkCount * 20L).asMutable()
        private var isDirty = false

        val bufferID get() = serverBuffer?.id ?: 0
        var count = 0; private set
        val isEmpty get() = count == 0

        fun update() {
            clientBuffer.reset()
            isDirty = true
            count = 0
        }

        fun put(vertexByteOffset: Int, indexByteOffset: Int, indexLength: Int, baseInstance: Int) {
            clientBuffer.ensureCapacity((count + 1) * 20L, false)

            clientBuffer.usePtr {
                setIntInc(indexLength / 4)
                    .setIntInc(1)
                    .setIntInc(indexByteOffset / 4)
                    .setIntInc(vertexByteOffset / TerrainRenderer.VERTEX_SIZE)
                    .setIntInc(baseInstance)
            }

            this.count++
        }

        fun checkUpdate() {
            if (isDirty && count != 0) {
                clientBuffer.flip()
                val dataSize = clientBuffer.rem
                var buffer = serverBuffer

                if (buffer == null || buffer.size < dataSize || buffer.size - dataSize > 1024 * 1024) {
                    buffer?.destroy()
                    buffer = BufferObject.Immutable().allocate(dataSize, clientBuffer.ptr, GL_DYNAMIC_STORAGE_BIT)
                    serverBuffer = buffer
                } else {
                    glInvalidateBufferData(buffer.id)
                    glNamedBufferSubData(buffer.id, 0L, dataSize, clientBuffer.ptr)
                }
            }
            isDirty = false
        }

        fun destroy() {
            serverBuffer?.destroy()
            clientBuffer.free()
        }
    }

    private inner class FrustumCullImpl : FrustumCull(renderer) {
        override fun isInFrustum(frustum: FrustumIntersection): Boolean {
            val x = (originX - renderer.camera.posX).toFloat()
            val y = (originY - renderer.camera.posY).toFloat()
            val z = (originZ - renderer.camera.posZ).toFloat()
            return frustum.testAab(x, y, z, x + 256.0f, y + (storage.sizeY shl 4), z + 256.0f)
        }
    }
}