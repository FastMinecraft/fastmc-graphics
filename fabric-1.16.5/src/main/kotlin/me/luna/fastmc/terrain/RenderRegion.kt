package me.luna.fastmc.terrain

import me.luna.fastmc.shared.opengl.GLDataType
import me.luna.fastmc.shared.opengl.VertexArrayObject
import me.luna.fastmc.shared.opengl.VertexBufferObject
import me.luna.fastmc.shared.opengl.buildAttribute
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.math.BlockPos

class RenderRegion(val index: Int) {
    private val origin0 = BlockPos.Mutable()
    val renderInfoArray = arrayOfNulls<RenderInfo>(RenderLayer.getBlockLayers().size)
    val origin: BlockPos get() = origin0
    var dirty = true


    @get:JvmName("isVisible")
    var visible = false

    fun getRenderInfo(index: Int): RenderInfo? {
        return renderInfoArray[index]
    }

    fun getInitRenderInfo(index: Int): RenderInfo {
        var renderInfo = renderInfoArray[index]
        if (renderInfo == null) {
            val vao = VertexArrayObject()
            val vbo = VertexBufferObject(VERTEX_ATTRIBUTE)
            vao.attachVbo(vbo)
            renderInfo = RenderInfo(0, 0, 0, vao, vbo)
            renderInfoArray[index] = renderInfo
        }
        return renderInfo
    }

    fun setOrigin(x: Int, z: Int) {
        if (x != origin0.x || z != origin0.z) {
            clear()
            origin0.set(x, 0, z)
            dirty = true
        }
    }

    fun clear() {
        for (i in renderInfoArray.indices) {
            renderInfoArray[i]?.vao?.destroy()
            renderInfoArray[i] = null
        }
    }

    class RenderInfo(
        chunksCount: Int,
        vertexCount: Int,
        vertexSize: Int,
        val vao: VertexArrayObject,
        val vbo: VertexBufferObject
    ) {
        var chunksCount = chunksCount; private set
        var vertexCount = vertexCount; private set
        var vertexSize = vertexSize; private set

        fun update(chunksCount: Int, vertexCount: Int, vertexSize: Int) {
            this.chunksCount = chunksCount
            this.vertexCount = vertexCount
            this.vertexSize = vertexSize
        }
    }

    companion object {
        @JvmField
        val VERTEX_ATTRIBUTE = buildAttribute(24) {
            float(0, 3, GLDataType.GL_FLOAT, false)
            float(1, 4, GLDataType.GL_UNSIGNED_BYTE, true)
            float(2, 2, GLDataType.GL_UNSIGNED_SHORT, true)
            float(3, 2, GLDataType.GL_UNSIGNED_BYTE, true)
        }
    }
}