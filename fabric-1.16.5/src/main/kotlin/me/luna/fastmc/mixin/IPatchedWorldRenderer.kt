package me.luna.fastmc.mixin

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import me.luna.fastmc.mixin.accessor.AccessorWorldRenderer
import me.luna.fastmc.shared.util.DoubleBufferedCollection
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.ParallelUtils
import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.terrain.ChunkVertexData
import me.luna.fastmc.terrain.RegionBuiltChunkStorage
import me.luna.fastmc.terrain.RenderRegion
import me.luna.fastmc.terrain.VertexDataTransformer
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.Frustum
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

@Suppress("NOTHING_TO_INLINE")
interface IPatchedWorldRenderer {
    var ticked: Boolean
    val renderTileEntityList: List<BlockEntity>
    val visibleChunksBitSet: DoubleBufferedCollection<ExtendedBitSet>

    suspend fun setupTerrainIteration0(
        mainThreadContext: CoroutineContext,
        visibleChunks: FastObjectArrayList<WorldRenderer.ChunkInfo>,
        renderTileEntityList: FastObjectArrayList<BlockEntity>,
        chunkStorage: RegionBuiltChunkStorage,
        frustum: Frustum,
        frame: Int,
        cameraChunkBlockPos: BlockPos,
        chunkCulling: Boolean,
        chunkInfoList: ObjectArrayList<WorldRenderer.ChunkInfo>
    ) {
        withContext(FastMcCoreScope.context) {
            this@IPatchedWorldRenderer as WorldRenderer
            val queue = ConcurrentLinkedQueue(chunkInfoList)
            val dummy = this@IPatchedWorldRenderer.ChunkInfo(null, null, 0)

            launch(FastMcCoreScope.context) {
                val layers = RenderLayer.getBlockLayers()
                val chunkArray: Array<ChunkBuilder.BuiltChunk> = chunkStorage.chunks
                val regionArray = chunkStorage.regionArray

                val job = launch(FastMcCoreScope.context) {
                    chunkStorage.updateSorting(cameraChunkBlockPos)
                }

                val newSet = visibleChunksBitSet.swapAndGet()
                newSet.clear()
                newSet.ensureCapacity(chunkArray.size)

                for (i in regionArray.indices) {
                    regionArray[i].visible = false
                }

                var it: WorldRenderer.ChunkInfo?
                while (true) {
                    do {
                        it = queue.poll()
                    } while (it == null)

                    if (it === dummy) break

                    val builtChunk = it.chunk
                    builtChunk as IPatchedBuiltChunk
                    val chunkData = builtChunk.getData()
                    val list = chunkData.blockEntities

                    if (list.isNotEmpty()) {
                        renderTileEntityList.addAll(list as ObjectArrayList<BlockEntity>)
                    }

                    if (!chunkData.isEmpty) {
                        builtChunk.region.visible = true
                    }

                    newSet.add(builtChunk.index)
                    visibleChunks.add(it)
                }

                job.join()
                val chunkIndices = chunkStorage.sortedChunkIndices

                for (regionIndex in regionArray.indices) {
                    val region = regionArray[regionIndex]
                    if (region.dirty) {
                        updateRegionVbo(mainThreadContext, chunkIndices, chunkArray, newSet, region, layers)
                    } else {
                        updateRegionVisibility(chunkArray, newSet, region, layers)
                    }
                    region.dirty = false
                }
            }

            coroutineScope {
                val counter = AtomicInteger(ParallelUtils.CPU_THREADS * 2)
                for (i in chunkInfoList.indices) {
                    recursiveSetupTerrainIteration(
                        this,
                        frustum,
                        frame,
                        cameraChunkBlockPos,
                        chunkCulling,
                        queue,
                        counter,
                        chunkInfoList[i]
                    )
                }
            }

            queue.add(dummy)
        }
    }

    fun setupTerrainIteration(
        visibleChunks: FastObjectArrayList<WorldRenderer.ChunkInfo>,
        renderTileEntityList: FastObjectArrayList<BlockEntity>,
        chunks: RegionBuiltChunkStorage,
        frustum: Frustum,
        frame: Int,
        cameraChunkBlockPos: BlockPos,
        chunkCulling: Boolean,
        chunkInfoList: ObjectArrayList<WorldRenderer.ChunkInfo>
    ) {
        runBlocking {
            setupTerrainIteration0(
                this.coroutineContext,
                visibleChunks,
                renderTileEntityList,
                chunks,
                frustum,
                frame,
                cameraChunkBlockPos,
                chunkCulling,
                chunkInfoList
            )
        }
    }

