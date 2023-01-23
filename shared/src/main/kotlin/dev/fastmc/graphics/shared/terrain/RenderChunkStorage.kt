package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.IntArrayFIFOQueueNoShrink
import dev.fastmc.common.collection.StaticBitSet
import dev.fastmc.common.sort.ByteInsertionSort
import dev.fastmc.common.sort.IntIntrosort
import dev.fastmc.graphics.shared.renderer.cameraChunkX
import dev.fastmc.graphics.shared.renderer.cameraChunkY
import dev.fastmc.graphics.shared.renderer.cameraChunkZ
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.shared.util.FastMcExtendScope
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
    var sortedChunkIndices = IntArray(totalChunk) { it }; private set
    var chunkOrder = sortedChunkIndices.copyOf(); private set
    var chunkOrderComp = chunkOrderComparator(chunkOrder); private set

    private val regionDistanceArray = IntArray(totalRegion)
    var regionIndices = ByteArray(totalRegion) { it.toByte() }; private set
    var regionOrder = regionIndices.copyOf(); private set

    private var caveCullingDirty = true
    private val caveCullingUpdateCounter = UpdateCounter()
    private var lastCaveCullingJob: Future<*>? = null
    private val caveCullingQueue = IntArrayFIFOQueueNoShrink(totalChunk)
    private val caveCullingBitSet0 = DoubleBuffered( { StaticBitSet(totalChunk) }, DoubleBuffered.CLEAR_INIT_ACTION)
    val caveCullingBitSet get() = caveCullingBitSet0.front

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

                    var indexX = Math.floorMod(startX, sizeXZ)
                    val startIndexZ = Math.floorMod(startZ, sizeXZ)
                    var indexZ: Int

                    for (x in startX until endX) {
                        indexZ = startIndexZ
                        for (z in startZ until endZ) {
                            var renderChunk: RenderChunk
                            var index = (indexX + indexZ * sizeXZ) * sizeY

                            renderChunk = renderChunkArray[index]
                            renderChunk.setPos(x, minChunkY, z)
                            renderChunk.renderRegion = region

                            updateAdjancentChunk(
                                renderChunk,
                                indexX,
                                indexZ,
                                startChunkX,
                                endChunkX,
                                startChunkZ,
                                endChunkZ
                            )

                            renderChunk = renderChunkArray[index + sizeY - 1]
                            renderChunk.setPos(x, maxChunkY - 1, z)
                            renderChunk.renderRegion = region

                            updateAdjancentChunk(
                                renderChunk,
                                indexX,
                                indexZ,
                                startChunkX,
                                endChunkX,
                                startChunkZ,
                                endChunkZ
                            )

                            for (y in minChunkY + 1 until maxChunkY - 1) {
                                renderChunk = renderChunkArray[++index]
                                renderChunk.setPos(x, y, z)
                                renderChunk.renderRegion = region

                                renderChunk.adjacentRenderChunk[Direction.I_DOWN] = renderChunkArray[index - 1]
                                renderChunk.adjacentRenderChunk[Direction.I_UP] = renderChunkArray[index + 1]

                                updateAdjancentChunk(
                                    renderChunk,
                                    indexX,
                                    indexZ,
                                    startChunkX,
                                    endChunkX,
                                    startChunkZ,
                                    endChunkZ
                                )
                            }

                            indexZ = (indexZ + 1) % sizeXZ
                        }
                        indexX = (indexX + 1) % sizeXZ
                    }
                }
            }
        }
    }

    private fun updateAdjancentChunk(
        renderChunk: RenderChunk,
        indexX: Int,
        indexZ: Int,
        startChunkX: Int,
        endChunkX: Int,
        startChunkZ: Int,
        endChunkZ: Int
    ) {
        val y = renderChunk.chunkY

        if (renderChunk.chunkX > startChunkX) {
            renderChunk.adjacentRenderChunk[Direction.I_WEST] =
                renderChunkArray[chunkPos2Index(Math.floorMod(indexX - 1, sizeXZ), y, indexZ)]
        } else {
            renderChunk.adjacentRenderChunk[Direction.I_WEST] = null
        }

        if (renderChunk.chunkX < endChunkX - 1) {
            renderChunk.adjacentRenderChunk[Direction.I_EAST] =
                renderChunkArray[chunkPos2Index(Math.floorMod(indexX + 1, sizeXZ), y, indexZ)]
        } else {
            renderChunk.adjacentRenderChunk[Direction.I_EAST] = null
        }

        if (renderChunk.chunkZ > startChunkZ) {
            renderChunk.adjacentRenderChunk[Direction.I_NORTH] =
                renderChunkArray[chunkPos2Index(indexX, y, Math.floorMod(indexZ - 1, sizeXZ))]
        } else {
            renderChunk.adjacentRenderChunk[Direction.I_NORTH] = null
        }

        if (renderChunk.chunkZ < endChunkZ - 1) {
            renderChunk.adjacentRenderChunk[Direction.I_SOUTH] =
                renderChunkArray[chunkPos2Index(indexX, y, Math.floorMod(indexZ + 1, sizeXZ))]
        } else {
            renderChunk.adjacentRenderChunk[Direction.I_SOUTH] = null
        }
    }

    private val updateChunkIndicesRunnable = Runnable {
        val newChunkIndices = sortedChunkIndices.copyOf()

        for (i in newChunkIndices) {
            val renderChunk = renderChunkArray[i]
            chunkDistanceArray[i] = -distanceSq(
                renderer.cameraChunkX, renderer.cameraChunkY, renderer.cameraChunkZ,
                renderChunk.chunkX, renderChunk.chunkY, renderChunk.chunkZ
            )
        }

        IntIntrosort.sort(newChunkIndices, chunkDistanceArray)

        val newChunkOrder = IntArray(totalChunk)
        for (i in newChunkOrder.indices) {
            newChunkOrder[newChunkIndices[i]] = i
        }

        sortedChunkIndices = newChunkIndices
        chunkOrder = newChunkOrder
        chunkOrderComp = chunkOrderComparator(newChunkOrder)
        sortingUpdateCounter.update()

        val newRegionIndices = regionIndices.copyOf()

        for (ib in newRegionIndices) {
            val i = ib.toInt()
            val region = regionArray[i]
            val cameraRegionChunkX = renderer.cameraChunkX shr 4 shl 4
            val cameraRegionChunkZ = renderer.cameraChunkZ shr 4 shl 4
            regionDistanceArray[i] = -distanceSq(
                cameraRegionChunkX, cameraRegionChunkZ,
                region.originX shr 4, region.originZ shr 4
            )
        }

        ByteInsertionSort.sort(newRegionIndices, regionDistanceArray)

        val newRegionOrder = ByteArray(totalRegion)
        for (i in newRegionIndices.indices) {
            newRegionOrder[newRegionIndices[i].toInt() and 0xFF] = i.toByte()
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
        val newCullingBitSet = caveCullingBitSet0.initBack().back

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

        caveCullingBitSet0.swap()
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

    private fun chunkOrderComparator(chunkOrder: IntArray): Comparator<RenderChunk> {
        return Comparator { o1, o2 ->
            chunkOrder[o1.index].compareTo(chunkOrder[o2.index])
        }
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