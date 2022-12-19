package dev.fastmc.graphics.shared.opengl

import java.nio.ByteBuffer

sealed class BufferObject : IGLObject, IGLTargetBinding {
    override val id = glCreateBuffers()

    var size = 0; private set

    open fun allocate(size: Int, flags: Int): BufferObject {
        this.size = size
        return this
    }

    open fun allocate(buffer: ByteBuffer, flags: Int): BufferObject {
        size = buffer.remaining()
        return this
    }

    fun invalidate() {
        glInvalidateBufferData(id)
    }

    override fun destroy() {
        glDeleteBuffers(id)
    }

    override fun bind(target: Int) {
        glBindBuffer(target, id)
    }

    override fun unbind(target: Int) {
        glBindBuffer(target, 0)
    }

    class Immutable : BufferObject() {
        override fun allocate(size: Int, flags: Int): Immutable {
            super.allocate(size, flags)
            glNamedBufferStorage(id, size.toLong(), flags)
            return this
        }

        override fun allocate(buffer: ByteBuffer, flags: Int): Immutable {
            super.allocate(buffer, flags)
            glNamedBufferStorage(id, buffer, flags)
            return this
        }
    }

    class Mutable : BufferObject() {
        override fun allocate(size: Int, flags: Int): Mutable {
            super.allocate(size, flags)
            glNamedBufferData(id, size.toLong(), flags)
            return this
        }

        override fun allocate(buffer: ByteBuffer, flags: Int): Mutable {
            super.allocate(buffer, flags)
            glNamedBufferData(id, buffer, flags)
            return this
        }
    }
}