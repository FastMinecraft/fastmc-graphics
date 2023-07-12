package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.sort.ObjectIntrosort
import dev.fastmc.graphics.FastMcMod
import dev.fastmc.graphics.shared.FpsDisplay
import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo
import dev.fastmc.graphics.shared.renderer.*
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.shared.util.FastMcExtendScope
import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.glwrapper.impl.GLDataType
import dev.luna5ama.glwrapper.impl.buildAttribute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("NOTHING_TO_INLINE")
abstract class TerrainRenderer(
    renderer: WorldRenderer,
    val layerCount: Int
) : IRenderer by renderer {
    abstract val minChunkY: Int
    abstract val maxChunkY: Int

    private var chunkStorageNullable: RenderChunkStorage? = null
    val chunkStorage get() = chunkStorageNullable!!

    fun updateChunkStorage(viewDistance: Int) {
        chunkStorageNullable?.destroy()
        chunkStorageNullable = RenderChunkStorage(this, viewDistance)
    }

    @Suppress("LeakingThis")
    val shaderManager = TerrainShaderManager(this)

    abstract val chunkBuilder: ChunkBuilder
    abstract val contextProvider: ContextProvider

    val renderTileEntityList = DoubleBuffered<FastObjectArrayList<ITileEntityInfo<*>>>(::FastObjectArrayList)
    val globalTileEntityList = DoubleBuffered<FastObjectArrayList<ITileEntityInfo<*>>>(::FastObjectArrayList)

    private var lastSortScheduleTask: Future<*>? = null
    private var lastRebuildScheduleTask: Future<*>? = null

    private var chunkCullingResults = emptyArray<Array<CullingResult>>()
    private var tileEntityResults = emptyArray<TileEntityResult>()

    protected var lastViewDistance = Int.MIN_VALUE

    private var lastCameraX0 = Int.MAX_VALUE
    private var lastCameraY0 = Int.MAX_VALUE
    private var lastCameraZ0 = Int.MAX_VALUE
    private var lastCameraYaw0 = Int.MAX_VALUE
    private var lastCameraPitch0 = Int.MAX_VALUE

    private var lastSortX = Double.MAX_VALUE
    private var lastSortY = Double.MAX_VALUE
    private var lastSortZ = Double.MAX_VALUE
    private var lastSortChunkX = Int.MAX_VALUE
    private var lastSortChunkY = Int.MAX_VALUE
    private var lastSortChunkZ = Int.MAX_VALUE

    private val forceUpdateTimer = TickTimer()
    private val rebuildTimer = TickTimer()
    private var lastMatrixHash = 0L

    private var updateCount = 0
    private var updateTimer = TickTimer()

    private var lastDebugUpdateTask: Future<*>? = null
    var debugInfoString = ""; private set

    abstract val viewDistance: Int
    abstract val isDebugEnabled: Boolean
    abstract val caveCulling: Boolean

    private val chunkLoadingStatusCacheTimer = TickTimer()
    protected abstract val chunkLoadingStatusCache: ChunkLoadingStatusCache<*>

    abstract fun update(uploadChunks: Boolean)

    protected fun update0(uploadChunks: Boolean) {
        if (isDebugEnabled) {
            updateDebugInfo()
        }

        val chunkStorage = chunkStorage
        val chunkBuilder = chunkBuilder

        val uploadChunkJob = if (uploadChunks) {
            CompletableDeferred()
        } else {
            CompletableDeferred(Unit)
        }

        val updateRegion = AtomicBoolean(false)
        val semaphore = CountDownLatch(1)

        FastMcCoreScope.launch {
            coroutineScope {
                FastMcMod.profiler.swap("checkUpdate")
                val cameraPosX0 = camera.blockX shr 1
                val cameraPosY0 = camera.blockY shr 1
                val cameraPosZ0 = camera.blockZ shr 1
                val cameraYaw0 = camera.yaw.floorToInt()
                val cameraPitch0 = camera.pitch.floorToInt()

                val caveCullingUpdate = chunkStorage.checkCaveCullingUpdate()
                val indicesUpdate = chunkStorage.checkChunkIndicesUpdate()
                val viewUpdate =
                    cameraPosX0 != lastCameraX0 || cameraPosY0 != lastCameraY0 || cameraPosZ0 != lastCameraZ0
                        || cameraYaw0 != lastCameraYaw0 || cameraPitch0 != lastCameraPitch0
                val frustumUpdate = lastMatrixHash != camera.matrixHash
                val forceUpdate = forceUpdateTimer.tickAndReset(1000L) || !chunkStorage.cameraChunk.isBuilt

                if (forceUpdate) {
                    chunkLoadingStatusCacheTimer.reset(-500)
                }

                val updateChunk = caveCullingUpdate || viewUpdate || frustumUpdate || forceUpdate
                updateRegion.compareAndSet(false, indicesUpdate || viewUpdate || frustumUpdate || forceUpdate)

                FastMcMod.profiler.swap("camera")
                chunkStorage.update(forceUpdate)

                FastMcMod.profiler.swap("chunkCulling")
                val chunkCullingJob = if (updateChunk) {
                    launch {
                        updateChunkCulling()
                    }
                } else {
                    null
                }

                FastMcMod.profiler.swap("regionCulling")
                val regionCullingJob = launch {
                    uploadChunkJob.join()
                    chunkCullingJob?.join()
                    if (updateRegion.get()) {
                        updateRegionCulling(indicesUpdate && !updateChunk, indicesUpdate || viewUpdate || forceUpdate)
                    }
                }

                FastMcMod.profiler.swap("translucentSort")
                sortTranslucent()

                FastMcMod.profiler.swap("uploadChunk")
                uploadChunkJob.join()
                FastMcMod.profiler.swap("chunkCulling")
                chunkCullingJob?.join()
                FastMcMod.profiler.swap("regionCulling")
                regionCullingJob.join()

                FastMcMod.profiler.swap("scheduleRebuild")
                scheduleRebuild()

                FastMcMod.profiler.swap("post")
                if (updateRegion.get() || updateChunk) {
                    lastMatrixHash = camera.matrixHash
                    lastCameraX0 = cameraPosX0
                    lastCameraY0 = cameraPosY0
                    lastCameraZ0 = cameraPosZ0
                    lastCameraYaw0 = cameraYaw0
                    lastCameraPitch0 = cameraPitch0
                }
            }

            semaphore.countDown()
        }

        if (uploadChunks) {
            chunkBuilder.update()

            FpsDisplay.onChunkUpdate(chunkBuilder.uploadCount)

            updateCount += chunkBuilder.visibleUploadCount

            if (updateCount > 0 && (updateTimer.tickAndReset(50L) || chunkBuilder.totalTaskCount < ParallelUtils.CPU_THREADS * 4)) {
                chunkStorage.markCaveCullingDirty()
                updateCount = 0
            }

            if (chunkBuilder.visibleUploadCount != 0) {
                updateRegion.set(true)
            }
            uploadChunkJob.complete(Unit)
        }

        semaphore.await()
    }

    fun clear() {
        lastSortScheduleTask?.cancel(true)
        lastRebuildScheduleTask?.cancel(true)
        lastSortScheduleTask = null
        lastRebuildScheduleTask = null

        renderTileEntityList.front.clearAndTrim()
        renderTileEntityList.back.clearAndTrim()

        globalTileEntityList.front.clearAndTrim()
        globalTileEntityList.back.clearAndTrim()

        chunkCullingResults = emptyArray()
        tileEntityResults = emptyArray()

        lastCameraX0 = Int.MAX_VALUE
        lastCameraY0 = Int.MAX_VALUE
        lastCameraZ0 = Int.MAX_VALUE
        lastCameraYaw0 = Int.MAX_VALUE
        lastCameraPitch0 = Int.MAX_VALUE

        lastSortX = Double.MAX_VALUE
        lastSortY = Double.MAX_VALUE
        lastSortZ = Double.MAX_VALUE
        lastSortChunkX = Int.MAX_VALUE
        lastSortChunkY = Int.MAX_VALUE
        lastSortChunkZ = Int.MAX_VALUE

        forceUpdateTimer.reset(-999L)
        rebuildTimer.reset(-999L)
        lastMatrixHash = 0

        updateCount = 0
        updateTimer.reset(-999L)

        debugInfoString = ""

        chunkBuilder.clear()
    }

    fun reload() {
        chunkCullingResults = Array(chunkStorage.totalRegion) {
            Array(ParallelUtils.CPU_THREADS) {
                CullingResult()
            }
        }
        tileEntityResults = Array(ParallelUtils.CPU_THREADS) {
            TileEntityResult()
        }
        lastViewDistance = viewDistance
    }

    private fun scheduleRebuild() {
        if (lastRebuildScheduleTask.isDoneOrNull && rebuildTimer.tickAndReset(16L)) {
            lastRebuildScheduleTask = FastMcExtendScope.pool.submit(scheduleRebuildRunnable)
        }
    }

    private val scheduleRebuildRunnable = Runnable {
        chunkBuilder.scheduleTasks {
            val chunkIndices = chunkStorage.sortedChunkIndices
            val chunkArray = chunkStorage.renderChunkArray

            for (i in chunkIndices.size - 1 downTo 0) {
                val renderChunk = chunkArray[chunkIndices[i]]
                if (renderChunk.isVisible && renderChunk.isDirty) {
                    createRebuild(renderChunk)
                }
            }
        }
    }

    private fun sortTranslucent() {
        if (!lastSortScheduleTask.isDoneOrNull) return

        val roundedChunkX = (camera.blockX + 8) shr 4
        val roundedChunkY = (camera.blockY + 8) shr 4
        val roundedChunkZ = (camera.blockZ + 8) shr 4
        var count = 0

        if (lastSortChunkX != roundedChunkX || lastSortChunkY != roundedChunkY || lastSortChunkZ != roundedChunkZ) {
            count = 128
        } else if (distanceSq(camera.posX, camera.posY, camera.posZ, lastSortX, lastSortY, lastSortZ) > 1.0) {
            count = 16
        }

        if (count != 0) {
            lastSortChunkX = roundedChunkX
            lastSortChunkY = roundedChunkY
            lastSortChunkZ = roundedChunkZ

            lastSortX = camera.posX
            lastSortY = camera.posY
            lastSortZ = camera.posZ

            val chunkStorage = chunkStorage
            val chunkBuilder = chunkBuilder
            val chunkArray = chunkStorage.renderChunkArray
            val chunkIndices = chunkStorage.sortedChunkIndices

            lastSortScheduleTask = FastMcExtendScope.pool.submit {
                chunkBuilder.scheduleTasks {
                    for (i in chunkIndices.size - 1 downTo 0) {
                        if (count == 0) break
                        val chunkIndex = chunkIndices[i]
                        val renderChunk = chunkArray[chunkIndex]
                        if (renderChunk.isEmpty) continue
                        if (!renderChunk.isVisible) continue
                        if (createSort(renderChunk)) {
                            --count
                        }
                    }
                }
            }
        }
    }

    @Suppress("DuplicatedCode")
    private suspend fun updateChunkCulling() {
        val chunkStorage = chunkStorage

        val renderTileEntityList0 = renderTileEntityList.back
        renderTileEntityList0.clear()

        val globalTileEntityList0 = globalTileEntityList.back
        globalTileEntityList0.clear()

        val chunkArray = chunkStorage.renderChunkArray
        val cameraChunk = chunkStorage.cameraChunk

        val results = chunkCullingResults
        if (results.isEmpty()) return

        val tileEntityResults = tileEntityResults
        if (tileEntityResults.isEmpty()) return

        val chunkIndices = chunkStorage.sortedChunkIndices
        var idCounter = ParallelUtils.CPU_THREADS

        val statusCache = chunkLoadingStatusCache
        statusCache.init(
            camera.chunkX,
            camera.chunkZ,
            chunkStorage.sizeXZ,
            chunkLoadingStatusCacheTimer.tickAndReset(1000L)
        )
        val caveCulling = caveCulling

        coroutineScope {
            if (caveCulling) {
                val caveCullingBitSet = chunkStorage.caveCullingBitSet
                ParallelUtils.splitListIndex(
                    total = chunkIndices.size,
                    parallelism = ParallelUtils.CPU_THREADS,
                    blockForEach = { start, end ->
                        val jobID = --idCounter
                        launch {
                            val tileEntityResult = tileEntityResults[jobID]

                            for (i in end - 1 downTo start) {
                                val chunkIndex = chunkIndices[chunkIndices.size - 1 - i]
                                val renderChunk = chunkArray[chunkIndex]

                                if (renderChunk !== cameraChunk) {
                                    renderChunk.isVisible = false

                                    if (!renderChunk.renderRegion.frustumCull.isInFrustum()) continue
                                    if (!renderChunk.checkFogRange()) continue
                                    if (!caveCullingBitSet.containsFast(chunkIndex)) continue
                                    if (!renderChunk.checkAnyAdjBuilt()) continue
                                    if (!renderChunk.frustumCull.isInFrustum()) continue
                                    if (!renderChunk.isBuilt && !renderChunk.checkAdjChunkLoaded(statusCache)) continue

                                    renderChunk.isVisible = true
                                    val result = results[renderChunk.renderRegion.index][jobID]
                                    result.array[result.size++] = renderChunk
                                    tileEntityResult.add(renderChunk)
                                } else {
                                    if (!renderChunk.isBuilt && !renderChunk.checkAdjChunkLoaded(statusCache))
                                        continue
                                    renderChunk.isVisible = true
                                    val result = results[renderChunk.renderRegion.index][jobID]
                                    result.array[result.size++] = renderChunk
                                    tileEntityResult.add(renderChunk)
                                }
                            }
                        }
                    }
                )
            } else {
                ParallelUtils.splitListIndex(
                    total = chunkIndices.size,
                    parallelism = ParallelUtils.CPU_THREADS,
                    blockForEach = { start, end ->
                        val jobID = --idCounter
                        launch {
                            val tileEntityResult = tileEntityResults[jobID]

                            for (i in end - 1 downTo start) {
                                val chunkIndex = chunkIndices[chunkIndices.size - 1 - i]
                                val renderChunk = chunkArray[chunkIndex]

                                if (renderChunk !== cameraChunk) {
                                    renderChunk.isVisible = false

                                    if (!renderChunk.renderRegion.frustumCull.isInFrustum()) continue
                                    if (!renderChunk.checkFogRange()) continue
                                    if (!renderChunk.checkAnyAdjBuilt()) continue
                                    if (!renderChunk.isBuilt && !renderChunk.checkAdjChunkLoaded(statusCache)) continue
                                    if (!renderChunk.frustumCull.isInFrustum()) continue

                                    renderChunk.isVisible = true
                                    val result = results[renderChunk.renderRegion.index][jobID]
                                    result.array[result.size++] = renderChunk
                                    tileEntityResult.add(renderChunk)
                                } else {
                                    if (!renderChunk.isBuilt && !renderChunk.checkAdjChunkLoaded(statusCache))
                                        continue
                                    renderChunk.isVisible = true
                                    val result = results[renderChunk.renderRegion.index][jobID]
                                    result.array[result.size++] = renderChunk
                                    tileEntityResult.add(renderChunk)
                                }
                            }
                        }
                    }
                )
            }
        }

        coroutineScope {
            launch {
                @Suppress("UNCHECKED_CAST")
                val tileEntityRenderer =
                    FastMcMod.worldRenderer.tileEntityRenderer as TileEntityInstancingRenderer<ITileEntityInfo<*>>
                tileEntityRenderer.clear()
                for (result in tileEntityResults) {
                    renderTileEntityList0.addAll(result.tileEntityList)
                    globalTileEntityList0.addAll(result.globalTileEntityList)
                    tileEntityRenderer.add(result.instancingTileEntityList)
                    result.clear()
                }
            }

            launch {
                for (region in chunkStorage.regionArray) {
                    val array = results[region.index]
                    val list = region.visibleRenderChunkList
                    list.clearFast()

                    val listArray = list.elements()
                    var size = 0
                    for (result in array) {
                        System.arraycopy(result.array, 0, listArray, size, result.size)
                        size += result.size
                        Arrays.fill(result.array, 0, result.size, null)
                        result.size = 0
                    }
                    list.setSize(size)
                }
            }
        }

        renderTileEntityList.swap()
        globalTileEntityList.swap()
    }

    private inline fun RenderChunk.checkAnyAdjBuilt(): Boolean {
        return checkAdjacentBuilt(Direction.I_DOWN)
            || checkAdjacentBuilt(Direction.I_UP)
            || checkAdjacentBuilt(Direction.I_NORTH)
            || checkAdjacentBuilt(Direction.I_SOUTH)
            || checkAdjacentBuilt(Direction.I_WEST)
            || checkAdjacentBuilt(Direction.I_EAST)
    }

    private inline fun RenderChunk.checkAdjacentBuilt(index: Int): Boolean {
        val adjacentChunk = adjacentRenderChunk[index]
        return adjacentChunk != null && adjacentChunk.isBuilt
    }

    private inline fun RenderChunk.checkAdjChunkLoaded(statusCache: ChunkLoadingStatusCache<*>): Boolean {
        return statusCache.isChunkLoaded(chunkX, chunkZ + 1)
            && statusCache.isChunkLoaded(chunkX - 1, chunkZ)
            && statusCache.isChunkLoaded(chunkX, chunkZ - 1)
            && statusCache.isChunkLoaded(chunkX + 1, chunkZ)
    }

    private suspend fun updateRegionCulling(resort: Boolean, refresh: Boolean) {
        coroutineScope {
            val layerCount = layerCount
            val chunkStorage = chunkStorage
            val regionArray = chunkStorage.regionArray
            val comparator = chunkStorage.chunkOrderComp

            for (regionIndex in regionArray.indices) {
                val region = regionArray[regionIndex]
                if (!region.frustumCull.isInFrustum()) continue

                launch {
                    val list = region.visibleRenderChunkList
                    val array = list.elements()
                    val size = list.size

                    if (resort) {
                        ObjectIntrosort.sort(array, 0, size, comparator)
                    }

                    if (refresh) {
                        for (i in 0 until size) {
                            array[i].resetUpdate()
                        }
                    } else {
                        var updated = false
                        var i = 0
                        while (!updated && i < size) {
                            updated = array[i++].checkUpdate()
                        }
                        while (i < size) {
                            array[i++].resetUpdate()
                        }
                        if (!updated) return@launch
                    }

                    for (i in 0 until size) {
                        region.tempVisibleBits[i] = calculateVisibleFaceBit(array[i]).toByte()
                    }

                    for (layerIndex in 0 until layerCount) {
                        val layerBatch = region.getLayer(layerIndex)
                        layerBatch.update()

                        for (i in 0 until size) {
                            val renderChunk = array[i]
                            val layer = renderChunk.layers[layerIndex]
                            val vertexRegion = layer.vertexRegion ?: continue
                            val indexRegion = layer.indexRegion ?: continue
                            layer.faceData?.addToBatch(
                                layerBatch,
                                vertexRegion.offset.toInt(),
                                indexRegion.offset.toInt(),
                                region.tempVisibleBits[i].toInt(),
                                renderChunk.localIndex
                            )
                        }
                    }
                }
            }
        }
    }

    private fun calculateVisibleFaceBit(renderChunk: RenderChunk): Int {
        return (Direction.B_EAST * ((renderChunk.minX - camera.posX).floorToInt() ushr 31)) or
            (Direction.B_WEST * ((camera.posX - renderChunk.maxX).floorToInt() ushr 31)) or
            (Direction.B_UP * ((renderChunk.minY - camera.posY).floorToInt() ushr 31)) or
            (Direction.B_DOWN * ((camera.posY - renderChunk.maxY).floorToInt() ushr 31)) or
            (Direction.B_SOUTH * ((renderChunk.minZ - camera.posZ).floorToInt() ushr 31)) or
            (Direction.B_NORTH * ((camera.posZ - renderChunk.maxZ).floorToInt() ushr 31))
    }

    private val updateDebugRunnable = Runnable {
        val chunkBuilder = chunkBuilder
        val uploadBufferPool = contextProvider.bufferPool
        val chunkStorage = chunkStorage
        val chunkArray = chunkStorage.renderChunkArray
        val regionArray = chunkStorage.regionArray

        var visibleRegionCount = 0
        var regionCount = 0

        var totalChunkVertexSize = 0L
        var bufferCapacity = 0L

        var visibleChunkCount = 0
        var totalChunkCount = 0
        var visibleChunkVertexSize = 0L

        for (i in regionArray.indices) {
            val region = regionArray[i]
            val allocated = region.vertexBufferPool.allocated + region.indexBufferPool.allocated
            totalChunkVertexSize += allocated
            if (allocated != 0L) {
                if (region.frustumCull.isInFrustum()) {
                    visibleRegionCount++
                }
                regionCount++
            }
            bufferCapacity += region.vertexBufferPool.capacity
            bufferCapacity += region.indexBufferPool.capacity
        }

        for (i in chunkArray.indices) {
            val renderChunk = chunkArray[i]
            val layers = renderChunk.layers
            if (renderChunk.isVisible) {
                for (i2 in layers.indices) {
                    val layer = layers[i2]

                    val vertexRegion = layer.vertexRegion
                    if (vertexRegion != null) {
                        visibleChunkVertexSize += vertexRegion.length
                    }

                    val indexRegion = layer.indexRegion
                    if (indexRegion != null) {
                        visibleChunkVertexSize += indexRegion.length
                    }
                }
            }
            if (!renderChunk.isEmpty) {
                if (renderChunk.isVisible) visibleChunkCount++
                totalChunkCount++
            }
        }

        debugInfoString = String.format(
            "D: %d, R: %d/%d/%d, C: %d/%d/%d(%.1f/%.1f/%.1f MB), T: %02d, A: %d, U: %02d, B: %d/%d(%.1f/%.1f MB)",
            lastViewDistance,
            visibleRegionCount,
            regionCount,
            regionArray.size,
            visibleChunkCount,
            totalChunkCount,
            chunkArray.size,
            visibleChunkVertexSize.toDouble() / 1048576.0,
            totalChunkVertexSize.toDouble() / 1048576.0,
            bufferCapacity.toDouble() / 1048576.0,
            chunkBuilder.totalTaskCount,
            chunkBuilder.activeTaskCount,
            chunkBuilder.uploadTaskCount,
            uploadBufferPool.allocatedRegion,
            uploadBufferPool.maxRegions,
            uploadBufferPool.allocatedSize.toDouble() / 1048576.0,
            uploadBufferPool.capacity.toDouble() / 1048576.0
        )
    }

    private fun updateDebugInfo() {
        if (lastDebugUpdateTask.isDoneOrNull) {
            lastDebugUpdateTask = FastMcExtendScope.pool.submit(updateDebugRunnable)
        }
    }

    private class TileEntityResult {
        val tileEntityList = FastObjectArrayList<ITileEntityInfo<*>>()
        val instancingTileEntityList = FastObjectArrayList<ITileEntityInfo<*>>()
        val globalTileEntityList = FastObjectArrayList<ITileEntityInfo<*>>()

        fun add(renderChunk: RenderChunk) {
            if (renderChunk.isVisible) {
                renderChunk.tileEntityList?.let {
                    tileEntityList.addAll(it)
                }
                renderChunk.instancingTileEntityList?.let {
                    instancingTileEntityList.addAll(it)
                }
            }
            renderChunk.globalTileEntityList?.let {
                globalTileEntityList.addAll(it)
            }
        }

        fun clear() {
            tileEntityList.clear()
            instancingTileEntityList.clear()
            globalTileEntityList.clear()
        }
    }

    fun renderLayer(layerIndex: Int) {
        val regionArray = chunkStorage.regionArray
        val shader = shaderManager.shader

        for (i in chunkStorage.regionIndices) {
            val region = regionArray[i.toInt()]
            if (!region.frustumCull.isInFrustum()) continue
            val layerBatch = region.getLayer(layerIndex)
            layerBatch.checkUpdate()
            if (layerBatch.isEmpty) continue

            shader.setRegionOffset(
                (region.originX - camera.posX).toFloat(),
                (region.originY - camera.posY).toFloat(),
                (region.originZ - camera.posZ).toFloat()
            )

            region.vao.bind()
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, layerBatch.bufferID)
            glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0L, layerBatch.count, 0)
        }
    }

    fun destroy() {
        chunkBuilder.clear()
        chunkStorageNullable?.destroy()
        chunkStorageNullable = null
        shaderManager.destroy()
    }

    private inner class CullingResult {
        var size = 0
        val array = arrayOfNulls<RenderChunk>(chunkStorage.regionChunkCount)
    }

    companion object {
        @JvmField
        val VERTEX_SIZE = 16

        @JvmField
        val VERTEX_ATTRIBUTE_MAIN = buildAttribute(VERTEX_SIZE) {
            float(0, 3, GLDataType.GL_UNSIGNED_SHORT, false) // Pos
            float(1, 2, GLDataType.GL_UNSIGNED_SHORT, true) // Block texture uv
            float(2, 2, GLDataType.GL_UNSIGNED_BYTE, false) // Light map uv
            float(3, 3, GLDataType.GL_UNSIGNED_BYTE, true) // Color multiplier
            int(4, 1, GLDataType.GL_UNSIGNED_BYTE) // Attributes
        }

        @JvmField
        val VERTEX_ATTRIBUTE_CHUNK_OFFSET = buildAttribute(4, 1) {
            float(6, 3, GLDataType.GL_UNSIGNED_BYTE, false) // Chunk offset
        }
    }
}