    companion object {
        @JvmField
        val DIRECTIONS = Direction.values()

        @JvmField
        val UPDATE_FUNCTION = IntUnaryOperator {
            max(it - 1, 0)
        }

        private fun IPatchedWorldRenderer.recursiveSetupTerrainIteration(
            scope: CoroutineScope,
            frustum: Frustum,
            frame: Int,
            cameraChunkBlockPos: BlockPos,
            chunkCulling: Boolean,
            queue: ConcurrentLinkedQueue<WorldRenderer.ChunkInfo>,
            counter: AtomicInteger,
            chunkInfo: WorldRenderer.ChunkInfo
        ) {
            this as AccessorWorldRenderer
            this as WorldRenderer

            val builtChunk = chunkInfo.chunk
            val direction = chunkInfo.direction
            val data = builtChunk.getData()

            for (nextDirection in DIRECTIONS) {
                if (chunkCulling) {
                    if (chunkInfo.canCull(nextDirection.opposite)) continue
                    if (direction != null && !data.isVisibleThrough(direction.opposite, nextDirection)) continue
                }

                val nextBuiltChunk = this.invokeGetAdjacentChunk(cameraChunkBlockPos, builtChunk, nextDirection)

                if (nextBuiltChunk != null
                    && nextBuiltChunk.shouldBuild()
                    && nextBuiltChunk.setRebuildFrame(frame)
                    && frustum.isVisible(nextBuiltChunk.boundingBox)
                ) {
                    val nextChunkInfo = this.ChunkInfo(nextBuiltChunk, nextDirection, chunkInfo.propagationLevel + 1)
                    nextChunkInfo.updateCullingState(chunkInfo.cullingState, nextDirection)
                    queue.add(nextChunkInfo)

                    if (counter.getAndUpdate(UPDATE_FUNCTION) > 0) {
                        scope.launch(FastMcCoreScope.context) {
                            recursiveSetupTerrainIteration(
                                scope,
                                frustum,
                                frame,
                                cameraChunkBlockPos,
                                chunkCulling,
                                queue,
                                counter,
                                nextChunkInfo
                            )
                            counter.getAndIncrement()
                        }
                    } else {
                        recursiveSetupTerrainIteration(
                            scope,
                            frustum,
                            frame,
                            cameraChunkBlockPos,
                            chunkCulling,
                            queue,
                            counter,
                            nextChunkInfo
                        )
                    }
                }
            }
        }

        private inline fun CoroutineScope.updateRegionVisibility(
            chunkArray: Array<ChunkBuilder.BuiltChunk>,
            visibleChunkBitSet: ExtendedBitSet,
            region: RenderRegion,
            layers: List<RenderLayer>
        ) {
            for (layerIndex in layers.indices) {
                val regionLayer = region.getRegionLayer(layerIndex) ?: continue
                launch(FastMcCoreScope.context) {
                    val firstList = IntArrayList()
                    val countList = IntArrayList()
                    var rendering = false
                    var pointer = 0
                    var first = 0
                    var count = 0

                    for (chunkIndex in regionLayer.chunkIndices) {
                        if (!regionLayer.chunkBitSet.containsInt(chunkIndex)) continue
                        val builtChunk = chunkArray[chunkIndex]
                        builtChunk as IPatchedBuiltChunk
                        val data = builtChunk.chunkVertexDataArray[layerIndex]

                        if (data != null) {
                            if (visibleChunkBitSet.containsInt(chunkIndex)) {
                                if (!rendering) {
                                    rendering = true
                                    first = pointer
                                }
                                count += data.vboInfo.vertexCount
                            } else {
                                if (rendering) {
                                    rendering = false
                                    firstList.add(first)
                                    countList.add(count)
                                    count = 0
                                }
                            }

                            pointer += data.vboInfo.vertexCount
                        }
                    }

                    if (rendering) {
                        firstList.add(first)
                        countList.add(count)
                    }

                    region.updateRegionLayerVisibility(layerIndex, firstList, countList)
                }
            }
        }

        private inline fun CoroutineScope.updateRegionVbo(
            mainThreadContext: CoroutineContext,
            chunkIndices: IntArray,
            chunkArray: Array<ChunkBuilder.BuiltChunk>,
            visibleChunkBitSet: ExtendedBitSet,
            region: RenderRegion,
            layers: List<RenderLayer>
        ) {
            for (layerIndex in layers.indices) {
                launch(FastMcCoreScope.context) {
                    val list = FastObjectArrayList<ChunkVertexData>()
                    val chunkBitSet = ExtendedBitSet()
                    val firstList = IntArrayList()
                    val countList = IntArrayList()
                    var rendering = false
                    var pointer = 0
                    var first = 0
                    var count = 0

                    chunkBitSet.ensureCapacity(chunkArray.size)

                    for (chunkIndex in chunkIndices) {
                        if (!region.chunks.containsInt(chunkIndex)) continue
                        val builtChunk = chunkArray[chunkIndex]
                        if (builtChunk.getData().isEmpty) continue

                        builtChunk as IPatchedBuiltChunk
                        val longOrigin = builtChunk.origin.asLong()
                        val data = builtChunk.chunkVertexDataArray[layerIndex]
                        if (data != null && data.builtOrigin == longOrigin) {
                            if (visibleChunkBitSet.containsInt(chunkIndex)) {
                                if (!rendering) {
                                    rendering = true
                                    first = pointer
                                }
                                count += data.vboInfo.vertexCount
                            } else {
                                if (rendering) {
                                    rendering = false
                                    firstList.add(first)
                                    countList.add(count)
                                    count = 0
                                }
                            }

                            chunkBitSet.addFast(chunkIndex)
                            list.add(data)
                            pointer += data.vboInfo.vertexCount
                        }
                    }

                    if (rendering) {
                        firstList.add(first)
                        countList.add(count)
                    }

                    if (list.isNotEmpty()) {
                        val vertexCount = list.sumOf {
                            it.vboInfo.vertexCount
                        }
                        val vertexSize = VertexDataTransformer.transformedSize(vertexCount)

                        withContext(mainThreadContext) {
                            region.updateRegionLayer(
                                mainThreadContext,
                                layerIndex,
                                list,
                                firstList,
                                countList,
                                vertexCount,
                                vertexSize,
                                chunkBitSet,
                                chunkIndices
                            )
                        }
                    }
                }
            }
        }
    }
}