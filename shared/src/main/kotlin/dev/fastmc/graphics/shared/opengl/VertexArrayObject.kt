package dev.fastmc.graphics.shared.opengl

import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.opengl.impl.VertexAttribute

class VertexArrayObject : IGLObject, IGLBinding {
    override val id: Int = glCreateVertexArrays()

    private var ibo: BufferObject? = null
    private val vboList = FastObjectArrayList<BufferObject>()
    private var vboBinding = 0

    fun attachIbo(ibo: BufferObject) {
        glVertexArrayElementBuffer(id, ibo.id)
        this.ibo = ibo
    }

    fun attachVbo(vbo: BufferObject, vertexAttribute: VertexAttribute) {
        vboList.add(vbo)
        glVertexArrayVertexBuffer(id, vboBinding, vbo.id, 0, vertexAttribute.stride)
        vertexAttribute.apply(this, vboBinding++)
    }

    override fun bind() {
        glBindVertexArray(id)
    }

    override fun unbind() {
        glBindVertexArray(0)
    }

    override fun destroy() {
        glDeleteVertexArrays(id)
        ibo?.destroy()
        ibo = null
        vboList.forEach {
            it.destroy()
        }
        vboList.clear()
    }

    fun clear() {
        if (ibo != null) {
            glVertexArrayElementBuffer(id, 0)
            ibo = null
        }
        for (i in vboList.indices) {
            glVertexArrayVertexBuffer(id, i, 0, 0, 0)
        }
        vboList.clear()
        vboBinding = 0
    }

    fun destroyVao() {
        glDeleteVertexArrays(id)
    }
}