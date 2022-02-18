package me.luna.fastmc.shared.renderbuilder

import java.nio.ByteBuffer

class ParallelBuilderWorker {
    private var buffer: ByteBuffer? = null
    private var index = -1
    private var localIndex = 0

    fun <T : IInfo<*>> run(builder: IParallelBuilder<T>, startIndex: Int, sequence: Sequence<T>) {
        buffer = builder.buffer
        val vertexSize = builder.vertexSize
        this.index = startIndex * vertexSize

        for (info in sequence) {
            builder.addParallel(this, info)
            index += vertexSize
            localIndex = 0
        }
    }

    fun put(value: Byte) {
        buffer!!.put(index + localIndex, value)
        localIndex += 1
    }

    fun putShort(value: Short) {
        buffer!!.putShort(index + localIndex, value)
        localIndex += 2
    }

    fun putInt(value: Int) {
        buffer!!.putInt(index + localIndex, value)
        localIndex += 4
    }

    fun putFloat(value: Float) {
        buffer!!.putFloat(index + localIndex, value)
        localIndex += 4
    }
}