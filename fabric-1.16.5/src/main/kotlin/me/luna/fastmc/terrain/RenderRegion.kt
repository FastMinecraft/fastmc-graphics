package me.luna.fastmc.terrain

import me.luna.fastmc.shared.opengl.*
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.math.BlockPos

class RenderRegion(val index: Int) {
    private val origin0 = BlockPos.Mutable()
    val regionLayerArray = arrayOfNulls<RegionLayer>(RenderLayer.getBlockLayers().size)
    val origin: BlockPos get() = origin0
    var dirty = true

    @get:JvmName("isVisible")
    var visible = false

    fun getRegionLayer(index: Int): RegionLayer? {
        return regionLayerArray[index]
    }

    inline fun updateRegionLayer(index: Int, newVboSize: Int, block: (VertexBufferObject) -> VboInfo) {
        val layer = regionLayerArray[index]
        val vao: VertexArrayObject
        val vbo: VertexBufferObject

        if (layer == null) {
            vao = VertexArrayObject()
            vbo = newVbo(newVboSize)
            vao.attachVbo(vbo)
        } else {
            vbo = layer.vboInfo.updateVbo(newVboSize, Companion::newVbo)
            if (vbo !== layer.vboInfo.vbo) {
                layer.vao.destroyVao()
                vao = VertexArrayObject()
                vao.attachVbo(vbo)
            } else {
                vao = layer.vao
            }
        }

        regionLayerArray[index] = RegionLayer(vao, block.invoke(vbo))
    }

    fun setOrigin(x: Int, z: Int) {
        if (x != origin0.x || z != origin0.z) {
            clear()
            origin0.set(x, 0, z)
            dirty = true
        }
    }

    fun clear() {
        for (i in regionLayerArray.indices) {
            regionLayerArray[i]?.vao?.destroy()
            regionLayerArray[i] = null
        }
    }

    data class RegionLayer(
        @JvmField val vao: VertexArrayObject,
        @JvmField val vboInfo: VboInfo
    )

    companion object {
        @JvmField
        val VERTEX_ATTRIBUTE = buildAttribute(24) {
            float(0, 3, GLDataType.GL_FLOAT, false)
            float(1, 4, GLDataType.GL_UNSIGNED_BYTE, true)
            float(2, 2, GLDataType.GL_UNSIGNED_SHORT, true)
            float(3, 2, GLDataType.GL_UNSIGNED_BYTE, true)
        }

        @JvmStatic
        fun newVbo(newVboSize: Int): VertexBufferObject {
            return VertexBufferObject(VERTEX_ATTRIBUTE).apply {
                glNamedBufferStorage(id, newVboSize.toLong(), GL_DYNAMIC_STORAGE_BIT)
            }
        }
    }
}