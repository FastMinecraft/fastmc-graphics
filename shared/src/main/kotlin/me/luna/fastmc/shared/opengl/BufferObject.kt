package me.luna.fastmc.shared.opengl

import java.nio.ByteBuffer

sealed class BufferObject : IGLObject, IGLTargetBinding {
    override val id = glCreateBuffers()

    var size = 0; private set

    open fun allocate(size: Int, flags: Int) {
        this.size = size
    }

    open fun allocate(buffer: ByteBuffer, flags: Int) {
        size = buffer.remaining()
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
        override fun allocate(size: Int, flags: Int) {
            super.allocate(size, flags)
            glNamedBufferStorage(id, size.toLong(), flags)
        }

        override fun allocate(buffer: ByteBuffer, flags: Int) {
            super.allocate(buffer, flags)
            glNamedBufferStorage(id, buffer, flags)
        }
    }

    class Mutable : BufferObject() {
        override fun allocate(size: Int, flags: Int) {
            super.allocate(size, flags)
            glNamedBufferData(id, size.toLong(), flags)
        }

        override fun allocate(buffer: ByteBuffer, flags: Int) {
            super.allocate(buffer, flags)
            glNamedBufferData(id, buffer, flags)
        }
    }
}