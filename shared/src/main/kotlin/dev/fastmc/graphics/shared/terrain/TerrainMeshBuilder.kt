package dev.fastmc.graphics.shared.terrain

import it.unimi.dsi.fastutil.floats.FloatArrayList

abstract class TerrainMeshBuilder {
    @Volatile
    var task: ChunkBuilderTask? = null

    abstract val bufferCount: Int

    @Volatile
    var bufferGroup: BufferGroup? = null

    private var modelAttribute = 0b0000_0001

    fun initBuffer(task: ChunkBuilderTask, layer: IPatchedRenderLayer) {
        this.task = task
        modelAttribute = layer.modelAttribute

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
            g.finish(task!!)
            if (g.vertexBuffers.all { it == null }) {
                return null
            }
        }
        return group
    }

    fun clearBuffer() {
        bufferGroup = null
        modelAttribute = 0b0000_0001
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
        val arr = region.arr
        check(arr.len == arr.baseLen) {"WtF"}
        if (arr.rem < 16) {
            region.expand(task!!)
        }

        arr.usePtr {
            setShortInc(((x + 0.25f) * 3971.818f).toInt().toShort())
                .setShortInc(((y + 0.25f) * 3971.818f).toInt().toShort())
                .setShortInc(((z + 0.25f) * 3971.818f).toInt().toShort())

                .setShortInc((u * 65535.0f).toInt().toShort())
                .setShortInc((v * 65535.0f).toInt().toShort())

                .setShortInc(lightMapUV.toShort())

                .setByteInc(r.toByte())
                .setByteInc(g.toByte())
                .setByteInc(b.toByte())

                .setByteInc(modelAttribute.toByte())
        }

        bufferGroup.vertexByteIndices[faceBit - 1] += 16L
    }

    open fun putQuad(faceBit: Int) {
        val bufferGroup = bufferGroup!!
        val region = bufferGroup.getIndexBuffer(faceBit - 1).region
        val arr = region.arr
        if (arr.rem < 24) {
            region.expand(task!!)
        }

        val vertexCount = bufferGroup.vertexCounts[faceBit - 1]

        arr.usePtr {
            setIntInc(vertexCount)
                .setIntInc(vertexCount + 1)
                .setIntInc(vertexCount + 3)

                .setIntInc(vertexCount + 2)
                .setIntInc(vertexCount + 3)
                .setIntInc(vertexCount + 1)
        }

        bufferGroup.vertexCounts[faceBit - 1] += 4
        bufferGroup.indexByteIndices[faceBit - 1] += 24L
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

        fun finish(task: ChunkBuilderTask) {
            vertexBuffers.forEachIndexed { i, it ->
                if (it == null) return@forEachIndexed
                if (vertexCounts[i] > 0) {
                    it.region.arr.flip()
                } else {
                    it.release(task)
                    vertexBuffers[i] = null
                }
            }
            indexBuffers.forEachIndexed { i, it ->
                if (it == null) return@forEachIndexed
                if (vertexCounts[i] > 0) {
                    it.region.arr.flip()
                } else {
                    it.release(task)
                    vertexBuffers[i] = null
                }
            }
        }
    }
}

class OpaqueTerrainMeshBuilder : TerrainMeshBuilder() {
    override val bufferCount: Int
        get() = 63
}

class TranslucentMeshBuilder : TerrainMeshBuilder() {
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