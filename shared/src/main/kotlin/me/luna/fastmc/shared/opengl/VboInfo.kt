package me.luna.fastmc.shared.opengl

data class VboInfo(
    @JvmField val vbo: VertexBufferObject,
    @JvmField val vertexCount: Int,
    @JvmField val vertexSize: Int,
    @JvmField val vboSize: Int
) {
    inline fun updateVbo(newVboSize: Int, newBuffer: (Int) -> VertexBufferObject): VertexBufferObject {
        return if (newVboSize == vboSize) {
            // Invalidate and return previous vbo if resize is NOT needed
            vbo.invalidate()
            vbo
        } else {
            // Reallocate a new vbo is resize IS needed
            vbo.destroy()
            newBuffer.invoke(newVboSize)
        }
    }
}