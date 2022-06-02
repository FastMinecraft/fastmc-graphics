package me.luna.fastmc.shared.terrain

import it.unimi.dsi.fastutil.floats.FloatArrayList
import me.luna.fastmc.shared.util.skip
import sun.misc.Unsafe

open class ChunkVertexBuilder {
    @Volatile
    var bufferPair: Pair<BufferContext, BufferContext>? = null

    @Volatile
    var task: ChunkBuilderTask? = null

    private var vertexCount = 0
    private var vertexByteIndex = 0L
    private var indexByteIndex = 0L

    suspend inline fun initBuffer(task: ChunkBuilderTask, contextProvider: ContextProvider) {
        this.task = task
        if (bufferPair == null) {
            bufferPair = contextProvider.getBufferContext(task) to contextProvider.getBufferContext(task)
            init0()
        }
    }

    open fun init0() {
        vertexCount = 0
        vertexByteIndex = 0L
        indexByteIndex = 0L
    }

    fun popBuffer(): Pair<BufferContext, BufferContext>? {
        val pair = bufferPair
        bufferPair = null

        if (pair != null) {
            if (vertexCount == 0) {
                pair.first.release(task!!)
                pair.second.release(task!!)
                return null
            }

            pair.first.region.buffer.flip()
            pair.second.region.buffer.flip()
        }

        return pair
    }

    fun clearBuffer() {
        bufferPair = null
    }

    open fun putVertex(x: Float, y: Float, z: Float, r: Int, g: Int, b: Int, u: Float, v: Float, lightMapUV: Int) {
        val region = bufferPair!!.first.region
        val buffer = region.buffer
        if (buffer.remaining() < 16) {
            region.expand(task!!)
        }

        val address = region.address + vertexByteIndex

        UNSAFE.putShort(address, ((x + 0.25f) * 255.49707f).toInt().toShort())
        UNSAFE.putShort(address + 2L, ((y + 0.25f) * 255.49707f).toInt().toShort())
        UNSAFE.putShort(address + 4L, ((z + 0.25f) * 255.49707f).toInt().toShort())

        UNSAFE.putShort(address + 6L, (u * 65535.0f).toInt().toShort())
        UNSAFE.putShort(address + 8L, (v * 65535.0f).toInt().toShort())

        UNSAFE.putShort(address + 10L, lightMapUV.toShort())

        UNSAFE.putByte(address + 12L, r.toByte())
        UNSAFE.putByte(address + 13L, g.toByte())
        UNSAFE.putByte(address + 14L, b.toByte())

        vertexByteIndex += 16L
        buffer.skip(16)
    }

    fun putQuad() {
        val region = bufferPair!!.second.region
        val buffer = region.buffer
        if (buffer.remaining() < 24) {
            region.expand(task!!)
        }

        val address = region.address + indexByteIndex

        UNSAFE.putInt(address, vertexCount)
        UNSAFE.putInt(address + 4L, vertexCount + 1)
        UNSAFE.putInt(address + 8L, vertexCount + 3)

        UNSAFE.putInt(address + 12L, vertexCount + 2)
        UNSAFE.putInt(address + 16L, vertexCount + 3)
        UNSAFE.putInt(address + 20L, vertexCount + 1)

        vertexCount += 4
        indexByteIndex += 24L
        buffer.skip(24)
    }

    private companion object {
        @JvmField
        val UNSAFE = run {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            field.get(null) as Unsafe
        }
    }
}

class TranslucentChunkVertexBuilder : ChunkVertexBuilder() {
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
        lightMapUV: Int
    ) {
        super.putVertex(x, y, z, r, g, b, u, v, lightMapUV)
        posArrayList.ensureCapacity(posArrayList.size + 3)
        posArrayList.add(x)
        posArrayList.add(y)
        posArrayList.add(z)
    }
}