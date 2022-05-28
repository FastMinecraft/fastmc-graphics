package me.luna.fastmc.shared.opengl.impl

import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.util.ObjectPool
import me.luna.fastmc.shared.util.pollEach
import java.util.concurrent.ConcurrentLinkedQueue

class RenderBufferPool(private val vertexAttribute: VertexAttribute, private val growPower: Int) {
    private val growAmount = 1 shl growPower
    private val regionPool = ObjectPool { Region() }

    var vbo = VertexBufferObject(vertexAttribute); private set

    var capacity = 0
    var allocated = 0; private set
    val unused get() = capacity - allocated
    val isEmpty get() = allocated == 0

    private var regionHead = Region().apply {
        length = 0
    }
    private var regionTail = regionHead
    private var unusedHead: Region? = regionHead
    private var unusedTail: Region? = regionHead

    private val releaseQueue = ConcurrentLinkedQueue<Region>()

    fun allocate(newLength: Int): Region {
        updateUnusedNodes()
        return allocate0(unusedHead!!, newLength)!!
    }

    fun update() {
        var updated = false

        releaseQueue.pollEach {
            glInvalidateBufferSubData(vbo.id, it.offset.toLong(), it.length.toLong())

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
        vbo.destroy()
    }

    fun ensureCapacity(newLength: Int) {
        if (capacity == 0) {
            val newSize = (newLength + growAmount - 1) shr growPower shl growPower
            glNamedBufferData(vbo.id, newSize.toLong(), GL_DYNAMIC_DRAW)
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
            val newVbo = VertexBufferObject(vertexAttribute)
            glNamedBufferData(newVbo.id, newSize.toLong(), GL_DYNAMIC_DRAW)
            allocated = 0

            current = regionHead
            while (current != null) {
                val prev = current.prev
                val next = current.next

                if (current.used) {
                    glCopyNamedBufferSubData(
                        vbo.id,
                        newVbo.id,
                        current.offset.toLong(),
                        allocated.toLong(),
                        current.length.toLong()
                    )
                    current.offset = allocated
                    allocated += current.length
                } else {
                    linkNodes(prev, next)
                    regionPool.put(current)
                }

                current = next
            }

            vbo.destroy()
            vbo = newVbo
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
            val newVbo = VertexBufferObject(vertexAttribute)
            glNamedBufferData(newVbo.id, newSize.toLong(), GL_DYNAMIC_DRAW)

            glCopyNamedBufferSubData(vbo.id, newVbo.id, 0L, 0L, lastUnused.toLong())
            vbo.destroy()
            vbo = newVbo
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

    private fun allocate0(startRegion: Region, newLength: Int): Region? {
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
        val vboID get() = vbo.id

        var used = false; internal set
        var offset = 0; internal set
        var length = 0; internal set
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

        internal fun slice(newLength: Int): Region? {
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