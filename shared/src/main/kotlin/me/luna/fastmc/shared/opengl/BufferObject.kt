package me.luna.fastmc.shared.opengl

import java.nio.ByteBuffer

sealed class BufferObject(val target: Target) : IGLObject {
    override val id = glCreateBuffers()

    var size = 0; private set

    open fun allocate(size: Int, flags: Int) {
        this.size = size
    }

    open fun allocate(buffer: ByteBuffer, flags: Int) {
        size = buffer.remaining()
    }

    override fun bind() {
        target.bind(id)
    }

    override fun unbind() {
        target.unbind()
    }

    fun invalidate() {
        glInvalidateBufferData(id)
    }

    override fun destroy() {
        glDeleteBuffers(id)
    }

    class Immutable(target: Target) : BufferObject(target) {
        override fun allocate(size: Int, flags: Int) {
            super.allocate(size, flags)
            glNamedBufferStorage(id, size.toLong(), flags)
        }

        override fun allocate(buffer: ByteBuffer, flags: Int) {
            super.allocate(buffer, flags)
            glNamedBufferStorage(id, buffer, flags)
        }
    }

    class Mutable(target: Target) : BufferObject(target) {
        override fun allocate(size: Int, flags: Int) {
            super.allocate(size, flags)
            glNamedBufferData(id, size.toLong(), flags)
        }

        override fun allocate(buffer: ByteBuffer, flags: Int) {
            super.allocate(buffer, flags)
            glNamedBufferData(id, buffer, flags)
        }
    }

    enum class Target(val glEnum: Int) {
        NONE(0) {
            override fun bind(id: Int) {

            }

            override fun unbind() {

            }
        },
        GL_ARRAY_BUFFER(me.luna.fastmc.shared.opengl.GL_ARRAY_BUFFER),
        GL_ELEMENT_ARRAY_BUFFER(me.luna.fastmc.shared.opengl.GL_ELEMENT_ARRAY_BUFFER),
        GL_UNIFORM_BUFFER(me.luna.fastmc.shared.opengl.GL_UNIFORM_BUFFER),
        GL_DRAW_INDIRECT_BUFFER(me.luna.fastmc.shared.opengl.GL_DRAW_INDIRECT_BUFFER);

        open fun bind(id: Int) {
            glBindBuffer(glEnum, id)
        }

        open fun unbind() {
            glBindBuffer(glEnum, 0)
        }
    }
}