package me.luna.fastmc.shared.opengl

import me.luna.fastmc.shared.util.collection.FastObjectArrayList

class VertexArrayObject : IGLObject {
    override val id: Int = glCreateVertexArrays()

    private var ibo: IndexBufferObject? = null
    private val vboList = FastObjectArrayList<VertexBufferObject>()
    private var vboBinding = 0

    fun attachIbo(ibo: IndexBufferObject) {
        glVertexArrayElementBuffer(id, ibo.id)
        this.ibo = ibo
    }

    fun attachVbo(vbo: VertexBufferObject) {
        vboList.add(vbo)
        glVertexArrayVertexBuffer(id, vboBinding, vbo.id, 0, vbo.vertexAttribute.stride)
        vbo.vertexAttribute.apply(this, vboBinding++)
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