package me.luna.fastmc.shared.opengl

open class VertexBufferObject(val vertexAttribute: VertexAttribute) : BufferObject() {
    override fun bind() {
        glBindBuffer(GL_ARRAY_BUFFER, id)
    }

    override fun unbind() {
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}