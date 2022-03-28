package me.luna.fastmc.mixin

import it.unimi.dsi.fastutil.ints.IntArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.luna.fastmc.FastMcMod
import me.luna.fastmc.mixin.accessor.AccessorChunkBuilder
import me.luna.fastmc.mixin.accessor.AccessorWorldRenderer
import me.luna.fastmc.shared.util.*
import me.luna.fastmc.shared.util.MathUtils.ceilToPOT
import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.terrain.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.Camera
import net.minecraft.client.render.Frustum
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import kotlin.coroutines.CoroutineContext

@Suppress("NOTHING_TO_INLINE")
interface IPatchedWorldRenderer {
    val renderTileEntityList: DoubleBufferedCollection<FastObjectArrayList<BlockEntity>>
    val preLoadChunkBitSet: DoubleBufferedCollection<ExtendedBitSet>
    val visibleChunkBitSet: DoubleBufferedCollection<ExtendedBitSet>

    var lastCameraX0: Int
    var lastCameraY0: Int
    var lastCameraZ0: Int
    var lastCameraYaw0: Int
    var lastCameraPitch0: Int
    val loadingTimer: TickTimer

    fun setupTerrain0(camera: Camera, frustum: Frustum, hasForcedFrustum: Boolean, frame: Int, spectator: Boolean) {
        this@IPatchedWorldRenderer as WorldRenderer
        this@IPatchedWorldRenderer as AccessorWorldRenderer

        client.profiler.push("pre")
        val chunkBuilder = chunkBuilder
        chunkBuilder as AccessorChunkBuilder
        chunkBuilder as IPatchedChunkBuilder

        val chunks = chunks as RegionBuiltChunkStorage
        val player = client.player ?: return

        if (client.options.viewDistance != viewDistance) {
            reload()
        }

        runBlocking {
            var cameraJob: Job
            val lightUpdateDeferred = async(FastMcCoreScope.context, CoroutineStart.LAZY) {
                (FastMcMod.worldRenderer as me.luna.fastmc.renderer.WorldRenderer).runLightUpdates()
            }
            var cullingJob: Job? = null

            withContext(FastMcCoreScope.context) {
                client.profiler.swap("updateChunks")
                val running = booleanArrayOf(true)
                val updateChunksJob = launch(this@runBlocking.coroutineContext) {
                    chunkBuilder.cameraPosition = camera.pos
                    needsTerrainUpdate = chunkBuilder.upload(running) || needsTerrainUpdate
                }


                world.profiler.swap("camera")
                val cameraBlockPos = camera.blockPos
                val cameraChunkOrigin = BlockPos(
                    cameraBlockPos.x shr 4 shl 4,
                    cameraBlockPos.y shr 4 shl 4,
                    cameraBlockPos.z shr 4 shl 4
                )
                cameraJob = launch(FastMcCoreScope.context) {
                    val cameraUpdateDeltaX = player.x - lastCameraChunkUpdateX
                    val cameraUpdateDeltaY = player.y - lastCameraChunkUpdateY
                    val cameraUpdateDeltaZ = player.z - lastCameraChunkUpdateZ
                    if (cameraChunkX != player.chunkX || cameraChunkY != player.chunkY || cameraChunkZ != player.chunkZ
                        || cameraUpdateDeltaX * cameraUpdateDeltaX + cameraUpdateDeltaY * cameraUpdateDeltaY + cameraUpdateDeltaZ * cameraUpdateDeltaZ > 16.0
                    ) {
                        lastCameraChunkUpdateX = player.x
                        lastCameraChunkUpdateY = player.y
                        lastCameraChunkUpdateZ = player.z
                        cameraChunkX = player.chunkX
                        cameraChunkY = player.chunkY
                        cameraChunkZ = player.chunkZ
                        chunks.updateCameraPosition(
                            cameraChunkOrigin,
                            player.x,
                            player.z
                        )
                    }
                    lightUpdateDeferred.start()
                }

                client.profiler.swap("culling")
                var update = false

                needsTerrainUpdate = chunks.updateCulling(cameraChunkOrigin, frame) || needsTerrainUpdate

                if (!hasForcedFrustum) {
                    val cameraPosX0 = cameraBlockPos.x shr 1
                    val cameraPosY0 = cameraBlockPos.y shr 1
                    val cameraPosZ0 = cameraBlockPos.z shr 1
                    val cameraYaw0 = MathHelper.floor(camera.yaw)
                    val cameraPitch0 = camera.pitch.fastFloor()

                    if (cameraPosX0 != lastCameraX0 || cameraPosY0 != lastCameraY0 || cameraPosZ0 != lastCameraZ0 || cameraYaw0 != lastCameraYaw0 || cameraPitch0 != lastCameraPitch0) {
                        lastCameraX0 = cameraPosX0
                        lastCameraY0 = cameraPosY0
                        lastCameraZ0 = cameraPosZ0
                        lastCameraYaw0 = cameraYaw0
                        lastCameraPitch0 = cameraPitch0
                        loadingTimer.reset()
                        update = true
                    } else if (needsTerrainUpdate && loadingTimer.tick(250L)) {
                        lastCameraX0 = cameraPosX0
                        lastCameraY0 = cameraPosY0
                        lastCameraZ0 = cameraPosZ0
                        lastCameraYaw0 = cameraYaw0
                        lastCameraPitch0 = cameraPitch0
                        update = true
                    }
                }

                if (update) {
                    needsTerrainUpdate = false

                    Entity.setRenderDistanceMultiplier(
                        MathHelper.clamp(
                            client.options.viewDistance / 8.0,
                            1.0,
                            2.5
                        ) * client.options.entityDistanceScaling
                    )

                    client.profiler.swap("camera")
                    cameraJob.join()

                    client.profiler.swap("lightUpdates")
                    lightUpdateDeferred.await()?.let { list ->
                        for (i in list.indices) {
                            val longPos = list.getLong(i)
                            val builtChunk = chunks.getRenderedChunk(
                                BlockPos.unpackLongX(longPos),
                                BlockPos.unpackLongY(longPos),
                                BlockPos.unpackLongZ(longPos)
                            ) ?: continue
                            builtChunk.scheduleRebuild(false)
                        }
                    }

                    client.profiler.push("iteration")
                    cullingJob = setupTerrainCulling(
                        this@runBlocking,
                        this@runBlocking.coroutineContext,
                        running,
                        updateChunksJob,
                        frustum,
                        cameraChunkOrigin,
                        client.chunkCullingEnabled
                            && !spectator
                            && !world.getBlockState(cameraBlockPos).isOpaqueFullCube(world, cameraBlockPos)
                    )

                    client.profiler.pop()
                }

                if (chunkBuilder.queuedTaskCount < ParallelUtils.CPU_THREADS * 4
                    && chunkBuilder.uploadQueue.size < ParallelUtils.CPU_THREADS * 4
                ) {
                    cullingJob?.let {
                        client.profiler.swap("culling")
                        client.profiler.push("iteration")
                        it.join()
                        client.profiler.pop()
                    } ?: run {
                        client.profiler.swap("camera")
                        cameraJob.join()
                        chunks.getChunkIndices()
                    }

                    client.profiler.swap("updateChunks")
                    running[0] = false
                    updateChunksJob.join()

                    client.profiler.swap("scheduleRebuild")
                    scheduleRebuild(cameraBlockPos)
                }

                cullingJob?.let {
                    client.profiler.swap("culling")
                    client.profiler.push("iteration")
                    it.join()
                    client.profiler.pop()
                }

                client.profiler.swap("post")
            }

            client.profiler.pop()
        }
    }

