package me.luna.fastmc.mixin

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import me.luna.fastmc.mixin.accessor.AccessorWorldRenderer
import me.luna.fastmc.shared.opengl.VboInfo
import me.luna.fastmc.shared.opengl.glCopyNamedBufferSubData
import me.luna.fastmc.shared.util.DoubleBufferedCollection
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.ParallelUtils
import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.terrain.ChunkVertexData
import me.luna.fastmc.terrain.RegionBuiltChunkStorage
import me.luna.fastmc.terrain.VertexDataTransformer
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.Frustum
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator
import kotlin.math.max

interface IPatchedWorldRenderer {
    var ticked: Boolean
    val renderTileEntityList: List<BlockEntity>
    val visibleChunksBitSet: DoubleBufferedCollection<ExtendedBitSet>

    fun recursiveSetupTerrainIteration(
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

        for (nextDirection in DIRECTIONS) {
            if (chunkCulling) {
                if (chunkInfo.canCull(nextDirection.opposite)) continue
                if (direction != null && !builtChunk.getData()
                        .isVisibleThrough(direction.opposite, nextDirection)
                ) continue
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

    suspend fun setupTerrainIteration0(
        scope: CoroutineScope,
        visibleChunks: FastObjectArrayList<WorldRenderer.ChunkInfo>,
        renderTileEntityList: FastObjectArrayList<BlockEntity>,
        chunks: RegionBuiltChunkStorage,
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

            launch(scope.coroutineContext) {
                val oldSet = visibleChunksBitSet.getAndSwap()
                val newSet = visibleChunksBitSet.get()
                val chunkArray = chunks.chunks
                newSet.clear()
                newSet.ensureCapacity(chunkArray.size)

                val layers = RenderLayer.getBlockLayers()
                val regionArray = chunks.regionArray
                val regionLayers = Array(regionArray.size) {
                    Array(layers.size) {
                        FastObjectArrayList<ChunkVertexData>()
                    }
                }

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
                    val region = builtChunk.region

                    if (list.isNotEmpty()) {
                        renderTileEntityList.addAll(list as ObjectArrayList<BlockEntity>)
                    }

                    if (!builtChunk.getData().isEmpty) {
                        region.visible = true
                    }

                    if (!builtChunk.needsRebuild()) {
                        val longOrigin = builtChunk.origin.asLong()
                        for (i in layers.indices) {
                            val dataArray = builtChunk.chunkVertexDataArray
                            dataArray[i]?.let {
                                if (it.builtOrigin == longOrigin) {
                                    regionLayers.getOrNull(region.index)?.get(i)?.add(it)
                                }
                            }
                        }
                    }

                    newSet.add(builtChunk.index)
                    visibleChunks.add(it)
                }

                for (i in chunkArray.indices) {
                    if (newSet.contains(i) != oldSet.contains(i)) {
                        (chunkArray[i] as IPatchedBuiltChunk).region.dirty = true
                    }
                }

                withContext(scope.coroutineContext) {
                    for (regionIndex in regionLayers.indices) {
                        val region = regionArray[regionIndex]
                        if (!region.dirty) continue
                        val array = regionLayers[regionIndex]

                        for (layerIndex in array.indices) {
                            val list = array[layerIndex]
                            if (list.isNotEmpty()) {
                                val vertexCount = list.sumOf {
                                    it.vboInfo.vertexCount
                                }
                                val vertexSize = VertexDataTransformer.transformedSize(vertexCount)
                                val newVboSize = (vertexSize + 1048575) shr 20 shl 20

                                region.updateRegionLayer(layerIndex, newVboSize) {
                                    var offset = 0L
                                    for (i in list.size - 1 downTo 0) {
                                        val data = list[i]
                                        glCopyNamedBufferSubData(
                                            data.vboInfo.vbo.id,
                                            it.id,
                                            0L,
                                            offset,
                                            data.vboInfo.vertexSize.toLong()
                                        )
                                        offset += data.vboInfo.vertexSize
                                    }
                                    VboInfo(it, vertexCount, vertexSize, newVboSize)
                                }
                            }
                        }

                        region.dirty = false
                    }
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
                this,
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
    }
}