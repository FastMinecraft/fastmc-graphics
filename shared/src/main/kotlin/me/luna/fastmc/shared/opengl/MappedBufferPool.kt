package me.luna.fastmc.shared.opengl

import me.luna.fastmc.shared.terrain.ChunkBuilderTask
import me.luna.fastmc.shared.util.ConcurrentObjectPool
import me.luna.fastmc.shared.util.collection.AtomicByteArray
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.shared.util.pollEach
import me.luna.fastmc.shared.util.resume
import sun.misc.Unsafe
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class MappedBufferPool(sectorSizePower: Int, private val sectorCapacity: Int, val maxRegions: Int) {
    private val sectorSize = 1 shl sectorSizePower
    val capacity = sectorSize * sectorCapacity

    private val vbo = ImmutableVertexBufferObject(
        VertexAttribute.EMPTY,
        capacity,
        GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT
    )
    private val baseBuffer = glMapNamedBufferRange(
        vbo.id,
        0,
        capacity.toLong(),
        GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_UNSYNCHRONIZED_BIT
    )!!
    private val baseAddress = baseBuffer.address

    private val regionPool = ConcurrentObjectPool { Region() }
    private val sectorState = AtomicByteArray(sectorCapacity)
    private val usedSectorCount0 = AtomicInteger(0)
    private val unusedRegionCount0 = AtomicInteger(maxRegions)

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val notifyQueue = ConcurrentLinkedQueue<Continuation<Unit>>()

    private val pendingRelease = ConcurrentLinkedQueue<Region>()
    private val releaseTaskQueue = java.util.ArrayDeque<ReleaseTask>()

    val allocatedSize get() = usedSectorCount0.get() * sectorSize
    val allocatedRegion get() = maxRegions - unusedRegionCount0.get()

    fun update() {
        val head = releaseTaskQueue.peekFirst()
        if (head != null && head.tryRelease()) {
            releaseTaskQueue.pollFirst()
            doNotify()
        }

        if (pendingRelease.isNotEmpty()) {
            val releaseTask = ReleaseTask()
            pendingRelease.pollEach {
                releaseTask.list.add(it)
            }
            releaseTaskQueue.offerLast(releaseTask)
        }
    }

    private fun doNotify() {
        lock.withLock {
            condition.signalAll()
        }
        val pollSize = notifyQueue.size
        for (i in 0 until pollSize) {
            val continuation = notifyQueue.poll() ?: break
            continuation.resume()
        }
    }

    fun tryAllocate(): Region? {
        if (checkCounter()) {
            if (sectorState.compareAndSet(0, FALSE, TRUE)) {
                usedSectorCount0.incrementAndGet()
                return regionPool.get().init(0)
            }

            var block = sectorCapacity shr 1
            while (block > 0) {
                var i = block
                while (i < sectorCapacity) {
                    if (sectorState.compareAndSet(i, FALSE, TRUE)) {
                        usedSectorCount0.incrementAndGet()
                        return regionPool.get().init(i)
                    }
                    i += block
                }
                block = block shr 1
            }
        }

        return null
    }

    suspend fun allocate(): Region {
        while (!checkCounter()) {
            suspendCoroutine<Unit> {
                notifyQueue.add(it)
            }
        }

        if (sectorState.compareAndSet(0, FALSE, TRUE)) {
            usedSectorCount0.incrementAndGet()
            return regionPool.get().init(0)
        }

        while (true) {
            var block = sectorCapacity / 2
            while (block > 0) {
                var i = block
                while (i < sectorCapacity) {
                    if (sectorState.compareAndSet(i, FALSE, TRUE)) {
                        usedSectorCount0.incrementAndGet()
                        return regionPool.get().init(i)
                    }
                    i += block
                }
                block /= 2
            }
            suspendCoroutine<Unit> {
                notifyQueue.add(it)
            }
        }
    }

    fun destroy() {
        vbo.destroy()
    }

    private fun checkCounter(): Boolean {
        var prev: Int
        do {
            prev = unusedRegionCount0.get()
            if (prev == 0) return false
        } while (!unusedRegionCount0.compareAndSet(prev, prev - 1))
        return true
    }

    private fun release(region: Region) {
        for (i in region.sectorOffset until region.sectorEnd) {
            sectorState.set(i, FALSE)
            usedSectorCount0.decrementAndGet()
        }
        regionPool.put(region)
        unusedRegionCount0.incrementAndGet()
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append('[')
        for (i in 0 until sectorCapacity) {
            stringBuilder.append(sectorState.get(i))
            if (i != sectorCapacity - 1) {
                stringBuilder.append(',')
            }
        }
        stringBuilder.append(']')
        return stringBuilder.toString()
    }

    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    inner class Region internal constructor() {
        val vboID get() = vbo.id

        var buffer = baseBuffer.duplicate().order(ByteOrder.nativeOrder())!!; private set
        private var swapBuffer = baseBuffer.duplicate().order(ByteOrder.nativeOrder())!!

        var sectorOffset = -1; internal set
        var sectorLength = -1; internal set
        val sectorEnd get() = sectorOffset + sectorLength

        val offset get() = sectorOffset * sectorSize
        val length get() = sectorLength * sectorSize

        fun init(start: Int): Region {
            this.sectorOffset = start
            sectorLength = 1
            buffer.address = baseAddress + offset
            buffer.capacity = length
            buffer.clear()
            return this
        }

        fun expand(task: ChunkBuilderTask) {
            while (true) {
                if (sectorEnd < sectorCapacity) {
                    if (sectorState.compareAndSet(sectorEnd, FALSE, TRUE)) {
                        usedSectorCount0.incrementAndGet()
                        sectorLength++
                        buffer.capacity = length
                        buffer.limit(buffer.capacity)
                        return
                    }
                }

                var block = sectorCapacity / 2
                while (block > 0) {
                    var i = block
                    while (i < sectorCapacity - sectorLength) {
                        run label@{
                            for (i1 in i until i + sectorLength + 1) {
                                if (sectorState.get(i1) == TRUE) {
                                    return@label
                                }
                            }

                            var allocated = 0
                            while (true) {
                                if (sectorState.compareAndSet(i + allocated, FALSE, TRUE)) {
                                    allocated++
                                    if (allocated > sectorLength) {
                                        swapBuffer.address = baseAddress + i * sectorSize
                                        swapBuffer.capacity = allocated * sectorSize
                                        swapBuffer.clear()

                                        buffer.flip()
                                        swapBuffer.put(buffer)

                                        val swap = swapBuffer
                                        swapBuffer = buffer
                                        buffer = swap

                                        for (i1 in sectorOffset until sectorEnd) {
                                            sectorState.set(i1, FALSE)
                                        }

                                        sectorOffset = i
                                        sectorLength = allocated
                                        usedSectorCount0.incrementAndGet()

                                        return
                                    }
                                } else if (allocated != 0) {
                                    for (i1 in i until i + allocated) {
                                        sectorState.set(i1, FALSE)
                                    }
                                    doNotify()
                                    return@label
                                }
                            }
                        }

                        i += block
                    }
                    block /= 2
                }

                if (lock.withLock { condition.awaitNanos(5_000_000L) } <= 0) {
                    task.checkCancelled()
                }
            }
        }

        fun release() {
            pendingRelease.add(this)
        }
    }

    private inner class ReleaseTask {
        val list = FastObjectArrayList<Region>()
        private val sync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0)

        fun tryRelease(): Boolean {
            return if (glGetSynciv(sync, GL_SYNC_STATUS) == GL_SIGNALED) {
                glDeleteSync(sync)
                for (i in list.indices) {
                    release(list[i])
                }
                true
            } else {
                false
            }
        }
    }

    private companion object {
        const val FALSE: Byte = 0
        const val TRUE: Byte = 1

        @JvmField
        val UNSAFE = run {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            field.get(null) as Unsafe
        }

        @JvmField
        val ADDRESS_OFFSET: Long

        @JvmField
        val CAPACITY_OFFSET: Long

        init {
            val bufferClass = Buffer::class.java
            ADDRESS_OFFSET = UNSAFE.objectFieldOffset(bufferClass.getDeclaredField("address"))
            CAPACITY_OFFSET = UNSAFE.objectFieldOffset(bufferClass.getDeclaredField("capacity"))
        }

        var ByteBuffer.address
            get() = UNSAFE.getLong(this, ADDRESS_OFFSET)
            set(value) {
                UNSAFE.putLong(this, ADDRESS_OFFSET, value)
            }

        var ByteBuffer.capacity
            get() = capacity()
            set(value) {
                UNSAFE.putInt(this, CAPACITY_OFFSET, value)
            }
    }
}