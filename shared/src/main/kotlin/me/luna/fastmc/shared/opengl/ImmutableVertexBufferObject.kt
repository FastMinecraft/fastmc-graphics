package me.luna.fastmc.shared.opengl

class ImmutableVertexBufferObject(vertexAttribute: VertexAttribute, val size: Int, flags: Int) :
    VertexBufferObject(vertexAttribute) {
    init {
        glNamedBufferStorage(id, size.toLong(), flags)
    }
}