package me.luna.fastmc.mixin

import it.unimi.dsi.fastutil.ints.IntArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.luna.fastmc.FastMcMod
import me.luna.fastmc.mixin.accessor.AccessorChunkBuilder
import me.luna.fastmc.mixin.accessor.AccessorWorldRenderer
import me.luna.fastmc.shared.util.*
import me.luna.fastmc.shared.util.DoubleBufferedCollection.Companion.emptyInitAction
import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.terrain.ChunkVertexData
import me.luna.fastmc.terrain.CullingInfo
import me.luna.fastmc.terrain.RegionBuiltChunkStorage
import me.luna.fastmc.terrain.RenderRegion
import me.luna.fastmc.util.isDoneOrNull
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.Camera
import net.minecraft.client.render.Frustum
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import java.util.concurrent.Future
import kotlin.coroutines.CoroutineContext

class PatchedWorldRenderer(private val thisRef: WorldRenderer) {
    val renderTileEntityList = DoubleBufferedCollection<FastObjectArrayList<BlockEntity>>(
        FastObjectArrayList(),
        FastObjectArrayList(),
        emptyInitAction()
    )
    val preLoadChunkBitSet = DoubleBufferedCollection(ExtendedBitSet(), ExtendedBitSet(), emptyInitAction())
    val visibleChunkBitSet = DoubleBufferedCollection(ExtendedBitSet(), ExtendedBitSet(), emptyInitAction())

    private val loadingTimer = TickTimer()

    private var lastRebuildTask: Future<*>? = null

    private var lastCameraX0 = Int.MAX_VALUE
    private var lastCameraY0 = Int.MAX_VALUE
    private var lastCameraZ0 = Int.MAX_VALUE
    private var lastCameraYaw0 = Int.MAX_VALUE
    private var lastCameraPitch0 = Int.MAX_VALUE

    fun clear() {
        renderTileEntityList.get().clearAndTrim()
        renderTileEntityList.getSwap().clearAndTrim()

        preLoadChunkBitSet.clearFastAll()
        visibleChunkBitSet.clearFastAll()

        lastCameraX0 = Int.MAX_VALUE
        lastCameraY0 = Int.MAX_VALUE
        lastCameraZ0 = Int.MAX_VALUE
        lastCameraYaw0 = Int.MAX_VALUE
        lastCameraPitch0 = Int.MAX_VALUE

        lastRebuildTask?.cancel(true)
        lastRebuildTask = null
    }

    private fun DoubleBufferedCollection<ExtendedBitSet>.clearFastAll() {
        get().clear()
        getSwap().clearFast()
    }

    fun setupTerrain0(camera: Camera, frustum: Frustum, hasForcedFrustum: Boolean, spectator: Boolean) {
        thisRef.setupTerrain0(camera, frustum, hasForcedFrustum, spectator)
    }

