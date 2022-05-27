package me.luna.fastmc.shared.terrain

import it.unimi.dsi.fastutil.floats.FloatArrayList
import me.luna.fastmc.shared.util.skip

open class ChunkVertexBuilder {
    @Volatile
    var bufferContext: BufferContext? = null

    @Volatile
    var task: ChunkBuilderTask? = null

    suspend inline fun initBuffer(task: ChunkBuilderTask, contextProvider: ContextProvider) {
        this.task = task
        if (bufferContext == null) {
            bufferContext = contextProvider.getBufferContext(task)
            init0()
        }
    }

    open fun init0() {

    }

    fun popBuffer(): BufferContext? {
        val buffer = bufferContext
        bufferContext = null
        return buffer
    }

    fun clearBuffer() {
        bufferContext = null
    }

    open fun putVertex(x: Float, y: Float, z: Float, r: Int, g: Int, b: Int, u: Float, v: Float, lightMapUV: Int) {
        val region = bufferContext!!.region
        if (region.buffer.remaining() < 16) {
            region.expand(task!!)
        }

        val buffer = region.buffer
        buffer.putShort(((x + 16.0f) * 227.55556f).toInt().toShort())
        buffer.putShort(((y + 16.0f) * 227.55556f).toInt().toShort())
        buffer.putShort(((z + 16.0f) * 227.55556f).toInt().toShort())

        buffer.putShort((u * 65535.0f).toInt().toShort())
        buffer.putShort((v * 65535.0f).toInt().toShort())

        buffer.putShort(lightMapUV.toShort())

        buffer.put(r.toByte())
        buffer.put(g.toByte())
        buffer.put(b.toByte())
        buffer.skip(1)
    }
}

class TranslucentChunkVertexBuilder : ChunkVertexBuilder() {
    val posArrayList = FloatArrayList()

    override fun init0() {
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
        lightMapUV: Int
    ) {
        super.putVertex(x, y, z, r, g, b, u, v, lightMapUV)
        posArrayList.ensureCapacity(posArrayList.size + 3)
        posArrayList.add(x + 16.0f)
        posArrayList.add(y + 16.0f)
        posArrayList.add(z + 16.0f)
    }
}