@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("BufferUtils")

package me.luna.fastmc.shared.util

import java.nio.*

inline fun allocateByte(capacity: Int): ByteBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())

inline fun allocateShort(capacity: Int): ShortBuffer = allocateByte(capacity * 2).asShortBuffer()

inline fun allocateInt(capacity: Int): IntBuffer = allocateByte(capacity * 4).asIntBuffer()

inline fun allocateFloat(capacity: Int): FloatBuffer = allocateByte(capacity * 4).asFloatBuffer()

inline fun Buffer.skip(count: Int) {
    this.position(position() + count)
}

interface ICachedBuffer {
    fun getByte(): ByteBuffer

    fun getWithCapacityByte(minCapacity: Int, newCapacity: Int): ByteBuffer

    fun getWithCapacityByte(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ByteBuffer

    fun ensureCapacityByte(minCapacity: Int, newCapacity: Int): ByteBuffer

    fun ensureCapacityByte(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ByteBuffer


    fun getShort(): ShortBuffer

    fun getWithCapacityShort(minCapacity: Int, newCapacity: Int): ShortBuffer

    fun getWithCapacityShort(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ShortBuffer

    fun ensureCapacityShort(minCapacity: Int, newCapacity: Int): ShortBuffer

    fun ensureCapacityShort(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ShortBuffer


    fun getInt(): IntBuffer

    fun getWithCapacityInt(minCapacity: Int, newCapacity: Int): IntBuffer

    fun getWithCapacityInt(minCapacity: Int, newCapacity: Int, maxCapacity: Int): IntBuffer

    fun ensureCapacityInt(minCapacity: Int, newCapacity: Int): IntBuffer

    fun ensureCapacityInt(minCapacity: Int, newCapacity: Int, maxCapacity: Int): IntBuffer


    fun getFloat(): FloatBuffer

    fun getWithCapacityFloat(minCapacity: Int, newCapacity: Int): FloatBuffer

    fun getWithCapacityFloat(minCapacity: Int, newCapacity: Int, maxCapacity: Int): FloatBuffer

    fun ensureCapacityFloat(minCapacity: Int, newCapacity: Int): FloatBuffer

    fun ensureCapacityFloat(minCapacity: Int, newCapacity: Int, maxCapacity: Int): FloatBuffer
}

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
class CachedBuffer(initialCapacity: Int) : ICachedBuffer {
    @Volatile
    private var byteBuffer = allocateByte(initialCapacity)

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

    override fun getByte(): ByteBuffer {
        return byteBuffer
    }

    override fun getWithCapacityByte(minCapacity: Int, newCapacity: Int): ByteBuffer {
        if (byteBuffer.capacity() < minCapacity) {
            allocate(newCapacity)
        }
        byteBuffer.clear()
        return getByte()
    }

    override fun getWithCapacityByte(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ByteBuffer {
        if (byteBuffer.capacity() < minCapacity || byteBuffer.capacity() > maxCapacity) {
            allocate(newCapacity)
        }
        byteBuffer.clear()
        return getByte()
    }

    override fun ensureCapacityByte(minCapacity: Int, newCapacity: Int): ByteBuffer {
        if (byteBuffer.capacity() < minCapacity) {
            reallocate(newCapacity)
        }
        return getByte()
    }

    override fun ensureCapacityByte(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ByteBuffer {
        if (byteBuffer.capacity() !in minCapacity..maxCapacity) {
            reallocate(newCapacity)
        }
        return getByte()
    }


    override fun getShort(): ShortBuffer {
        var buffer = shortBuffer
        if (buffer == null) {
            buffer = this.byteBuffer.asShortBuffer()!!
            shortBuffer = buffer
        }
        return buffer
    }

    override fun getWithCapacityShort(minCapacity: Int, newCapacity: Int): ShortBuffer {
        if (byteBuffer.capacity() < minCapacity * 2) {
            allocate(newCapacity * 2)
        }
        byteBuffer.clear()
        return getShort()
    }

    override fun getWithCapacityShort(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ShortBuffer {
        if (byteBuffer.capacity() < minCapacity * 2 || byteBuffer.capacity() > maxCapacity * 2) {
            allocate(newCapacity * 2)
        }
        byteBuffer.clear()
        return getShort()
    }

    override fun ensureCapacityShort(minCapacity: Int, newCapacity: Int): ShortBuffer {
        if (byteBuffer.capacity() < minCapacity * 2) {
            reallocate(newCapacity * 2)
        }
        return getShort()
    }

    override fun ensureCapacityShort(minCapacity: Int, newCapacity: Int, maxCapacity: Int): ShortBuffer {
        if (byteBuffer.capacity() < minCapacity * 2 || byteBuffer.capacity() > maxCapacity * 2) {
            reallocate(newCapacity * 2)
        }
        return getShort()
    }


    override fun getInt(): IntBuffer {
        var buffer = intBuffer
        if (buffer == null) {
            buffer = this.byteBuffer.asIntBuffer()!!
            intBuffer = buffer
        }
        return buffer
    }

    override fun getWithCapacityInt(minCapacity: Int, newCapacity: Int): IntBuffer {
        if (byteBuffer.capacity() < minCapacity * 2) {
            allocate(newCapacity * 2)
        }
        byteBuffer.clear()
        return getInt()
    }

    override fun getWithCapacityInt(minCapacity: Int, newCapacity: Int, maxCapacity: Int): IntBuffer {
        if (byteBuffer.capacity() < minCapacity * 2 || byteBuffer.capacity() > maxCapacity * 2) {
            allocate(newCapacity * 2)
        }
        byteBuffer.clear()
        return getInt()
    }

    override fun ensureCapacityInt(minCapacity: Int, newCapacity: Int): IntBuffer {
        if (byteBuffer.capacity() < minCapacity * 2) {
            reallocate(newCapacity * 2)
        }
        return getInt()
    }

    override fun ensureCapacityInt(minCapacity: Int, newCapacity: Int, maxCapacity: Int): IntBuffer {
        if (byteBuffer.capacity() < minCapacity * 2 || byteBuffer.capacity() > maxCapacity * 2) {
            reallocate(newCapacity * 2)
        }
        return getInt()
    }


    override fun getFloat(): FloatBuffer {
        var buffer = floatBuffer
        if (buffer == null) {
            buffer = this.byteBuffer.asFloatBuffer()!!
            floatBuffer = buffer
        }
        return buffer
    }

    override fun getWithCapacityFloat(minCapacity: Int, newCapacity: Int): FloatBuffer {
        if (byteBuffer.capacity() < minCapacity * 2) {
            allocate(newCapacity * 2)
        }
        byteBuffer.clear()
        return getFloat()
    }

    override fun getWithCapacityFloat(minCapacity: Int, newCapacity: Int, maxCapacity: Int): FloatBuffer {
        if (byteBuffer.capacity() < minCapacity * 2 || byteBuffer.capacity() > maxCapacity * 2) {
            allocate(newCapacity * 2)
        }
        byteBuffer.clear()
        return getFloat()
    }

    override fun ensureCapacityFloat(minCapacity: Int, newCapacity: Int): FloatBuffer {
        if (byteBuffer.capacity() < minCapacity * 2) {
            reallocate(newCapacity * 2)
        }
        return getFloat()
    }

    override fun ensureCapacityFloat(minCapacity: Int, newCapacity: Int, maxCapacity: Int): FloatBuffer {
        if (byteBuffer.capacity() < minCapacity * 2 || byteBuffer.capacity() > maxCapacity * 2) {
            reallocate(newCapacity * 2)
        }
        return getFloat()
    }
}