    suspend fun scheduleRebuild(cameraBlockPos: BlockPos) {
        this as AccessorWorldRenderer
        val visible = visibleChunkBitSet.get()
        val preLoad = preLoadChunkBitSet.get()
        val visibleNotEmpty = visible.isNotEmpty()
        val preLoadNotEmpty = preLoad.isNotEmpty()

        if (visibleNotEmpty || preLoadNotEmpty) {
            FastMcCoreScope.launch {
                val chunks = chunks as RegionBuiltChunkStorage
                val chunkIndices = chunks.getChunkIndices()
                val chunkArray = chunks.chunks

                if (visibleNotEmpty) {
                    var count = ParallelUtils.CPU_THREADS * 2
                    visible.ensureArrayLength(chunkArray.size)
                    for (i in chunkIndices.size - 1 downTo 0) {
                        val chunkIndex = chunkIndices[i]
                        if (!visible.containsFast(chunkIndex)) continue

                        val builtChunk = chunkArray[chunkIndex]
                        if (!builtChunk.needsRebuild()) continue
                        val task = builtChunk.createRebuildTask()
                        builtChunk.cancelRebuild()
                        chunkBuilder.send(task)

                        if (--count <= 0) return@launch
                    }
                } else {
                    var count = ParallelUtils.CPU_THREADS
                    preLoad.ensureArrayLength(chunkArray.size)
                    for (i in chunkIndices.size - 1 downTo 0) {
                        val chunkIndex = chunkIndices[i]
                        if (!preLoad.containsFast(chunkIndex)) continue

                        val builtChunk = chunkArray[chunkIndex]
                        if (!builtChunk.needsRebuild()) continue
                        val task = builtChunk.createRebuildTask()
                        builtChunk.cancelRebuild()
                        chunkBuilder.send(task)

                        if (--count <= 0) return@launch
                    }
                }
            }
        }
    }