    private fun WorldRenderer.setupTerrain0(
        camera: Camera,
        frustum: Frustum,
        hasForcedFrustum: Boolean,
        spectator: Boolean
    ) {
        this as AccessorWorldRenderer

        if (client.options.viewDistance != viewDistance) {
            reload()
            return
        }

        client.profiler.push("pre")
        val chunkBuilder = chunkBuilder
        chunkBuilder as AccessorChunkBuilder
        chunkBuilder as IPatchedChunkBuilder

        val chunkStorage = chunks as RegionBuiltChunkStorage
        val player = client.player ?: return

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
                        chunkStorage.updateCameraPosition(cameraChunkOrigin, player.x, player.z)
                    }
                    lightUpdateDeferred.start()
                }

                client.profiler.swap("culling")
                var update = false

                needsTerrainUpdate = chunkStorage.updateCulling(cameraChunkOrigin) || needsTerrainUpdate
                needsTerrainUpdate = chunkStorage.checkChunkIndicesUpdate() || needsTerrainUpdate

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
                            val builtChunk = chunkStorage.getRenderedChunk(
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
                        client.chunkCullingEnabled
                            && !spectator
                            && !world.getBlockState(cameraBlockPos).isOpaqueFullCube(world, cameraBlockPos)
                    )

                    client.profiler.pop()
                }

                if (lastRebuildTask.isDoneOrNull
                    && chunkBuilder.queuedTaskCount < ParallelUtils.CPU_THREADS * 4
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
                    }

                    client.profiler.swap("updateChunks")
                    running[0] = false
                    updateChunksJob.join()

                    client.profiler.swap("scheduleRebuild")
                    scheduleRebuild()
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

    private fun WorldRenderer.scheduleRebuild() {
        this as AccessorWorldRenderer
        val visible = visibleChunkBitSet.get()
        val preLoad = preLoadChunkBitSet.get()
        val visibleNotEmpty = visible.isNotEmpty()
        val preLoadNotEmpty = preLoad.isNotEmpty()

        if (visibleNotEmpty || preLoadNotEmpty) {
            lastRebuildTask = FastMcCoreScope.pool.submit {
                val chunks = chunks as RegionBuiltChunkStorage
                val chunkIndices = chunks.chunkIndices
                val chunkArray = chunks.chunks
                var count = ParallelUtils.CPU_THREADS * 2

                if (visibleNotEmpty) {
                    visible.ensureArrayLength(chunkArray.size)
                    for (i in chunkIndices.size - 1 downTo 0) {
                        val chunkIndex = chunkIndices[i]
                        if (!visible.containsFast(chunkIndex)) continue

                        val builtChunk = chunkArray[chunkIndex]
                        if (!builtChunk.needsRebuild()) continue
                        val task = builtChunk.createRebuildTask()
                        builtChunk.cancelRebuild()
                        chunkBuilder.send(task)

                        if (--count <= 0) return@submit
                    }
                }

                count /= 2
                preLoad.ensureArrayLength(chunkArray.size)
                for (i in chunkIndices.size - 1 downTo 0) {
                    val chunkIndex = chunkIndices[i]
                    if (!preLoad.containsFast(chunkIndex)) continue

                    val builtChunk = chunkArray[chunkIndex]
                    if (!builtChunk.needsRebuild()) continue
                    val task = builtChunk.createRebuildTask()
                    builtChunk.cancelRebuild()
                    chunkBuilder.send(task)

                    if (--count <= 0) return@submit
                }
            }
        }
    }

    private fun WorldRenderer.setupTerrainCulling(
        scope: CoroutineScope,
        mainThreadContext: CoroutineContext,
        running: BooleanArray,
        updateChunksJob: Job,
        frustum: Frustum,
        caveCulling: Boolean,
    ): Job {
        this as AccessorWorldRenderer

        return scope.launch(FastMcCoreScope.context) {
            val chunks = chunks as RegionBuiltChunkStorage
            val channel = Channel<CullingInfo>(ParallelUtils.CPU_THREADS)

            launch(FastMcCoreScope.context) {
                handleCullingInfo(chunks, channel)
                running[0] = false
                updateChunksJob.join()
                updateRegion(this, mainThreadContext)
            }

            coroutineScope {
                for (region in chunks.regionArray) {
                    region.visible = false
                }
                val caveCullingBitSet = chunks.caveCullingBitSet
                cullingIteration(this, caveCulling, frustum, caveCullingBitSet, channel)
            }

            channel.close()
        }
    }

    private suspend fun WorldRenderer.handleCullingInfo(
        chunks: RegionBuiltChunkStorage,
        channel: Channel<CullingInfo>
    ) {
        this as AccessorWorldRenderer

        val chunkArray = chunks.chunks

        val preLoadChunkBitSet0 = preLoadChunkBitSet.getSwap()
        val renderTileEntityList0 = renderTileEntityList.getSwap()
        val visibleChunkBitSet0 = visibleChunkBitSet.getSwap()

        preLoadChunkBitSet0.clear()
        renderTileEntityList0.clear()
        visibleChunkBitSet0.clear()

        preLoadChunkBitSet0.ensureCapacity(chunkArray.size)
        visibleChunkBitSet0.ensureCapacity(chunkArray.size)

        var entityCapacity = world.blockEntities.size
        entityCapacity = MathUtils.ceilToPOT(entityCapacity + entityCapacity / 2)
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
    }

    private fun WorldRenderer.updateRegion(scope: CoroutineScope, mainThreadContext: CoroutineContext) {
        this as AccessorWorldRenderer

        val chunks = chunks as RegionBuiltChunkStorage
        val layers = RenderLayer.getBlockLayers()
        val regionArray = chunks.regionArray
        val chunkArray = chunks.chunks
        val chunkIndices = chunks.chunkIndices
        val visibleChunkBitSet = visibleChunkBitSet.get()

        for (regionIndex in regionArray.indices) {
            val region = regionArray[regionIndex]
            if (!region.visible) continue

            if (region.dirty.getAndSet(false)) {
                updateRegionVbo(scope, mainThreadContext, chunkArray, chunkIndices, visibleChunkBitSet, region, layers)
            } else {
                updateRegionVisibility(scope, visibleChunkBitSet, region, layers)
            }
        }
    }

    private fun WorldRenderer.cullingIteration(
        scope: CoroutineScope,
        caveCulling: Boolean,
        frustum: Frustum,
        caveCullingBitSet: ExtendedBitSet,
        channel: Channel<CullingInfo>
    ) {
        this as AccessorWorldRenderer

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

                            if (!builtChunk.shouldBuild()) continue
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

                            if (!builtChunk.shouldBuild()) continue
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

    private fun updateRegionVbo(
        scope: CoroutineScope,
        mainThreadContext: CoroutineContext,
        chunkArray: Array<ChunkBuilder.BuiltChunk>,
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
                    if (data != null && data.vboInfo.vertexCount != 0 && data.builtOrigin == longOrigin) {
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
                    region.updateRegionLayer(
                        mainThreadContext,
                        layerIndex,
                        list,
                        firstList,
                        countList,
                    )
                }
            }
        }
    }

    private fun updateRegionVisibility(
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
}