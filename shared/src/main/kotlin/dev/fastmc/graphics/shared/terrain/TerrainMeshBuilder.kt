package dev.fastmc.graphics.shared.terrain

import dev.luna5ama.kmogus.usePtr
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
            bufferGroup = BufferGroup(task, arrayOfNulls(bufferCount))
            init0()
        }
    }

    open fun init0() {

    }

    fun finish(): BufferGroup? {
        val group = bufferGroup
        bufferGroup = null
        if (group != null) {
            group.finish(task!!)
            if (group.vertexBuffers.all { it == null }) {
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
        if (region.arr.rem < 16) {
            region.expand(task!!)
        }

        region.arr.usePtr {
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
    }

    open fun putQuad(faceBit: Int) {
        val bufferGroup = bufferGroup!!
        bufferGroup.quadCounts[faceBit - 1]++
    }

    class BufferGroup(
        private val task: ChunkBuilderTask,
        val vertexBuffers: Array<BufferContext?>
    ) {
        val quadCounts = IntArray(vertexBuffers.size)

        fun getVertexBuffer(faceBitIndex: Int): BufferContext {
            var buffer = vertexBuffers[faceBitIndex]
            if (buffer == null) {
                buffer = task.renderer.contextProvider.getBufferContext(task)
                vertexBuffers[faceBitIndex] = buffer
            }
            return buffer
        }

        fun finish(task: ChunkBuilderTask) {
            vertexBuffers.forEachIndexed { i, it ->
                if (it == null) return@forEachIndexed
                if (quadCounts[i] > 0) {
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

    val quadCount get() = posArrayList.size / 12

    fun getQuadCenterArray(): FloatArray {
        val result = FloatArray(quadCount * 3)
        val posArray = posArrayList.elements()

        for (i in 0 until quadCount) {
            val centerIndex = i * 3
            val posIndex = i * 12
            result[centerIndex] =
                (posArray[posIndex] + posArray[posIndex + 3] + posArray[posIndex + 6] + posArray[posIndex + 9]) / 4.0f
            result[centerIndex + 1] =
                (posArray[posIndex + 1] + posArray[posIndex + 4] + posArray[posIndex + 7] + posArray[posIndex + 10]) / 4.0f
            result[centerIndex + 2] =
                (posArray[posIndex + 2] + posArray[posIndex + 5] + posArray[posIndex + 8] + posArray[posIndex + 11]) / 4.0f
        }

        return result
    }

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