package dev.fastmc.graphics.shared.opengl.impl

import dev.fastmc.common.*
import dev.fastmc.common.collection.AtomicByteArray
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.terrain.ChunkBuilderTask
import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.glwrapper.impl.BufferObject
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.Ptr
import dev.luna5ama.kmogus.asMutable
import dev.luna5ama.kmogus.memcpy
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MappedBufferPool(sectorSizePower: Int, private val sectorCapacity: Int, val maxRegions: Int) {
    private val sectorSize = 1L shl sectorSizePower
    val capacity = sectorSize * sectorCapacity

    private val vbo = BufferObject.Immutable()
        .allocate(capacity, GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT)

    private val baseBuffer = glMapNamedBufferRange(
        vbo.id,
        0,
        capacity,
        GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_UNSYNCHRONIZED_BIT
    )
    private val basePtr = baseBuffer.ptr

    private val regionPool = ConcurrentObjectPool { Region() }
    private val sectorState = AtomicByteArray(sectorCapacity)
    private val usedSectorCount0 = AtomicInteger(0)
    private val unusedRegionCount0 = AtomicInteger(maxRegions)

    private val lock = ReentrantLock(false)
    private val condition = lock.newCondition()

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

        var releaseTask: ReleaseTask? = null
        pendingRelease.pollEach {
            if (releaseTask == null) {
                releaseTask = ReleaseTask()
            }
            releaseTask!!.list.add(it)
        }
        if (releaseTask != null) {
            releaseTaskQueue.offerLast(releaseTask!!)
        }
    }

    private fun doNotify() {
        lock.withLock {
            condition.signalAll()
        }
    }

    fun allocate(): Region {
        while (!checkCounter()) {
            //
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

            lock.withLock { condition.await() }
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

    inner class Region internal constructor() {
        val vboID get() = vbo.id

        private val arr0 = InternalArr()
        val arr = arr0.asMutable()

        var sectorOffset = -1; internal set
        var sectorLength = -1; internal set
        val sectorEnd get() = sectorOffset + sectorLength

        val offset get() = sectorOffset * sectorSize
        val length get() = sectorLength * sectorSize

        fun init(start: Int): Region {
            this.sectorOffset = start
            sectorLength = 1
            arr.len = length

            return this
        }

        fun expand(task: ChunkBuilderTask) {
            while (true) {
                if (sectorEnd < sectorCapacity) {
                    if (sectorState.compareAndSet(sectorEnd, FALSE, TRUE)) {
                        usedSectorCount0.incrementAndGet()
                        sectorLength++
                        arr.len = length
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
                                    if (++allocated > sectorLength) {
                                        val prevSectorOffset = sectorOffset
                                        val prevSectorEnd = sectorEnd
                                        val prevPtr = arr.basePtr
                                        val prevLen = arr.len

                                        sectorOffset = i
                                        sectorLength = allocated

                                        memcpy(prevPtr, arr.basePtr, prevLen)

                                        arr.len = length
                                        arr.pos = prevLen

                                        for (i1 in prevSectorOffset until prevSectorEnd) {
                                            sectorState.set(i1, FALSE)
                                        }
                                        doNotify()

                                        usedSectorCount0.incrementAndGet()

                                        return
                                    }
                                } else {
                                    if (allocated != 0) {
                                        for (i1 in i until i + allocated) {
                                            sectorState.set(i1, FALSE)
                                        }
                                        doNotify()
                                    }
                                    break
                                }
                            }
                        }

                        i += block
                    }
                    block /= 2
                }

                task.checkCancelled()
                lock.withLock { condition.awaitNanos(1_000_000) }
            }
        }

        fun release() {
            pendingRelease.add(this)
        }

        private inner class InternalArr : Arr {
            override val len: Long
                get() = length

            override val ptr: Ptr
                get() = basePtr + offset

            override fun free() {
                throw UnsupportedOperationException()
            }

            override fun realloc(newLength: Long, init: Boolean) {
                throw UnsupportedOperationException()
            }
        }
    }

    private inner class ReleaseTask {
        val list = FastObjectArrayList<Region>()
        private val sync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0)

        fun tryRelease(): Boolean {
            return if (glGetSynci(sync, GL_SYNC_STATUS) == GL_SIGNALED) {
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
    }
}