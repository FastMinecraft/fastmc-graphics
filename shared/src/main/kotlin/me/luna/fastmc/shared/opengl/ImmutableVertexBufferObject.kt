package me.luna.fastmc.shared.opengl

import me.luna.fastmc.shared.opengl.impl.VertexAttribute

class ImmutableVertexBufferObject(vertexAttribute: VertexAttribute, val size: Int, flags: Int) :
    VertexBufferObject(vertexAttribute) {
    init {
        glNamedBufferStorage(id, size.toLong(), flags)
    }
}