    private fun setupTerrainCulling(
        scope: CoroutineScope,
        mainThreadContext: CoroutineContext,
        running: BooleanArray,
        updateChunksJob: Job,
        frustum: Frustum,
        cameraChunkOrigin: BlockPos,
        caveCulling: Boolean,
    ): Job {
        this as AccessorWorldRenderer
        this as WorldRenderer
        val chunks = chunks as RegionBuiltChunkStorage

        return scope.launch(FastMcCoreScope.context) {
            val channel = Channel<CullingInfo>(ParallelUtils.CPU_THREADS)

            launch(FastMcCoreScope.context) {
                val layers = RenderLayer.getBlockLayers()
                val chunkArray: Array<ChunkBuilder.BuiltChunk> = chunks.chunks
                val regionArray = chunks.regionArray

                val preLoadChunkBitSet0 = preLoadChunkBitSet.getSwap()
                val renderTileEntityList0 = renderTileEntityList.getSwap()
                val visibleChunkBitSet0 = visibleChunkBitSet.getSwap()

                preLoadChunkBitSet0.clear()
                renderTileEntityList0.clear()
                visibleChunkBitSet0.clear()

                preLoadChunkBitSet0.ensureCapacity(chunkArray.size)
                visibleChunkBitSet0.ensureCapacity(chunkArray.size)

                var entityCapacity = world.blockEntities.size
                entityCapacity = ceilToPOT(entityCapacity + entityCapacity / 2)
                if (renderTileEntityList0.capacity > entityCapacity shl 1) {
                    renderTileEntityList0.trim(entityCapacity)
                } else {
                    renderTileEntityList0.ensureCapacity(entityCapacity)
                }

                for (info in channel) {
                    preLoadChunkBitSet0.addAll(info.preLoad)
                    visibleChunkBitSet0.addAll(info.visible)
                    renderTileEntityList0.addAll(info.tileEntity)
                }

                preLoadChunkBitSet.getAndSwap()
                renderTileEntityList.getAndSwap()
                visibleChunkBitSet.getAndSwap()

                running[0] = false
                updateChunksJob.join()
                val chunkIndices = chunks.getChunkIndices()

                updateRegion(
                    this,
                    regionArray,
                    mainThreadContext,
                    chunkArray,
                    chunkIndices,
                    visibleChunkBitSet0,
                    layers
                )
            }

            coroutineScope {
                for (region in chunks.regionArray) {
                    region.visible = false
                }

                val nonEmptyChunkSet = NonEmptyChunkSet(world, viewDistance, cameraChunkOrigin)
                val caveCullingBitSet = chunks.getCaveCullingBitSet()

                cullingIteration(this, caveCulling, frustum, nonEmptyChunkSet, caveCullingBitSet, channel)
            }

            channel.close()
        }
    }

    fun updateRegion(
        scope: CoroutineScope,
        regionArray: Array<RenderRegion>,
        mainThreadContext: CoroutineContext,
        chunkArray: Array<BuiltChunk>,
        chunkIndices: IntArray,
        visibleChunkBitSet0: ExtendedBitSet,
        layers: MutableList<RenderLayer>
    ) {
        for (regionIndex in regionArray.indices) {
            val region = regionArray[regionIndex]
            if (region.dirty.getAndSet(false)) {
                updateRegionVbo(scope, mainThreadContext, chunkArray, chunkIndices, visibleChunkBitSet0, region, layers)
            } else if (region.visible) {
                updateRegionVisibility(scope, visibleChunkBitSet0, region, layers)
            }
        }
    }

