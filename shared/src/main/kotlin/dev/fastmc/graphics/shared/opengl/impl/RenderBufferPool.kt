package dev.fastmc.graphics.shared.opengl.impl

import dev.fastmc.common.ObjectPool
import dev.fastmc.common.pollEach
import dev.luna5ama.glwrapper.api.GL_DYNAMIC_DRAW
import dev.luna5ama.glwrapper.api.glCopyNamedBufferSubData
import dev.luna5ama.glwrapper.api.glInvalidateBufferSubData
import dev.luna5ama.glwrapper.impl.BufferObject
import java.util.concurrent.ConcurrentLinkedQueue

class RenderBufferPool(private val growPower: Int) {
    private val growAmount = 1 shl growPower
    private val regionPool = ObjectPool { Region() }

    var bufferObject = BufferObject.Mutable(); private set

    var capacity = 0L
    var allocated = 0L; private set
    val unused get() = capacity - allocated
    val isEmpty get() = allocated == 0L

    private var regionHead = Region().apply {
        length = 0
    }
    private var regionTail = regionHead
    private var unusedHead: Region? = regionHead
    private var unusedTail: Region? = regionHead

    private val releaseQueue = ConcurrentLinkedQueue<Region>()

    fun allocate(newLength: Long): Region {
        updateUnusedNodes()
        ensureCapacity(newLength)
        return allocate0(unusedHead!!, newLength)!!
    }

    fun update() {
        var updated = false

        releaseQueue.pollEach {
            it.invalidate()

            it.used = false
            allocated -= it.length

            val prev = it.prev
            val next = it.next

            if (prev != null && !prev.used) {
                it.offset = prev.offset
                it.length += prev.length
                linkNodes(prev.prev, it)
                regionPool.put(prev)
            }

            if (next != null && !next.used) {
                it.length += next.length
                linkNodes(it, next.next)
                regionPool.put(next)
            }

            updated = true
        }

        if (updated) {
            updateUnusedNodes()
        }
    }

    fun destroy() {
        bufferObject.destroy()
    }

    fun ensureCapacity(newLength: Long) {
        if (capacity == 0L) {
            val newSize = (newLength + growAmount - 1) shr growPower shl growPower
            bufferObject.allocate(newSize, GL_DYNAMIC_DRAW)
            capacity = newSize
            regionHead.length = newSize
            return
        }

        var current: Region? = unusedHead
        var space = 0L
        while (current != null) {
            if (current.length >= newLength) {
                return
            }
            space += current.length
            current = current.nextUnused
        }

        if (unused > growAmount * 2) {
            val diff = newLength - unused
            val newSize = if (diff > 0) {
                (capacity + diff + growAmount - 1) shr growPower shl growPower
            } else {
                capacity
            }
            val newBufferObject = BufferObject.Mutable().allocate(newSize, GL_DYNAMIC_DRAW)
            allocated = 0

            current = regionHead
            while (current != null) {
                val prev = current.prev
                val next = current.next

                if (current.used) {
                    glCopyNamedBufferSubData(
                        bufferObject.id,
                        newBufferObject.id,
                        current.offset,
                        allocated,
                        current.length
                    )
                    current.offset = allocated
                    allocated += current.length
                } else {
                    linkNodes(prev, next)
                    regionPool.put(current)
                }

                current = next
            }

            bufferObject.destroy()
            bufferObject = newBufferObject
            capacity = newSize

            val newTail = regionPool.get().init()
            newTail.offset = regionTail.end
            newTail.length = capacity - newTail.offset
            linkNodes(regionTail, newTail)
            linkNodes(newTail, null)
        } else {
            val tail = regionTail
            val lastUnused = if (!tail.used) tail.offset else capacity
            val newSize = (lastUnused + newLength + growAmount - 1) shr growPower shl growPower
            val newBufferObject = BufferObject.Mutable().allocate(newSize, GL_DYNAMIC_DRAW)

            glCopyNamedBufferSubData(bufferObject.id, newBufferObject.id, 0L, 0L, lastUnused)
            bufferObject.destroy()
            bufferObject = newBufferObject
            capacity = newSize

            val diff = newSize - tail.end
            if (tail.used) {
                val newRegion = regionPool.get().init()
                newRegion.offset = tail.end
                newRegion.length = diff
                linkNodes(tail, newRegion)
                linkNodes(newRegion, null)
            } else {
                tail.length += diff
            }
        }

        updateUnusedNodes()
    }

    private fun allocate0(startRegion: Region, newLength: Long): Region? {
        var current: Region? = startRegion
        while (current != null) {
            if (current.length >= newLength) {
                current.used = true
                current.slice(newLength)
                allocated += current.length
                updateUnusedNodes()
                return current
            }
            current = current.nextUnused
        }
        return null
    }

    private fun linkNodes(from: Region?, to: Region?) {
        if (from != null) {
            from.next = to
            if (to == null) {
                regionTail = from
            }
        }

        if (to != null) {
            to.prev = from
            if (from == null) {
                regionHead = to
            }
        }
    }

    private fun updateUnusedNodes() {
        var prev: Region? = null
        var current: Region? = regionHead
        while (current != null) {
            if (current.used) {
                current.prevUnused = null
                current.nextUnused = null
            } else {
                linkNodesUnused(prev, current)
                prev = current
            }
            val next = current.next
            if (next != null && (next.offset <= current.offset || current.end != next.offset)) {
                error("WTF")
            }
            current = current.next
        }
        linkNodesUnused(prev, null)

        val head = unusedHead
        if (head != null && head.used) {
            unusedHead = null
        }

        val tail = unusedTail
        if (tail != null && tail.used) {
            unusedTail = null
        }
    }

    private fun linkNodesUnused(from: Region?, to: Region?) {
        if (from != null) {
            from.nextUnused = to
            if (to == null) {
                unusedTail = from
            }
        }

        if (to != null) {
            to.prevUnused = from
            if (from == null) {
                unusedHead = to
            }
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append('[')
        var i: Region? = regionHead
        while (i != null) {
            stringBuilder.append(i)
            if (i.next != null) {
                stringBuilder.append(", ")
            }
            i = i.next
        }
        stringBuilder.append(']')
        return stringBuilder.toString()
    }

    inner class Region {
        val bufferObjectID get() = bufferObject.id

        var used = false; internal set
        var offset = 0L; internal set
        var length = 0L; internal set
        val end get() = offset + length

        var prev: Region? = null; internal set
        var next: Region? = null; internal set

        var prevUnused: Region? = null; internal set
        var nextUnused: Region? = null; internal set

        internal fun init(): Region {
            used = false
            offset = 0
            length = 0

            prev = null
            next = null
            prevUnused = null
            nextUnused = null

            return this
        }

        internal fun slice(newLength: Long): Region? {
            if (newLength == this.length) {
                return null
            }

            val remain = this.length - newLength
            this.length = newLength
            val newRegion = regionPool.get().init()
            newRegion.offset = this.end
            newRegion.length = remain
            linkNodes(newRegion, this.next)
            linkNodes(this, newRegion)

            return newRegion
        }

        fun invalidate() {
            glInvalidateBufferSubData(bufferObjectID, offset, length)
        }

        fun release() {
            releaseQueue.add(this)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Region

            if (offset != other.offset) return false

            return true
        }

        override fun hashCode(): Int {
            return offset.hashCode()
        }

        override fun toString(): String {
            return "[$offset..$end, $length, $used]"
        }
    }
}