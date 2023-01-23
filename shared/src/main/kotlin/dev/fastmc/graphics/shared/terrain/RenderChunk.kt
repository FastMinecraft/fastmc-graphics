package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.Cancellable
import dev.fastmc.common.UpdateCounter
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo
import dev.fastmc.graphics.shared.opengl.impl.RenderBufferPool
import org.joml.FrustumIntersection
import java.util.concurrent.atomic.AtomicReference

class RenderChunk(
    private val renderer: TerrainRenderer,
    @JvmField var region: RenderRegion,
    @JvmField val index: Int
) : Cancellable {
    private val updateCounter = UpdateCounter()
    private val lastTaskRef = AtomicReference<ChunkBuilderTask>()

    var chunkX = 0; private set
    var chunkY = 0; private set
    var chunkZ = 0; private set

    inline val originX get() = chunkX shl 4
    inline val originY get() = chunkY shl 4
    inline val originZ get() = chunkZ shl 4

    inline val minX get() = originX + bound.minX
    inline val minY get() = originY + bound.minY
    inline val minZ get() = originZ + bound.minZ

    inline val maxX get() = originX + bound.maxX
    inline val maxY get() = originY + bound.maxY
    inline val maxZ get() = originZ + bound.maxZ

    @JvmField
    var bound: Bound = Bound.Default

    @JvmField
    var regionIndex = 0

    @JvmField
    val frustumCull: FrustumCull = FrustumCullImpl()

    @JvmField
    val adjacentRenderChunk = arrayOfNulls<RenderChunk>(6)

    @JvmField
    var isDirty = true

    @JvmField
    var isEmpty = true

    @JvmField
    var isBuilt = false

    @JvmField
    var isVisible = false

    @Volatile
    @JvmField
    var isDestroyed = false

    @JvmField
    var occlusionData = ChunkOcclusionData.EMPTY

    @JvmField
    var translucentData: TranslucentData? = null

    @JvmField
    val layers = Array(renderer.layerCount) { Layer() }

    @JvmField
    var tileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null

    @JvmField
    var instancingTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null

    @JvmField
    var globalTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null

    override val isCancelled: Boolean
        get() = isDestroyed

    fun checkFogRange(): Boolean {
        return renderer.shaderManager.checkFogRange(originX + 8, originY + 8, originZ + 8)
    }

    fun onTaskStart(task: ChunkBuilderTask) {
        val lastTask = lastTaskRef.getAndSet(task)
        if (task is RebuildTask || task.javaClass == lastTask?.javaClass) lastTask?.cancel()
    }

    fun onTaskFinish(task: ChunkBuilderTask) {
        lastTaskRef.compareAndSet(task, null)
    }

    fun onUpdate() {
        isEmpty = true
        for (layer in layers) {
            if (layer.faceData != null) {
                isEmpty = false
                break
            }
        }
        isBuilt = true
        updateCounter.update()
    }

    fun resetUpdate() {
        updateCounter.reset()
    }

    fun checkUpdate(): Boolean {
        return updateCounter.check()
    }

    fun setPos(chunkX: Int, chunkY: Int, chunkZ: Int) {
        if (chunkX != this.chunkX || chunkY != this.chunkY || chunkZ != this.chunkZ) {
            this.chunkX = chunkX
            this.chunkY = chunkY
            this.chunkZ = chunkZ

            bound = Bound.Default
            regionIndex = (chunkY shl 8) or ((chunkZ and 15) shl 4) or (chunkX and 15)

            lastTaskRef.getAndSet(null)?.cancel()
            occlusionData = ChunkOcclusionData.EMPTY
            translucentData = null

            for (i in layers.indices) {
                val layer = layers[i]
                layer.vertexRegion = null
                layer.indexRegion = null
                layer.faceData = null
            }

            tileEntityList = null
            instancingTileEntityList = null
            globalTileEntityList = null

            isDirty = true
            isEmpty = true
            isBuilt = false
            frustumCull.reset()

            updateCounter.update()
        }
    }

    fun destroy() {
        isDestroyed = true
        lastTaskRef.getAndSet(null)?.cancel()
    }

    class Layer {
        var vertexRegion: RenderBufferPool.Region? = null
            set(value) {
                val temp = field
                if (temp != null && temp !== value) {
                    temp.release()
                }
                field = value
            }
        var indexRegion: RenderBufferPool.Region? = null
            set(value) {
                val temp = field
                if (temp != null && temp !== value) {
                    temp.release()
                }
                field = value
            }
        var faceData: FaceData? = null
    }

    private inner class FrustumCullImpl : FrustumCull(renderer) {
        override fun isInFrustum(frustum: FrustumIntersection): Boolean {
            val x = (originX - renderer.renderPosX).toFloat()
            val y = (originY - renderer.renderPosY).toFloat()
            val z = (originZ - renderer.renderPosZ).toFloat()
            return frustum.testAab(
                x + bound.minX, y + bound.minY, z + bound.minZ,
                x + bound.maxX, y + bound.maxY, z + bound.maxZ
            )
        }
    }

    sealed interface Bound {
        val minX: Float
        val minY: Float
        val minZ: Float
        val maxX: Float
        val maxY: Float
        val maxZ: Float

        object Default : Bound {
            override val minX get() = 0.0f
            override val minY get() = 0.0f
            override val minZ get() = 0.0f
            override val maxX get() = 16.0f
            override val maxY get() = 16.0f
            override val maxZ get() = 16.0f
        }
    }

    data class MutableBound(
        override var minX: Float,
        override var minY: Float,
        override var minZ: Float,
        override var maxX: Float,
        override var maxY: Float,
        override var maxZ: Float
    ) : Bound {
        constructor() : this(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    }
}