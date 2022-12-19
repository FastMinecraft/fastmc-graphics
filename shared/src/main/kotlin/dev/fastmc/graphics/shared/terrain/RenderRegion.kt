package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.opengl.*
import dev.fastmc.graphics.shared.opengl.impl.RenderBufferPool
import dev.fastmc.graphics.shared.opengl.impl.buildAttribute
import org.joml.FrustumIntersection
import kotlin.math.min

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
    val layerBatchArray = Array(renderer.layerCount) { LayerBatch() }

    @JvmField
    val visibleRenderChunkList = FastObjectArrayList.wrap(arrayOfNulls<RenderChunk>(storage.regionChunkCount), 0)

    @JvmField
    val tempVisibleBits = ByteArray(storage.regionChunkCount)

    @JvmField
    val vertexBufferPool = RenderBufferPool((4 * 1024 * 1024).countTrailingZeroBits())

    @JvmField
    val indexBufferPool = RenderBufferPool((4 * 1024 * 1024).countTrailingZeroBits())

    @JvmField
    val boundingBoxBuffer = BoundingBoxBuffer(storage)

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
        boundingBoxBuffer.destroy()
        for (layer in layerBatchArray) {
            layer.destroy()
        }
    }

    class BoundingBoxBuffer(storage: RenderChunkStorage) {
        private val vao = VertexArrayObject()
        private val serverBuffer = BufferObject.Immutable().allocate(
            storage.regionChunkCount * 8,
            GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT
        )
        private val mappedBuffer = glMapNamedBufferRange(
            serverBuffer.id,
            0,
            serverBuffer.size.toLong(),
            GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_UNSYNCHRONIZED_BIT
        )!!

        init {
            vao.attachVbo(BOX_BUFFER, BOX_VERTEX_ARRTIBUTE)
            vao.attachVbo(serverBuffer, VERTEX_ATTRIBUTE)
        }

        private var index = 0
        var count = 0; private set
        val isEmpty get() = count == 0

        fun update() {
            index = 0
            count = 0
        }

        fun put(renderChunk: RenderChunk) {
            val address = mappedBuffer.address + index

            UNSAFE.putShort(address, renderChunk.regionIndex.toShort())
            UNSAFE.putByte(address + 2, ((renderChunk.minX + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte())
            UNSAFE.putByte(address + 3, ((renderChunk.minY + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte())
            UNSAFE.putByte(address + 4, ((renderChunk.minZ + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte())
            UNSAFE.putByte(address + 5, ((renderChunk.maxX - renderChunk.minX + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte())
            UNSAFE.putByte(address + 6, ((renderChunk.maxY - renderChunk.minY + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte())
            UNSAFE.putByte(address + 7, ((renderChunk.maxZ - renderChunk.minZ + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte())

            index += 8
            count++
        }

        fun destroy() {
            vao.destroyVao()
            serverBuffer.destroy()
        }

        companion object {
            @JvmField
            val BOX_BUFFER = run {
                MemoryStack.use {
                    withMalloc(144) {
                        // Down
                        it.put(0).put(0).put(0).skip(1)
                        it.put(1).put(0).put(1).skip(1)
                        it.put(0).put(0).put(1).skip(1)
                        it.put(0).put(0).put(0).skip(1)
                        it.put(1).put(0).put(0).skip(1)
                        it.put(1).put(0).put(1).skip(1)

                        // Up
                        it.put(0).put(1).put(1).skip(1)
                        it.put(1).put(1).put(0).skip(1)
                        it.put(0).put(1).put(0).skip(1)
                        it.put(0).put(1).put(1).skip(1)
                        it.put(1).put(1).put(1).skip(1)
                        it.put(1).put(1).put(0).skip(1)

                        // West
                        it.put(0).put(1).put(1).skip(1)
                        it.put(0).put(0).put(0).skip(1)
                        it.put(0).put(0).put(1).skip(1)
                        it.put(0).put(1).put(1).skip(1)
                        it.put(0).put(1).put(0).skip(1)
                        it.put(0).put(0).put(0).skip(1)

                        // East
                        it.put(1).put(1).put(0).skip(1)
                        it.put(1).put(0).put(1).skip(1)
                        it.put(1).put(0).put(0).skip(1)
                        it.put(1).put(1).put(0).skip(1)
                        it.put(1).put(1).put(1).skip(1)
                        it.put(1).put(0).put(1).skip(1)

                        // North
                        it.put(0).put(1).put(0).skip(1)
                        it.put(1).put(0).put(0).skip(1)
                        it.put(0).put(0).put(0).skip(1)
                        it.put(0).put(1).put(0).skip(1)
                        it.put(1).put(1).put(0).skip(1)
                        it.put(1).put(0).put(0).skip(1)

                        // South
                        it.put(1).put(1).put(1).skip(1)
                        it.put(0).put(0).put(1).skip(1)
                        it.put(1).put(0).put(1).skip(1)
                        it.put(1).put(1).put(1).skip(1)
                        it.put(0).put(1).put(1).skip(1)
                        it.put(0).put(0).put(1).skip(1)

                        BufferObject.Immutable().allocate(it, 0)
                    }
                }
            }

            @JvmField
            val BOX_VERTEX_ARRTIBUTE = buildAttribute(4, 1) {
                float(0, 3, GLDataType.GL_BYTE, false)
                padding(1)
            }

            @JvmField
            val VERTEX_ATTRIBUTE = buildAttribute(8) {
                int(1, 1, GLDataType.GL_UNSIGNED_SHORT)
                float(2, 3, GLDataType.GL_UNSIGNED_BYTE, false)
                float(3, 3, GLDataType.GL_UNSIGNED_BYTE, false)
            }
        }
    }

    inner class LayerBatch {
        private var serverBuffer: BufferObject? = null
        private val cachedClientBuffer = CachedBuffer(storage.regionChunkCount * 20)
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
            val clientBuffer = cachedClientBuffer.ensureCapacityByte((count + 1) * 20)
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
                    val newSize = min(clientBuffer.remaining() * 2, storage.regionChunkCount * FaceData.MAX_COUNT * 20)
                    buffer = BufferObject.Immutable().allocate(newSize, GL_DYNAMIC_STORAGE_BIT)
                    glNamedBufferSubData(buffer.id, 0L, clientBuffer)
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