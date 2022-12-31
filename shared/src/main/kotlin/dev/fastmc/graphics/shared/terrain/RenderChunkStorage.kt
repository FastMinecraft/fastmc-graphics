package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.IntArrayFIFOQueueNoShrink
import dev.fastmc.common.collection.StaticBitSet
import dev.fastmc.graphics.shared.renderer.cameraChunkX
import dev.fastmc.graphics.shared.renderer.cameraChunkY
import dev.fastmc.graphics.shared.renderer.cameraChunkZ
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.shared.util.FastMcExtendScope
import it.unimi.dsi.fastutil.ints.IntArrays
import it.unimi.dsi.fastutil.ints.IntComparator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Future
import kotlin.math.max
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
class RenderChunkStorage(
    private val renderer: TerrainRenderer,
    viewDistance: Int,
) {
    @JvmField
    val minChunkY = renderer.minChunkY

    @JvmField
    val maxChunkY = renderer.maxChunkY

    val sizeY get() = maxChunkY - minChunkY

    @JvmField
    val sizeXZ = viewDistance * 2 + 1

    @JvmField
    val regionSizeXZ = ((viewDistance + 7) shr 3) + 1

    @JvmField
    val totalChunk = sizeXZ * sizeY * sizeXZ

    @JvmField
    val totalRegion = regionSizeXZ * regionSizeXZ

    val regionChunkCount get() = 16 * (maxChunkY - minChunkY) * 16

    @JvmField
    val regionArray = Array(totalRegion) {
        RenderRegion(renderer, this, it)
    }

    @JvmField
    val renderChunkArray = Array(totalChunk) {
        RenderChunk(renderer, regionArray[0], it)
    }

    private val sortingUpdateCounter = UpdateCounter()
    private var lastSortingJob: Future<*>? = null
    private val chunkDistanceArray = IntArray(totalChunk)
    private val chunkSortSuppArray = IntArray(totalChunk) { it }
    var sortedChunkIndices = chunkSortSuppArray.copyOf(); private set
    var chunkOrder = chunkSortSuppArray.copyOf(); private set

    private val regionDistanceArray = IntArray(totalRegion)
    var regionIndices = IntArray(totalRegion) { it }; private set
    var regionOrder = regionIndices.copyOf(); private set

    private var caveCullingDirty = true
    private val caveCullingUpdateCounter = UpdateCounter()
    private var lastCaveCullingJob: Future<*>? = null
    private val caveCullingQueue = IntArrayFIFOQueueNoShrink(totalChunk)
    private val caveCullingBitSet0 = DoubleBufferedCollection(
        StaticBitSet(totalChunk),
        StaticBitSet(totalChunk),
        DoubleBufferedCollection.emptyInitAction()
    )
    val caveCullingBitSet get() = caveCullingBitSet0.get()

    var cameraChunk = renderChunkArray[0]; private set

    private var lastCameraUpdateChunkX = Int.MAX_VALUE
    private var lastCameraUpdateChunkY = Int.MAX_VALUE
    private var lastCameraUpdateChunkZ = Int.MAX_VALUE

    private var lastCameraUpdateX = Double.MAX_VALUE
    private var lastCameraUpdateY = Double.MAX_VALUE
    private var lastCameraUpdateZ = Double.MAX_VALUE

    init {
        for (x in 0 until regionSizeXZ) {
            for (z in 0 until regionSizeXZ) {
                val i = regionPos2Index(x, z)
                val region = regionArray[i]
                region.setPos(x shl 8, z shl 8)
            }
        }

        for (renderChunk in renderChunkArray) {
            val region = getRegionByBlock(renderChunk.originX, renderChunk.originZ)
            renderChunk.renderRegion = region
        }
    }

    suspend fun update(force: Boolean) {
        updateCaveCulling()
        cameraChunk = getRenderChunkByChunk0(
            renderer.cameraChunkX,
            renderer.cameraChunkY.coerceIn(minChunkY, maxChunkY - 1),
            renderer.cameraChunkZ
        )

        if (force
            || lastCameraUpdateChunkX != renderer.cameraChunkX
            || lastCameraUpdateChunkY != renderer.cameraChunkY
            || lastCameraUpdateChunkZ != renderer.cameraChunkZ
            || distanceSq(
                renderer.cameraX, renderer.cameraY, renderer.cameraZ,
                lastCameraUpdateX, lastCameraUpdateY, lastCameraUpdateZ
            ) > 16.0
        ) {
            lastCameraUpdateChunkX = renderer.cameraChunkX
            lastCameraUpdateChunkY = renderer.cameraChunkY
            lastCameraUpdateChunkZ = renderer.cameraChunkZ
            lastCameraUpdateX = renderer.cameraX
            lastCameraUpdateY = renderer.cameraY
            lastCameraUpdateZ = renderer.cameraZ
            updateRegions()
            markCaveCullingDirty()
            if (lastSortingJob.isDoneOrNull) {
                lastSortingJob = FastMcExtendScope.pool.submit(updateChunkIndicesRunnable)
            }
        }
    }

    private suspend fun updateRegions() {
        coroutineScope {
            val halfSize = sizeXZ shr 1
            val startChunkX = renderer.cameraChunkX - halfSize
            val startChunkZ = renderer.cameraChunkZ - halfSize
            val endChunkX = startChunkX + sizeXZ
            val endChunkZ = startChunkZ + sizeXZ

            val startRegionX = (renderer.cameraChunkX - halfSize) shr 4
            val startRegionZ = (renderer.cameraChunkZ - halfSize) shr 4
            val offsetX = Math.floorMod(startRegionX, regionSizeXZ)
            val offsetZ = Math.floorMod(startRegionZ, regionSizeXZ)

            for (i in regionArray.indices) {
                launch(FastMcCoreScope.context) {
                    val blockX = (Math.floorMod((i % regionSizeXZ) - offsetX, regionSizeXZ) + startRegionX) shl 8
                    val blockZ = (Math.floorMod((i / regionSizeXZ) - offsetZ, regionSizeXZ) + startRegionZ) shl 8

                    val region = regionArray[i]
                    region.setPos(blockX, blockZ)

                    val startX = max(blockX shr 4, startChunkX)
                    val startZ = max(blockZ shr 4, startChunkZ)
                    val endX = min((blockX shr 4) + 16, endChunkX)
                    val endZ = min((blockZ shr 4) + 16, endChunkZ)

                    for (x in startX until endX) {
                        for (z in startZ until endZ) {
                            for (y in minChunkY until maxChunkY) {
                                val renderChunk = getRenderChunkByChunk0(x, y, z)
                                renderChunk.setPos(x, y, z)
                                renderChunk.renderRegion = region
                            }
                        }
                    }
                }
            }
        }

        coroutineScope {
            ParallelUtils.splitListIndex(
                totalChunk,
                blockForEach = { start, end ->
                    launch {
                        for (i in start until end) {
                            renderChunkArray[i].updateAdjacentChunk()
                        }
                    }
                }
            )
        }
    }

    private val updateChunkIndicesRunnable = Runnable {
        val newChunkIndices = sortedChunkIndices.copyOf()
        newChunkIndices.copyInto(chunkSortSuppArray)

        for (i in newChunkIndices) {
            val renderChunk = renderChunkArray[i]
            chunkDistanceArray[i] = -distanceSq(
                renderer.cameraChunkX, renderer.cameraChunkY, renderer.cameraChunkZ,
                renderChunk.chunkX, renderChunk.chunkY, renderChunk.chunkZ
            )
        }

        IntArrays.mergeSort(
            newChunkIndices,
            0,
            newChunkIndices.size,
            object : IntComparator {
                private val distanceArray = this@RenderChunkStorage.chunkDistanceArray
                override fun compare(k1: Int, k2: Int): Int {
                    return distanceArray[k1].compareTo(distanceArray[k2])
                }
            },
            chunkSortSuppArray
        )

        val newChunkOrder = IntArray(totalChunk)
        for (i in newChunkOrder.indices) {
            newChunkOrder[newChunkIndices[i]] = i
        }

        sortedChunkIndices = newChunkIndices
        chunkOrder = newChunkOrder
        sortingUpdateCounter.update()

        val newRegionIndices = regionIndices.copyOf()

        for (i in newRegionIndices) {
            val region = regionArray[i]
            val cameraRegionChunkX = renderer.cameraChunkX shr 4 shl 4
            val cameraRegionChunkZ = renderer.cameraChunkZ shr 4 shl 4
            regionDistanceArray[i] = -distanceSq(
                cameraRegionChunkX, cameraRegionChunkZ,
                region.originX shr 4, region.originZ shr 4
            )
        }

        IntArrays.mergeSort(
            newRegionIndices,
            object : IntComparator {
                private val distanceArray = this@RenderChunkStorage.regionDistanceArray
                override fun compare(k1: Int, k2: Int): Int {
                    return distanceArray[k1].compareTo(distanceArray[k2])
                }
            }
        )

        val newRegionOrder = IntArray(totalRegion)
        for (i in newRegionIndices.indices) {
            newRegionOrder[newRegionIndices[i]] = i
        }

        regionIndices = newRegionIndices
        regionOrder = newRegionOrder
    }

    fun markCaveCullingDirty() {
        caveCullingDirty = true
    }

    private fun updateCaveCulling() {
        if (lastCaveCullingJob.isDoneOrNull && caveCullingDirty) {
            caveCullingDirty = false
            lastCaveCullingJob = FastMcExtendScope.pool.submit(updateCaveCullingRunnable)
        }
    }

    private val updateCaveCullingRunnable = Runnable {
        val newCullingBitSet = caveCullingBitSet0.getSwap()
        newCullingBitSet.clear()

        val cameraChunk = getRenderChunkByChunk(renderer.cameraChunkX, renderer.cameraChunkY, renderer.cameraChunkZ)
        val directions = Direction.VALUES

        if (cameraChunk != null) {
            newCullingBitSet.addFast(cameraChunk.index)
            for (nextDirection in directions) {
                val nextRenderChunk = cameraChunk.adjacentRenderChunk[nextDirection.ordinal] ?: continue
                if (!cameraChunk.occlusionData.isVisible(nextDirection)) continue
                newCullingBitSet.addFast(nextRenderChunk.index)
                caveCullingQueue.enqueue(nextRenderChunk.index or (nextDirection.idOpposite shl 17) or (nextDirection.bitOpposite shl 20))
            }
        } else {
            if (renderer.cameraChunkY < minChunkY) {
                for (i in 0 until sizeXZ * sizeXZ) {
                    val renderChunk = renderChunkArray[i * sizeY]
                    if (!renderChunk.isBuilt && renderChunk !== this.cameraChunk) continue
                    newCullingBitSet.addFast(renderChunk.index)
                    caveCullingQueue.enqueue(renderChunk.index or (Direction.I_DOWN shl 17) or (Direction.B_DOWN shl 20))
                }
            } else {
                val offset = sizeY - 1
                for (i in 0 until sizeXZ * sizeXZ) {
                    val renderChunk = renderChunkArray[i * sizeY + offset]
                    if (!renderChunk.isBuilt && renderChunk !== this.cameraChunk) continue
                    newCullingBitSet.addFast(renderChunk.index)
                    caveCullingQueue.enqueue(renderChunk.index or (Direction.I_UP shl 17) or (Direction.B_UP shl 20))
                }
            }
        }

        while (!caveCullingQueue.isEmpty) {
            val i = caveCullingQueue.dequeueInt()
            val chunkIndex = i and 0b1_1111_1111_1111_1111
            val oppositeDirection = (i shr 17) and 0b111
            val excludedDirections = (i shr 20) and 0b111111
            val renderChunk = renderChunkArray[chunkIndex]

            for (nextDirection in directions) {
                if (excludedDirections and nextDirection.bit != 0) continue
                if (!renderChunk.occlusionData.isVisible(directions[oppositeDirection], nextDirection)) continue
                val nextRenderChunk = renderChunk.adjacentRenderChunk[nextDirection.ordinal] ?: continue
                if (!newCullingBitSet.add(nextRenderChunk.index)) continue
                if (!nextRenderChunk.isBuilt) continue
                caveCullingQueue.enqueue(nextRenderChunk.index or (nextDirection.idOpposite shl 17) or ((excludedDirections or nextDirection.bitOpposite) shl 20))
            }
        }

        caveCullingBitSet0.swapAndGet()
        caveCullingUpdateCounter.update()
    }

    fun checkChunkIndicesUpdate(): Boolean {
        return sortingUpdateCounter.check()
    }

    fun checkCaveCullingUpdate(): Boolean {
        return caveCullingUpdateCounter.check()
    }

    fun getRegionByBlock(blockX: Int, blockZ: Int): RenderRegion {
        return getRegionByRegion(blockX shr 8, blockZ shr 8)
    }

    fun getRegionByChunk(chunkX: Int, chunkZ: Int): RenderRegion {
        return getRegionByRegion(chunkX shr 4, chunkZ shr 4)
    }

    fun getRegionByRegion(regionX: Int, regionZ: Int): RenderRegion {
        return regionArray[regionPos2Index(
            Math.floorMod(regionX, regionSizeXZ),
            Math.floorMod(regionZ, regionSizeXZ)
        )]
    }

    fun getRenderChunkByBlock(blockX: Int, blockY: Int, blockZ: Int): RenderChunk? {
        return getRenderChunkByChunk(blockX shr 4, blockY shr 4, blockZ shr 4)
    }

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    fun getRenderChunkByChunk(chunkX: Int, chunkY: Int, chunkZ: Int): RenderChunk? {
        return if (chunkY >= minChunkY && chunkY < maxChunkY) {
            getRenderChunkByChunk0(chunkX, chunkY, chunkZ)
        } else {
            null
        }
    }

    private fun getRenderChunkByChunk0(
        chunkX: Int,
        chunkY: Int,
        chunkZ: Int
    ): RenderChunk {
        return renderChunkArray[chunkPos2Index(
            Math.floorMod(chunkX, sizeXZ),
            chunkY,
            Math.floorMod(chunkZ, sizeXZ)
        )]
    }

    inline fun chunkPos2Index(x: Int, y: Int, z: Int): Int {
        return (y - minChunkY) + (x + z * sizeXZ) * sizeY
    }

    inline fun regionPos2Index(x: Int, z: Int): Int {
        return x + z * regionSizeXZ
    }

    fun destroy() {
        for (renderChunk in renderChunkArray) {
            renderChunk.destroy()
        }
        for (region in regionArray) {
            region.destroy()
        }
    }
}