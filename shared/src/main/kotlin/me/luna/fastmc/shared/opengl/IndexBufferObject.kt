package me.luna.fastmc.shared.opengl

class IndexBufferObject : BufferObject() {
    override fun bind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id)
    }

    override fun unbind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }
}