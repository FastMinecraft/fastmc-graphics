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
    @JvmField var renderRegion: RenderRegion,
    @JvmField val index: Int
) : Cancellable {
    private val updateCounter = UpdateCounter()
    private val lastTaskRef = AtomicReference<ChunkBuilderTask>()

    var chunkX = 0; private set
    var chunkY = 0; private set
    var chunkZ = 0; private set

    var localChunkX = 0; private set
    var localChunkY = 0; private set
    var localChunkZ = 0; private set

    inline val originX get() = chunkX shl 4
    inline val originY get() = chunkY shl 4
    inline val originZ get() = chunkZ shl 4

    inline val minX get() = originX
    inline val minY get() = originY
    inline val minZ get() = originZ

    inline val maxX get() = originX + 16
    inline val maxY get() = originY + 16
    inline val maxZ get() = originZ + 16

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

    @JvmField
    var localIndex = 0

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

            localChunkX = chunkX - renderRegion.originChunkX
            localChunkY = chunkY - renderRegion.originChunkY
            localChunkZ = chunkZ - renderRegion.originChunkZ

            localIndex = localChunkY + (localChunkX + localChunkZ * 16) * renderer.chunkStorage.sizeY

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
            val x = (originX - renderer.camera.posX).toFloat()
            val y = (originY - renderer.camera.posY).toFloat()
            val z = (originZ - renderer.camera.posZ).toFloat()
            return frustum.testAab(x, y, z, x + 16.0f, y + 16.0f, z + 16.0f)
        }
    }
}