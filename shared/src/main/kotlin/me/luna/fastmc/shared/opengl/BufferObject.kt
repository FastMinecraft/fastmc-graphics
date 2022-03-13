package me.luna.fastmc.shared.opengl

abstract class BufferObject: IGLObject {
    override val id: Int = glCreateBuffers()

    override fun destroy() {
        glDeleteBuffers(id)
    }
}