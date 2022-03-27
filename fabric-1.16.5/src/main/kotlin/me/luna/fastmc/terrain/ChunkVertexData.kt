package me.luna.fastmc.terrain

import me.luna.fastmc.shared.opengl.*

class ChunkVertexData(
    @JvmField val chunkIndex: Int,
    @JvmField val builtOrigin: Long,
    @JvmField val vboInfo: VboInfo
) {
    fun updateVbo(minVboSize: Int, newVboSize: Int, maxVboSize: Int): ImmutableVertexBufferObject {
        return vboInfo.updateVbo(minVboSize, newVboSize, maxVboSize, Companion::newVbo)
    }

    companion object {
        @JvmStatic
        fun newVbo(newVboSize: Int): ImmutableVertexBufferObject {
            return ImmutableVertexBufferObject(RenderRegion.VERTEX_ATTRIBUTE, newVboSize).apply {
                glNamedBufferStorage(id, newVboSize.toLong(), GL_DYNAMIC_STORAGE_BIT)
            }
        }
    }
}