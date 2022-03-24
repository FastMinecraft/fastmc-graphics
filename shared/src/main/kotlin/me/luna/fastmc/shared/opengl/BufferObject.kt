package me.luna.fastmc.shared.opengl

abstract class BufferObject : IGLObject {
    override val id: Int = glCreateBuffers()

    fun invalidate() {
        glInvalidateBufferData(id)
    }

    override fun destroy() {
        glDeleteBuffers(id)
    }
}