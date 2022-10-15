@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("BufferUtils")

package me.luna.fastmc.shared.util

import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import sun.misc.Unsafe
import java.nio.*
import java.util.function.Consumer
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

internal val UNSAFE = run {
    val field = Unsafe::class.java.getDeclaredField("theUnsafe")
    field.isAccessible = true
    field.get(null) as Unsafe
}

private val ADDRESS_OFFSET = UNSAFE.objectFieldOffset(Buffer::class.java.getDeclaredField("address"))
private val POSITION_OFFSET = UNSAFE.objectFieldOffset(Buffer::class.java.getDeclaredField("position"))
private val MARK_OFFSET = UNSAFE.objectFieldOffset(Buffer::class.java.getDeclaredField("mark"))
private val LIMIT_OFFSET = UNSAFE.objectFieldOffset(Buffer::class.java.getDeclaredField("limit"))
private val CAPACITY_OFFSET = UNSAFE.objectFieldOffset(Buffer::class.java.getDeclaredField("capacity"))

var ByteBuffer.address
    get() = UNSAFE.getLong(this, ADDRESS_OFFSET)
    set(value) {
        UNSAFE.putLong(this, ADDRESS_OFFSET, value)
    }

var ByteBuffer.position
    get() = position()
    set(value) {
        UNSAFE.putInt(this, POSITION_OFFSET, value)
    }

var ByteBuffer.mark
    get() = UNSAFE.getInt(this, MARK_OFFSET)
    set(value) {
        UNSAFE.putInt(this, MARK_OFFSET, value)
    }

var ByteBuffer.limit
    get() = limit()
    set(value) {
        UNSAFE.putInt(this, LIMIT_OFFSET, value)
    }

var ByteBuffer.capacity
    get() = capacity()
    set(value) {
        UNSAFE.putInt(this, CAPACITY_OFFSET, value)
    }

private val DIRECT_BYTE_BUFFER_CLASS = allocateByte(0).javaClass

fun wrapDirectByteBuffer(address: Long, capacity: Int): ByteBuffer {
    val buffer = UNSAFE.allocateInstance(DIRECT_BYTE_BUFFER_CLASS) as ByteBuffer

    buffer.address = address
    buffer.mark = -1
    buffer.limit = capacity
    buffer.capacity = capacity

    return buffer
}

private val FREE_FUNC = run {
    try {
        val invokeCleanerMethod = UNSAFE.javaClass.getDeclaredMethod("invokeCleaner", ByteBuffer::class.java)
        Consumer<ByteBuffer> {
            invokeCleanerMethod.invoke(UNSAFE, it)
        }
    } catch (e: NoSuchMethodException) {
        val cleanerField = DIRECT_BYTE_BUFFER_CLASS.getDeclaredField("cleaner")
        val cleanerOffset = UNSAFE.objectFieldOffset(cleanerField)
        val cleanMethod = cleanerField.type.getDeclaredMethod("clean")
        cleanMethod.isAccessible = true
        Consumer<ByteBuffer> { buffer ->
            UNSAFE.getObject(buffer, cleanerOffset)?.let {
                cleanMethod.invoke(it)
            }
        }
    }
}

fun ByteBuffer.free() {
    FREE_FUNC.accept(this)
}

class MemoryStack private constructor(initCapacity: Int) {
    private var rawBuffer = allocateByte(initCapacity)
    private val objectPool = ObjectPool {
        (UNSAFE.allocateInstance(DIRECT_BYTE_BUFFER_CLASS) as ByteBuffer).order(ByteOrder.nativeOrder())
    }

    private var frameIndex = 0
    private var frameSize = 8
    private var frameBufferReference = arrayOfNulls<ByteBuffer>(frameSize)
    private var framePointers = IntArray(frameSize)
    private var frameBufferObjects = Array<FastObjectArrayList<ByteBuffer>>(frameSize) { FastObjectArrayList() }
    private var pointer = 0

    fun push(): MemoryStack {
        if (frameIndex == frameSize) {
            frameSize *= 2
            frameBufferReference = frameBufferReference.copyOf(frameSize)
            framePointers = framePointers.copyOf(frameSize)
            @Suppress("UNCHECKED_CAST")

            frameBufferObjects = frameBufferObjects.copyOf(frameSize).apply {
                for (i in frameBufferObjects.size until frameSize) {
                    this[i] = FastObjectArrayList()
                }
            } as Array<FastObjectArrayList<ByteBuffer>>
        }

        frameBufferReference[frameIndex] = rawBuffer
        framePointers[frameIndex] = pointer
        frameIndex++
        return this
    }

    fun pop(): MemoryStack {
        --frameIndex
        frameBufferReference[frameIndex] = null
        pointer = framePointers[frameIndex]

        val objects = frameBufferObjects[frameIndex]
        for (i in objects.indices) {
            objectPool.put(objects[i])
        }
        objects.clear()

        return this
    }

    fun freeBuffer0(buffer: ByteBuffer) {
        objectPool.put(buffer)
    }

    fun malloc0(size: Int): ByteBuffer {
        val newPointer = pointer + size
        if (newPointer > rawBuffer.capacity()) {
            rawBuffer = allocateByte(MathUtils.ceilToPOT(newPointer))
        }

        return objectPool.get().apply {
            address = rawBuffer.address + pointer
            pointer = newPointer

            position = 0
            mark = -1
            limit = size
            capacity = size
        }
    }

    fun calloc0(size: Int): ByteBuffer {
        return malloc0(size).apply {
            UNSAFE.setMemory(address, size.toLong(), 0)
        }
    }

    fun malloc(size: Int): ByteBuffer {
        return malloc0(size).apply {
            frameBufferObjects[frameIndex - 1].add(this)
        }
    }

    fun calloc(size: Int): ByteBuffer {
        return calloc0(size).apply {
            frameBufferObjects[frameIndex - 1].add(this)
        }
    }

    inline fun <T> withMalloc(size: Int, crossinline block: (ByteBuffer) -> T) {
        val buffer = malloc0(size)
        try {
            block(buffer)
        } finally {
            freeBuffer0(buffer)
        }
    }

    inline fun <T> withCalloc(size: Int, crossinline block: (ByteBuffer) -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        val buffer = calloc0(size)

        try {
            return block(buffer)
        } finally {
            freeBuffer0(buffer)
        }
    }

    inline fun <T> use(crossinline block: MemoryStack.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        push()
        try {
            return block.invoke(this)
        } finally {
            pop()
        }
    }

    companion object {
        private val threadLocal = ThreadLocal.withInitial { MemoryStack(1024 * 1024) }

        fun get(): MemoryStack {
            return threadLocal.get()
        }

        inline fun <T> use(crossinline block: MemoryStack.() -> T): T {
            contract {
                callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            }
            return get().use(block)
        }
    }
}