    private fun cullingIteration(
        scope: CoroutineScope,
        caveCulling: Boolean,
        frustum: Frustum,
        nonEmptyChunkSet: NonEmptyChunkSet,
        caveCullingBitSet: ExtendedBitSet,
        channel: Channel<CullingInfo>
    ) {
        this as AccessorWorldRenderer
        this as WorldRenderer

        val chunkArray = chunks.chunks

        if (caveCulling) {
            ParallelUtils.splitListIndex(
                total = chunkArray.size,
                parallelism = ParallelUtils.CPU_THREADS,
                blockForEach = { start, end ->
                    scope.launch(FastMcCoreScope.context) {
                        val cullingInfo = CullingInfo(chunkArray.size)

                        for (i in start until end) {
                            val builtChunk = chunkArray[i]
                            builtChunk as IPatchedBuiltChunk
                            val index = builtChunk.index

                            if (!caveCullingBitSet.containsFast(index)) continue
                            cullingInfo.preLoad.addFast(index)

                            if (!builtChunk.shouldBuild(nonEmptyChunkSet)) continue
                            if (!frustum.isVisible(builtChunk.boundingBox)) continue

                            cullingInfo.visible.addFast(index)
                            builtChunk.region.visible = true
                        }

                        channel.trySend(cullingInfo)
                    }
                }
            )
        } else {
            ParallelUtils.splitListIndex(
                total = chunkArray.size,
                parallelism = ParallelUtils.CPU_THREADS,
                blockForEach = { start, end ->
                    scope.launch(FastMcCoreScope.context) {
                        val cullingInfo = CullingInfo(chunkArray.size)

                        for (i in start until end) {
                            val builtChunk = chunkArray[i]
                            builtChunk as IPatchedBuiltChunk
                            val index = builtChunk.index

                            cullingInfo.preLoad.addFast(index)

                            if (!builtChunk.shouldBuild(nonEmptyChunkSet)) continue
                            if (!frustum.isVisible(builtChunk.boundingBox)) continue

                            cullingInfo.visible.addFast(index)
                            builtChunk.region.visible = true
                        }

                        channel.trySend(cullingInfo)
                    }
                }
            )
        }
    }

    private inline fun updateRegionVbo(
        scope: CoroutineScope,
        mainThreadContext: CoroutineContext,
        chunkArray: Array<BuiltChunk>,
        chunkIndices: IntArray,
        visibleChunkBitSet: ExtendedBitSet,
        region: RenderRegion,
        layers: List<RenderLayer>
    ) {
        for (layerIndex in layers.indices) {
            scope.launch(FastMcExtendScope.context) {
                val list = FastObjectArrayList<ChunkVertexData>()
                val size = visibleChunkBitSet.size
                val firstList = IntArrayList(size)
                val countList = IntArrayList(size)
                var rendering = false
                var pointer = 0
                var first = 0
                var count = 0

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
                            layerIndex,
                            list,
                            firstList,
                            countList,
                            vertexCount,
                            vertexSize
                        )
                    }
                }
            }
        }
    }

    private inline fun updateRegionVisibility(
        scope: CoroutineScope,
        visibleChunkBitSet: ExtendedBitSet,
        region: RenderRegion,
        layers: List<RenderLayer>
    ) {
        for (layerIndex in layers.indices) {
            val regionLayer = region.getRegionLayer(layerIndex) ?: continue
            scope.launch(FastMcExtendScope.context) {
                val size = visibleChunkBitSet.size
                val firstList = IntArrayList(size)
                val countList = IntArrayList(size)
                var rendering = false
                var pointer = 0
                var first = 0
                var count = 0

                for (i in regionLayer.dataList.indices) {
                    val data = regionLayer.dataList[i]

                    if (visibleChunkBitSet.containsFast(data.chunkIndex)) {
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

                if (rendering) {
                    firstList.add(first)
                    countList.add(count)
                }

                region.updateRegionLayerVisibility(layerIndex, firstList, countList)
            }
        }
    }

    private inline fun ChunkBuilder.BuiltChunk.shouldBuild(
        nonEmptyChunkSet: NonEmptyChunkSet,
    ): Boolean {
        val chunkX = origin.x shr 4
        val chunkZ = origin.z shr 4

        return nonEmptyChunkSet.isNotEmpty(chunkX, chunkZ + 1)
            && nonEmptyChunkSet.isNotEmpty(chunkX - 1, chunkZ)
            && nonEmptyChunkSet.isNotEmpty(chunkX, chunkZ - 1)
            && nonEmptyChunkSet.isNotEmpty(chunkX + 1, chunkZ)
    }
}