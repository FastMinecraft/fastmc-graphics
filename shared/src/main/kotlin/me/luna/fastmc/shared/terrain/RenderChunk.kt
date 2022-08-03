package me.luna.fastmc.shared.terrain

import me.luna.fastmc.shared.opengl.impl.RenderBufferPool
import me.luna.fastmc.shared.instancing.tileentity.info.ITileEntityInfo
import me.luna.fastmc.shared.renderer.cameraChunkX
import me.luna.fastmc.shared.renderer.cameraChunkY
import me.luna.fastmc.shared.renderer.cameraChunkZ
import me.luna.fastmc.shared.util.Cancellable
import me.luna.fastmc.shared.util.Direction
import me.luna.fastmc.shared.util.UpdateCounter
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.shared.util.distanceSq
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

    fun updateAdjacentChunk() {
        for (i in Direction.VALUES.indices) {
            val direction = Direction.VALUES[i]
            var other = renderer.chunkStorage.getRenderChunkByChunk(
                chunkX + direction.offsetX,
                chunkY + direction.offsetY,
                chunkZ + direction.offsetZ
            )

            if (other != null
                && ((other.chunkX - chunkX) xor direction.offsetX)
                or ((other.chunkY - chunkY) xor direction.offsetY)
                or ((other.chunkZ - chunkZ) xor direction.offsetZ) != 0
            ) {
                other = null
            }

            adjacentRenderChunk[i] = other
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
            return frustum.testAab(x, y, z, x + 16.0f, y + 16.0f, z + 16.0f)
        }
    }

    companion object {
        @JvmStatic
        fun unsynchronizedComparator(): Comparator<RenderChunk> {
            return UnsynchronizedComparator
        }

        @JvmStatic
        fun synchronizedComparator(renderer: TerrainRenderer): Comparator<RenderChunk> {
            return SynchronizedComparator(renderer)
        }

        private object UnsynchronizedComparator : Comparator<RenderChunk> {
            override fun compare(o1: RenderChunk, o2: RenderChunk): Int {
                val visible1 = o1.frustumCull.isInFrustum()
                val visible2 = o2.frustumCull.isInFrustum()

                if (visible1 != visible2) {
                    return if (visible1) 1 else -1
                }

                return -distanceSq(
                    o1.renderer.cameraChunkX, o1.renderer.cameraChunkY, o1.renderer.cameraChunkZ,
                    o1.chunkX, o1.chunkY, o1.chunkZ
                ).compareTo(
                    -distanceSq(
                        o2.renderer.cameraChunkX, o2.renderer.cameraChunkY, o2.renderer.cameraChunkZ,
                        o2.chunkX, o2.chunkY, o2.chunkZ
                    )
                )
            }
        }

        private class SynchronizedComparator(terrainRenderer: TerrainRenderer) : Comparator<RenderChunk> {
            private val frustum = terrainRenderer.frustum
            private val matrixHash = terrainRenderer.matrixHash

            private val cameraChunkX = terrainRenderer.cameraChunkX
            private val cameraChunkY = terrainRenderer.cameraChunkY
            private val cameraChunkZ = terrainRenderer.cameraChunkZ

            override fun compare(o1: RenderChunk, o2: RenderChunk): Int {
                val visible1 = o1.frustumCull.isInFrustum(frustum, matrixHash)
                val visible2 = o2.frustumCull.isInFrustum(frustum, matrixHash)

                if (visible1 != visible2) {
                    return if (visible1) 1 else -1
                }

                return -distanceSq(
                    cameraChunkX, cameraChunkY, cameraChunkZ,
                    o1.chunkX, o1.chunkY, o1.chunkZ
                ).compareTo(
                    -distanceSq(
                        cameraChunkX, cameraChunkY, cameraChunkZ,
                        o2.chunkX, o2.chunkY, o2.chunkZ
                    )
                )
            }
        }
    }
}