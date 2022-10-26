package me.luna.fastmc.shared.terrain

import it.unimi.dsi.fastutil.floats.FloatArrayList
import me.luna.fastmc.shared.util.UNSAFE
import me.luna.fastmc.shared.util.skip

abstract class TerrainVertexBuilder {
    @Volatile
    var task: ChunkBuilderTask? = null

    abstract val bufferCount: Int

    @Volatile
    var bufferGroup: BufferGroup? = null

    fun initBuffer(task: ChunkBuilderTask) {
        this.task = task
        if (bufferGroup == null) {
            bufferGroup = BufferGroup(
                task,
                arrayOfNulls(bufferCount),
                arrayOfNulls(bufferCount),
            )
            init0()
        }
    }

    open fun init0() {

    }

    fun finish(): BufferGroup? {
        val group = bufferGroup
        bufferGroup = null
        group?.let { g ->
            g.flip(task!!)
            if (g.vertexBuffers.all { it == null }) {
                return null
            }
        }
        return group
    }

    fun clearBuffer() {
        bufferGroup = null
    }

    open fun putVertex(
        x: Float,
        y: Float,
        z: Float,
        r: Int,
        g: Int,
        b: Int,
        u: Float,
        v: Float,
        lightMapUV: Int,
        faceBit: Int
    ) {
        val bufferGroup = bufferGroup!!
        val region = bufferGroup.getVertexBuffer(faceBit - 1).region
        val buffer = region.buffer
        if (buffer.remaining() < 16) {
            region.expand(task!!)
        }

        val address = region.address + bufferGroup.vertexByteIndices[faceBit - 1]

        UNSAFE.putShort(address, ((x + 0.25f) * 3971.818f).toInt().toShort())
        UNSAFE.putShort(address + 2L, ((y + 0.25f) * 3971.818f).toInt().toShort())
        UNSAFE.putShort(address + 4L, ((z + 0.25f) * 3971.818f).toInt().toShort())

        UNSAFE.putShort(address + 6L, (u * 65535.0f).toInt().toShort())
        UNSAFE.putShort(address + 8L, (v * 65535.0f).toInt().toShort())

        UNSAFE.putShort(address + 10L, (lightMapUV + 0x0808).toShort())

        UNSAFE.putByte(address + 12L, r.toByte())
        UNSAFE.putByte(address + 13L, g.toByte())
        UNSAFE.putByte(address + 14L, b.toByte())

        bufferGroup.vertexByteIndices[faceBit - 1] += 16L
        buffer.skip(16)
    }

    open fun putQuad(faceBit: Int) {
        val bufferGroup = bufferGroup!!
        val region = bufferGroup.getIndexBuffer(faceBit - 1).region
        val buffer = region.buffer
        if (buffer.remaining() < 24) {
            region.expand(task!!)
        }

        val vertexCount = bufferGroup.vertexCounts[faceBit - 1]
        val address = region.address + bufferGroup.indexByteIndices[faceBit - 1]

        UNSAFE.putInt(address, vertexCount)
        UNSAFE.putInt(address + 4L, vertexCount + 1)
        UNSAFE.putInt(address + 8L, vertexCount + 3)

        UNSAFE.putInt(address + 12L, vertexCount + 2)
        UNSAFE.putInt(address + 16L, vertexCount + 3)
        UNSAFE.putInt(address + 20L, vertexCount + 1)

        bufferGroup.vertexCounts[faceBit - 1] += 4
        bufferGroup.indexByteIndices[faceBit - 1] += 24L
        buffer.skip(24)
    }

    class BufferGroup(
        private val task: ChunkBuilderTask,
        val vertexBuffers: Array<BufferContext?>,
        val indexBuffers: Array<BufferContext?>
    ) {
        val vertexCounts = IntArray(vertexBuffers.size)
        val vertexByteIndices = LongArray(vertexBuffers.size)
        val indexByteIndices = LongArray(indexBuffers.size)

        fun getVertexBuffer(faceBitIndex: Int): BufferContext {
            var buffer = vertexBuffers[faceBitIndex]
            if (buffer == null) {
                buffer = task.renderer.contextProvider.getBufferContext(task)
                vertexBuffers[faceBitIndex] = buffer
            }
            return buffer
        }

        fun getIndexBuffer(faceBit: Int): BufferContext {
            var buffer = indexBuffers[faceBit]
            if (buffer == null) {
                buffer = task.renderer.contextProvider.getBufferContext(task)
                indexBuffers[faceBit] = buffer
            }
            return buffer
        }

        fun flip(task: ChunkBuilderTask) {
            vertexBuffers.forEachIndexed { i, it ->
                if (it == null) return@forEachIndexed
                if (vertexCounts[i] > 0) {
                    it.region.buffer.flip()
                } else {
                    it.release(task)
                    vertexBuffers[i] = null
                }
            }
            indexBuffers.forEachIndexed { i, it ->
                if (it == null) return@forEachIndexed
                if (vertexCounts[i] > 0) {
                    it.region.buffer.flip()
                } else {
                    it.release(task)
                    vertexBuffers[i] = null
                }
            }
        }
    }
}

class OpaqueTerrainVertexBuilder : TerrainVertexBuilder() {
    override val bufferCount: Int
        get() = 63
}

class TranslucentVertexBuilder : TerrainVertexBuilder() {
    override val bufferCount: Int
        get() = 1

    val posArrayList = FloatArrayList()

    override fun init0() {
        super.init0()
        posArrayList.clear()
    }

    override fun putVertex(
        x: Float,
        y: Float,
        z: Float,
        r: Int,
        g: Int,
        b: Int,
        u: Float,
        v: Float,
        lightMapUV: Int,
        faceBit: Int
    ) {
        super.putVertex(x, y, z, r, g, b, u, v, lightMapUV, 1)
        posArrayList.ensureCapacity(posArrayList.size + 3)
        posArrayList.add(x)
        posArrayList.add(y)
        posArrayList.add(z)
    }

    override fun putQuad(faceBit: Int) {
        super.putQuad(1)
    }
}