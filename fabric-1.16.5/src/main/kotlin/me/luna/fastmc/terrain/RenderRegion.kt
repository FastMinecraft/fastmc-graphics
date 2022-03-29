package me.luna.fastmc.terrain

import it.unimi.dsi.fastutil.ints.IntArrayList
import kotlinx.coroutines.withContext
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.shared.util.toIntBuffer
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.math.BlockPos
import java.nio.IntBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

@Suppress("NOTHING_TO_INLINE")
class RenderRegion(val index: Int) {
    private val origin0 = BlockPos.Mutable()
    val regionLayerArray = arrayOfNulls<RegionLayer>(RenderLayer.getBlockLayers().size)
    val chunks = ExtendedBitSet()
    val origin: BlockPos get() = origin0
    val dirty = AtomicBoolean(true)

    @get:JvmName("isVisible")
    var visible = false

    fun getRegionLayer(index: Int): RegionLayer? {
        return regionLayerArray[index]
    }

    inline suspend fun updateRegionLayer(
        mainThreadContext: CoroutineContext,
        index: Int,
        dataList: FastObjectArrayList<ChunkVertexData>,
        firstArray: IntArrayList,
        countArray: IntArrayList
    ) {
        val vertexCount = dataList.sumOf {
            it.vboInfo.vertexCount
        }
        val vertexSize = VertexDataTransformer.transformedSize(vertexCount)

        val newVboSize = ((vertexSize + 1048575) shr 20 shl 20) + 2097152
        val maxVboSize = newVboSize + 4194304
        val layer = regionLayerArray[index]
        val vao: VertexArrayObject
        val vbo: ImmutableVertexBufferObject

        withContext(mainThreadContext) {
            if (layer == null) {
                vao = VertexArrayObject()
                vbo = newVbo(newVboSize)
                vao.attachVbo(vbo)
            } else {
                vbo = layer.vboInfo.updateVbo(vertexSize, newVboSize, maxVboSize, Companion::newVbo)
                if (vbo !== layer.vboInfo.vbo) {
                    layer.vao.destroyVao()
                    vao = VertexArrayObject()
                    vao.attachVbo(vbo)
                } else {
                    vao = layer.vao
                }
            }

            var offset = 0L
            for (i in dataList.indices) {
                val data = dataList[i]
                glCopyNamedBufferSubData(
                    data.vboInfo.vbo.id,
                    vbo.id,
                    0L,
                    offset,
                    data.vboInfo.vertexSize.toLong()
                )
                offset += data.vboInfo.vertexSize
            }
        }

        regionLayerArray[index] = RegionLayer(
            vao,
            firstArray.toIntBuffer(),
            countArray.toIntBuffer(),
            VboInfo(vbo, vertexCount, vertexSize),
            dataList
        )
    }

    fun updateRegionLayerVisibility(index: Int, firstArray: IntArrayList, countArray: IntArrayList) {
        regionLayerArray[index]?.let {
            regionLayerArray[index] =
                it.copy(firstArray = firstArray.toIntBuffer(), countArray = countArray.toIntBuffer())
        }
    }

    fun setOrigin(x: Int, z: Int) {
        if (x != origin0.x || z != origin0.z) {
            origin0.set(x, 0, z)
            dirty.set(true)
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
        @JvmField val firstArray: IntBuffer,
        @JvmField val countArray: IntBuffer,
        @JvmField val vboInfo: VboInfo,
        @JvmField val dataList: FastObjectArrayList<ChunkVertexData>
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
        fun newVbo(newVboSize: Int): ImmutableVertexBufferObject {
            return ImmutableVertexBufferObject(VERTEX_ATTRIBUTE, newVboSize).apply {
                glNamedBufferStorage(id, newVboSize.toLong(), GL_DYNAMIC_STORAGE_BIT)
            }
        }
    }
}