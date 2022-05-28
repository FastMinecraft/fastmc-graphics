package me.luna.fastmc.shared.opengl

class UniformBufferObject(val blockName: String, val size: Int, val flags: Int) : BufferObject() {
    init {
        glNamedBufferStorage(id, size.toLong(), flags)
    }

    override fun bind() {
        glBindBuffer(GL_UNIFORM_BUFFER, id)
    }

    override fun unbind() {
        glBindBuffer(GL_UNIFORM_BUFFER, 0)
    }
}