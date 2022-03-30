@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("BufferUtils")

package me.luna.fastmc.shared.util

import it.unimi.dsi.fastutil.ints.IntArrayList
import java.nio.*

inline fun allocateInt(capacity: Int): IntBuffer = allocateByte(capacity * 4).asIntBuffer()

inline fun allocateFloat(capacity: Int): FloatBuffer = allocateByte(capacity * 4).asFloatBuffer()

inline fun allocateShort(capacity: Int): ShortBuffer = allocateByte(capacity * 2).asShortBuffer()

inline fun allocateByte(capacity: Int): ByteBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())

inline fun Buffer.skip(count: Int) {
    this.position(position() + count)
}

class CachedByteBuffer(initialCapacity: Int) {
    private var buffer = allocateByte(initialCapacity)

    fun getWithCapacity(minCapacity: Int, newCapacity: Int): ByteBuffer {
        if (buffer.capacity() < minCapacity) {
            buffer = allocateByte(newCapacity)
        }

        buffer.clear()
        return buffer
    }

    fun get(): ByteBuffer {
        return buffer
    }
}

class CachedIntBuffer(initialCapacity: Int) {
    private var buffer = allocateInt(initialCapacity)

    fun getWithCapacity(minCapacity: Int, newCapacity: Int): IntBuffer {
        if (buffer.capacity() < minCapacity) {
            buffer = allocateInt(newCapacity)
        }

        buffer.clear()
        return buffer
    }

    fun get(): IntBuffer {
        return buffer
    }
}