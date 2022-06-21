@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("BufferUtils")

package me.luna.fastmc.shared.util

import java.nio.*

inline fun allocateByte(capacity: Int): ByteBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())

inline fun allocateShort(capacity: Int): ShortBuffer = allocateByte(capacity * 4).asShortBuffer()

inline fun allocateInt(capacity: Int): IntBuffer = allocateByte(capacity * 4).asIntBuffer()

inline fun allocateFloat(capacity: Int): FloatBuffer = allocateByte(capacity * 4).asFloatBuffer()

inline fun <T : Buffer> T.skip(count: Int): Buffer {
    this.position(position() + count)
    return this
}

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
class CachedBuffer(initialCapacity: Int) {
    @Volatile
    private var byteBuffer = allocateByte(initialCapacity)

    @Volatile
    private var charBuffer: CharBuffer? = null

    @Volatile
    private var shortBuffer: ShortBuffer? = null

    @Volatile
    private var intBuffer: IntBuffer? = null

    @Volatile
    private var floatBuffer: FloatBuffer? = null

    private fun allocate(capacity: Int) {
        byteBuffer = allocateByte(capacity)
        shortBuffer = null
        floatBuffer = null
        intBuffer = null
    }

    private fun reallocate(capacity: Int) {
        byteBuffer.flip()
        val newBuffer = allocateByte(capacity)
        newBuffer.put(byteBuffer)

        byteBuffer = newBuffer
        shortBuffer = null
        floatBuffer = null
        intBuffer = null
    }

    fun getByte(): ByteBuffer {
        return byteBuffer
    }

    fun getWithCapacityByte(minCapacity: Int, newCapacity: Int): ByteBuffer {
        if (byteBuffer.capacity() < minCapacity) {
            allocate(newCapacity)
        }
        byteBuffer.clear()
        return getByte()
    }

