package me.luna.fastmc.shared.opengl

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import me.luna.fastmc.shared.util.collection.FastObjectArrayList

class VertexArrayObject: IGLObject {
    override val id: Int = glCreateVertexArrays()

    private val iboList = FastObjectArrayList<IndexBufferObject>()
    private val vboSet = ObjectOpenHashSet<VertexBufferObject>()
    private var vboBinding = 0

    fun attachIbo(ibo: IndexBufferObject) {
        glVertexArrayElementBuffer(id, ibo.id)
        iboList.add(ibo)
    }

    fun attachVbo(vbo: VertexBufferObject, offset: Long, stride: Int): Int {
        vboSet.add(vbo)
        glVertexArrayVertexBuffer(id, vboBinding, vbo.id, offset, stride)
        return vboBinding++
    }

    override fun bind() {
        glBindVertexArray(id)
    }

    override fun unbind() {
        glBindVertexArray(0)
    }

    override fun destroy() {
        glDeleteVertexArrays(id)
        iboList.forEach {
            it.destroy()
        }
        vboSet.forEach {
            it.destroy()
        }
        iboList.clear()
        vboSet.clear()
    }

    fun destroyVao() {
        glDeleteVertexArrays(id)
    }
}