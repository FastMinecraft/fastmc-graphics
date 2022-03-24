package me.luna.fastmc.terrain

import me.luna.fastmc.mixin.IPatchedRenderLayer
import me.luna.fastmc.shared.opengl.GLDataType
import me.luna.fastmc.shared.opengl.VertexBufferObject
import me.luna.fastmc.shared.opengl.buildAttribute
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.math.BlockPos

class RenderRegion(val index: Int) {
    private val origin0 = BlockPos.Mutable()
    private val vboArray = arrayOfNulls<RenderInfo>(RenderLayer.getBlockLayers().size)
    val origin: BlockPos get() = origin0
    var dirty = true

    fun getRenderInfo(layer: RenderLayer): RenderInfo {
        return getRenderInfo((layer as IPatchedRenderLayer).index)
    }

    fun getRenderInfo(index: Int): RenderInfo {
        var renderInfo = vboArray[index]
        if (renderInfo == null) {
            renderInfo = RenderInfo(0, VertexBufferObject(VERTEX_ATTRIBUTE))
            vboArray[index] = renderInfo
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
        for (i in vboArray.indices) {
            vboArray[i]?.vbo?.destroy()
            vboArray[i] = null
        }
    }

    class RenderInfo(var vertexCount: Int, val vbo: VertexBufferObject)

    companion object {
        @JvmField
        val VERTEX_ATTRIBUTE = buildAttribute(28) {
            float(0, 3, GLDataType.GL_FLOAT, false)
            float(1, 4, GLDataType.GL_UNSIGNED_BYTE, true)
            float(2, 2, GLDataType.GL_FLOAT, false)
            float(3, 2, GLDataType.GL_SHORT, true)
        }
    }
}