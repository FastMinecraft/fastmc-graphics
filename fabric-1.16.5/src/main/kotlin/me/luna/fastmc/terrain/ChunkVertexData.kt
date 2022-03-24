package me.luna.fastmc.terrain

import me.luna.fastmc.shared.opengl.*

class ChunkVertexData(
    @JvmField val builtOrigin: Long,
    @JvmField val vboInfo: VboInfo
) {
    fun updateVbo(newVboSize: Int): VertexBufferObject {
        return vboInfo.updateVbo(newVboSize, Companion::newVbo)
    }

    companion object {
        @JvmStatic
        fun newVbo(newVboSize: Int): VertexBufferObject {
            return VertexBufferObject(RenderRegion.VERTEX_ATTRIBUTE).apply {
                glNamedBufferStorage(id, newVboSize.toLong(), GL_DYNAMIC_STORAGE_BIT)
            }
        }
    }
}