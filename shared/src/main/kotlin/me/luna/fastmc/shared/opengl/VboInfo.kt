package me.luna.fastmc.shared.opengl

data class VboInfo(
    @JvmField val vbo: ImmutableVertexBufferObject,
    @JvmField val vertexCount: Int,
    @JvmField val vertexSize: Int
) {
    inline fun updateVbo(minVboSize: Int, newVboSize: Int, maxVboSize: Int, newBuffer: (Int) -> ImmutableVertexBufferObject): ImmutableVertexBufferObject {
        return if (vbo.size in minVboSize..maxVboSize) {
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