    fun getWithCapacityByte(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ByteBuffer {
        if (byteBuffer.capacity() < minCapacity || byteBuffer.capacity() > maxCapacity) {
            allocate(newCapacity)
        }
        byteBuffer.clear()
        return getByte()
    }

    fun ensureCapacityByte(minCapacity: Int, newCapacity: Int): ByteBuffer {
        if (byteBuffer.capacity() < minCapacity) {
            reallocate(newCapacity)
        }
        return getByte()
    }

    fun ensureCapacityByte(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ByteBuffer {
        if (byteBuffer.capacity() !in minCapacity..maxCapacity) {
            reallocate(newCapacity)
        }
        return getByte()
    }


    fun getChar(): CharBuffer {
        var buffer = charBuffer
        if (buffer == null) {
            buffer = this.byteBuffer.asCharBuffer()!!
            charBuffer = buffer
        }
        return buffer
    }

    fun getWithCapacityChar(minCapacity: Int, newCapacity: Int): CharBuffer {
        if (byteBuffer.capacity() < minCapacity * 4) {
            allocate(newCapacity * 4)
        }
        byteBuffer.clear()
        return getChar()
    }

    fun getWithCapacityChar(minCapacity: Int, newCapacity: Int, maxCapacity: Int): CharBuffer {
        if (byteBuffer.capacity() < minCapacity * 4 || byteBuffer.capacity() > maxCapacity * 4) {
            allocate(newCapacity * 4)
        }
        byteBuffer.clear()
        return getChar()
    }

    fun ensureCapacityChar(minCapacity: Int, newCapacity: Int): CharBuffer {
        if (byteBuffer.capacity() < minCapacity * 4) {
            reallocate(newCapacity * 4)
        }
        return getChar()
    }

    fun ensureCapacityChar(minCapacity: Int, newCapacity: Int, maxCapacity: Int): CharBuffer {
        if (byteBuffer.capacity() < minCapacity * 4 || byteBuffer.capacity() > maxCapacity * 4) {
            reallocate(newCapacity * 4)
        }
        return getChar()
    }


    fun getShort(): ShortBuffer {
        var buffer = shortBuffer
        if (buffer == null) {
            buffer = this.byteBuffer.asShortBuffer()!!
            shortBuffer = buffer
        }
        return buffer
    }

    fun getWithCapacityShort(minCapacity: Int, newCapacity: Int): ShortBuffer {
        if (byteBuffer.capacity() < minCapacity * 4) {
            allocate(newCapacity * 4)
        }
        byteBuffer.clear()
        return getShort()
    }

    fun getWithCapacityShort(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ShortBuffer {
        if (byteBuffer.capacity() < minCapacity * 4 || byteBuffer.capacity() > maxCapacity * 4) {
            allocate(newCapacity * 4)
        }
        byteBuffer.clear()
        return getShort()
    }

    fun ensureCapacityShort(minCapacity: Int, newCapacity: Int): ShortBuffer {
        if (byteBuffer.capacity() < minCapacity * 4) {
            reallocate(newCapacity * 4)
        }
        return getShort()
    }

    fun ensureCapacityShort(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ShortBuffer {
        if (byteBuffer.capacity() < minCapacity * 4 || byteBuffer.capacity() > maxCapacity * 4) {
            reallocate(newCapacity * 4)
        }
        return getShort()
    }


    fun getInt(): IntBuffer {
        var buffer = intBuffer
        if (buffer == null) {
            buffer = this.byteBuffer.asIntBuffer()!!
            intBuffer = buffer
        }
        return buffer
    }

    fun getWithCapacityInt(minCapacity: Int, newCapacity: Int): IntBuffer {
        if (byteBuffer.capacity() < minCapacity * 4) {
            allocate(newCapacity * 4)
        }
        byteBuffer.clear()
        return getInt()
    }

    fun getWithCapacityInt(minCapacity: Int, newCapacity: Int, maxCapacity: Int): IntBuffer {
        if (byteBuffer.capacity() < minCapacity * 4 || byteBuffer.capacity() > maxCapacity * 4) {
            allocate(newCapacity * 4)
        }
        byteBuffer.clear()
        return getInt()
    }

    fun ensureCapacityInt(minCapacity: Int, newCapacity: Int): IntBuffer {
        if (byteBuffer.capacity() < minCapacity * 4) {
            reallocate(newCapacity * 4)
        }
        return getInt()
    }

    fun ensureCapacityInt(minCapacity: Int, newCapacity: Int, maxCapacity: Int): IntBuffer {
        if (byteBuffer.capacity() < minCapacity * 4 || byteBuffer.capacity() > maxCapacity * 4) {
            reallocate(newCapacity * 4)
        }
        return getInt()
    }


    fun getFloat(): FloatBuffer {
        var buffer = floatBuffer
        if (buffer == null) {
            buffer = this.byteBuffer.asFloatBuffer()!!
            floatBuffer = buffer
        }
        return buffer
    }

    fun getWithCapacityFloat(minCapacity: Int, newCapacity: Int): FloatBuffer {
        if (byteBuffer.capacity() < minCapacity * 4) {
            allocate(newCapacity * 4)
        }
        byteBuffer.clear()
        return getFloat()
    }

    fun getWithCapacityFloat(minCapacity: Int, newCapacity: Int, maxCapacity: Int): FloatBuffer {
        if (byteBuffer.capacity() < minCapacity * 4 || byteBuffer.capacity() > maxCapacity * 4) {
            allocate(newCapacity * 4)
        }
        byteBuffer.clear()
        return getFloat()
    }

    fun ensureCapacityFloat(minCapacity: Int, newCapacity: Int): FloatBuffer {
        if (byteBuffer.capacity() < minCapacity * 4) {
            reallocate(newCapacity * 4)
        }
        return getFloat()
    }

    fun ensureCapacityFloat(minCapacity: Int, newCapacity: Int, maxCapacity: Int): FloatBuffer {
        if (byteBuffer.capacity() < minCapacity * 4 || byteBuffer.capacity() > maxCapacity * 4) {
            reallocate(newCapacity * 4)
        }
        return getFloat()
    }
}