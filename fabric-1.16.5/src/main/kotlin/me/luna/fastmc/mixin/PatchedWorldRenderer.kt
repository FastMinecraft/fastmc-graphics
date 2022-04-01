package me.luna.fastmc.mixin

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.luna.fastmc.mixin.accessor.AccessorChunkBuilder
import me.luna.fastmc.mixin.accessor.AccessorWorldRenderer
import me.luna.fastmc.shared.FpsDisplay
import me.luna.fastmc.shared.util.*
import me.luna.fastmc.shared.util.DoubleBufferedCollection.Companion.emptyInitAction
import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.terrain.ChunkVertexData
import me.luna.fastmc.terrain.CullingInfo
import me.luna.fastmc.terrain.RegionBuiltChunkStorage
import me.luna.fastmc.terrain.RenderRegion
import me.luna.fastmc.util.OffThreadLightingProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.Camera
import net.minecraft.client.render.Frustum
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.LightType
import net.minecraft.world.World
import net.minecraft.world.chunk.ChunkStatus
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

@Suppress("NOTHING_TO_INLINE")
class PatchedWorldRenderer(private val thisRef: WorldRenderer) {
    val renderTileEntityList = DoubleBufferedCollection<FastObjectArrayList<BlockEntity>>(
        FastObjectArrayList(),
        FastObjectArrayList(),
        emptyInitAction()
    )
    val preLoadChunkBitSet = DoubleBufferedCollection(ExtendedBitSet(), ExtendedBitSet(), emptyInitAction())
    val visibleChunkBitSet = DoubleBufferedCollection(ExtendedBitSet(), ExtendedBitSet(), emptyInitAction())
    val updatingChunkBitSet = DoubleBufferedCollection(ExtendedBitSet(), ExtendedBitSet())

    private val loadingTimer = TickTimer()
    private val lightUpdate = Long2LongLinkedOpenHashMap()
    private val pendingSkyLightUpdate = LongArrayList()
    private val pendingBlockLightUpdate = LongArrayList()
    private val updating = AtomicBoolean(true)

    private var lastRebuildTask: Job? = null

    private var lastCameraX0 = Int.MAX_VALUE
    private var lastCameraY0 = Int.MAX_VALUE
    private var lastCameraZ0 = Int.MAX_VALUE
    private var lastCameraYaw0 = Int.MAX_VALUE
    private var lastCameraPitch0 = Int.MAX_VALUE

    private val uploadTimeoutTimer = TickTimer()
    private var uploadCounter = 0

    fun clear() {
        renderTileEntityList.get().clearAndTrim()
        renderTileEntityList.getSwap().clearAndTrim()

        preLoadChunkBitSet.clearFastAll()
        visibleChunkBitSet.clearFastAll()
        updatingChunkBitSet.clearFastAll()

        lastCameraX0 = Int.MAX_VALUE
        lastCameraY0 = Int.MAX_VALUE
        lastCameraZ0 = Int.MAX_VALUE
        lastCameraYaw0 = Int.MAX_VALUE
        lastCameraPitch0 = Int.MAX_VALUE
        uploadCounter = 0

        lastRebuildTask?.cancel()
        lastRebuildTask = null

        lightUpdate.clear()
        pendingSkyLightUpdate.clear()
        pendingBlockLightUpdate.clear()
        updating.set(false)
    }

    fun resize(capacity: Int) {
        preLoadChunkBitSet.ensureCapacityAll(capacity)
        visibleChunkBitSet.ensureCapacityAll(capacity)
        updatingChunkBitSet.ensureCapacityAll(capacity)
    }

    private inline fun DoubleBufferedCollection<ExtendedBitSet>.ensureCapacityAll(capacity: Int) {
        get().ensureCapacity(capacity)
        getSwap().ensureCapacity(capacity)
    }

    private inline fun DoubleBufferedCollection<ExtendedBitSet>.clearFastAll() {
        get().clear()
        getSwap().clearFast()
    }

    fun scheduleLightUpdate(type: LightType, pos: ChunkSectionPos) {
        val longPos = BlockPos.asLong(pos.sectionX, pos.sectionY, pos.sectionZ)
        val list = when (type) {
            LightType.SKY -> pendingSkyLightUpdate
            LightType.BLOCK -> pendingBlockLightUpdate
        }
        list.add(longPos)
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
            withContext(FastMcCoreScope.context) {
                var cullingJob: Job? = null

                client.profiler.swap("updateChunks")
                val running = booleanArrayOf(true)
                val updateChunksJob = launch(this@runBlocking.coroutineContext) {
                    chunkBuilder.cameraPosition = camera.pos
                    val uploadCount = chunkBuilder.upload(running)
                    FpsDisplay.onChunkUpdate(uploadCount)

                    uploadCounter += uploadCount
                    if (uploadCounter >= ParallelUtils.CPU_THREADS * 32
                        || uploadCounter != 0 && (uploadTimeoutTimer.tick(250L)
                        || visibleChunkBitSet.get().size < ParallelUtils.CPU_THREADS * 4)) {
                        uploadCounter = 0
                        needsTerrainUpdate = true
                        uploadTimeoutTimer.reset()
                        loadingTimer.reset(-999L)
                    }
                }

                world.profiler.swap("camera")
                val cameraBlockPos = camera.blockPos
                val cameraChunkOrigin = BlockPos(
                    cameraBlockPos.x shr 4 shl 4,
                    cameraBlockPos.y shr 4 shl 4,
                    cameraBlockPos.z shr 4 shl 4
                )

                val updateCamera = cameraChunkX != player.chunkX
                    || cameraChunkY != player.chunkY
                    || cameraChunkZ != player.chunkZ
                    || distanceSq(
                    player.x, player.y, player.z,
                    lastCameraChunkUpdateX, lastCameraChunkUpdateY, lastCameraChunkUpdateZ
                ) > 16.0

                if (updateCamera) {
                    lastCameraChunkUpdateX = player.x
                    lastCameraChunkUpdateY = player.y
                    lastCameraChunkUpdateZ = player.z
                    cameraChunkX = player.chunkX
                    cameraChunkY = player.chunkY
                    cameraChunkZ = player.chunkZ
                    chunkStorage.updateCameraPosition(cameraChunkOrigin, player.x, player.z)
                } else {
                    client.profiler.swap("culling")
                    var update = false

                    needsTerrainUpdate = chunkStorage.checkCaveCullingUpdate() || needsTerrainUpdate
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

                        needsTerrainUpdate = false

                        Entity.setRenderDistanceMultiplier(
                            MathHelper.clamp(
                                client.options.viewDistance / 8.0,
                                1.0,
                                2.5
                            ) * client.options.entityDistanceScaling
                        )

                        chunkStorage.updateCaveCulling(cameraChunkOrigin)
                    }

                    client.profiler.swap("lightUpdate")
                    if (lastRebuildTask.isCompletedOrNull && chunkBuilder.queuedTaskCount < ParallelUtils.CPU_THREADS * 8) {
                        runLightUpdate()

                        client.profiler.swap("translucentSort")
                        sortTranslucent(camera.pos)

                        client.profiler.swap("scheduleRebuild")
                        scheduleRebuild()
                    } else {
                        runLightUpdate()
                    }

                    cullingJob?.let {
                        client.profiler.swap("culling")
                        it.join()
                    }
                }

                client.profiler.swap("updateChunks")
                running[0] = false
                updateChunksJob.join()

                client.profiler.swap("post")
            }

            client.profiler.pop()
        }
    }

    private fun AccessorWorldRenderer.runLightUpdate() {
        if (updating.getAndSet(false)) {
            val time = System.currentTimeMillis() + 30000L

            for (i in pendingSkyLightUpdate.indices) {
                lightUpdate.put(pendingSkyLightUpdate.getLong(i), time)
            }
            pendingSkyLightUpdate.clear()

            for (i in pendingBlockLightUpdate.indices) {
                lightUpdate.put(pendingBlockLightUpdate.getLong(i), time)
            }
            pendingBlockLightUpdate.clear()

            val provider = world.chunkManager.lightingProvider as OffThreadLightingProvider
            provider.scheduleUpdate {
                provider.doLightUpdates()
                updating.set(true)
            }
        }
    }

    @Suppress("DuplicatedCode")
    private fun WorldRenderer.scheduleRebuild() {
        this as AccessorWorldRenderer

        val visible = visibleChunkBitSet.get()
        val preLoad = preLoadChunkBitSet.get()
        val updating = updatingChunkBitSet.swapAndGet()

        lastRebuildTask = FastMcCoreScope.launch {
            val chunkStorage = chunks as RegionBuiltChunkStorage
            val chunkBuilder = chunkBuilder

            if (lightUpdate.isNotEmpty()) {
                val iterator = lightUpdate.long2LongEntrySet().iterator()
                val current = System.currentTimeMillis()

                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (current >= entry.longValue) {
                        iterator.remove()
                        continue
                    }

                    val longPos = entry.longKey
                    val x = BlockPos.unpackLongX(longPos)
                    val y = BlockPos.unpackLongY(longPos)
                    val z = BlockPos.unpackLongZ(longPos)
                    val builtChunk = chunkStorage.getRenderedChunk(x, y, z)

                    if (builtChunk == null || builtChunk.origin.x shr 4 != x || builtChunk.origin.y shr 4 != y || builtChunk.origin.z shr 4 != z) {
                        iterator.remove()
                    } else if (!builtChunk.getData().isEmpty) {
                        updating.addFast((builtChunk as IPatchedBuiltChunk).index)
                        chunkBuilder.scheduleRebuild(builtChunk)
                        iterator.remove()
                    }
                }
            }

            val chunkIndices = chunkStorage.chunkIndices
            val chunkArray = chunkStorage.chunks

            var count = ParallelUtils.CPU_THREADS * 2
            for (i in chunkIndices.size - 1 downTo 0) {
                val chunkIndex = chunkIndices[i]
                if (!visible.containsFast(chunkIndex)) continue

                val builtChunk = chunkArray[chunkIndex]
                if (!builtChunk.needsRebuild()) continue
                if (updating.addFastCheck((builtChunk as IPatchedBuiltChunk).index)) {
                    chunkBuilder.scheduleRebuild(builtChunk)
                }

                if (--count == 0) return@launch
            }

            count /= 2
            if (count == 0) return@launch
            for (i in chunkIndices.size - 1 downTo 0) {
                val chunkIndex = chunkIndices[i]
                if (!preLoad.containsFast(chunkIndex)) continue

                val builtChunk = chunkArray[chunkIndex]
                if (!builtChunk.needsRebuild()) continue
                if (!updating.containsFast((builtChunk as IPatchedBuiltChunk).index)) {
                    chunkBuilder.scheduleRebuild(builtChunk)
                }

                if (--count == 0) return@launch
            }
        }
    }

    private fun ChunkBuilder.scheduleRebuild(builtChunk: BuiltChunk) {
        val task = builtChunk.createRebuildTask()
        builtChunk.cancelRebuild()
        send(task)
    }

    private fun WorldRenderer.sortTranslucent(cameraPos: Vec3d) {
        this as AccessorWorldRenderer

        if (cameraPos.squaredDistanceTo(lastTranslucentSortX, lastTranslucentSortY, lastTranslucentSortZ) > 1.0) {
            lastTranslucentSortX = cameraPos.x
            lastTranslucentSortY = cameraPos.y
            lastTranslucentSortZ = cameraPos.z
            val chunkStorage = chunks as RegionBuiltChunkStorage
            val chunkBuilder = this.chunkBuilder
            val visibleChunkBitSet = visibleChunkBitSet.get()
            val chunkIndices = chunkStorage.chunkIndices
            val layer = RenderLayer.getTranslucent()
            var count = 0

            for (i in chunkIndices.size - 1 downTo 0) {
                val chunkIndex = chunkIndices[i]
                if (!visibleChunkBitSet.containsFast(chunkIndex)) continue
                if (count >= 16) break
                if (chunkStorage.chunks[chunkIndex].scheduleSort(layer, chunkBuilder)) {
                    ++count
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
        cameraChunkOrigin: BlockPos,
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
                cullingIteration(this, caveCulling, frustum, cameraChunkOrigin, caveCullingBitSet, channel)
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
        cameraChunkOrigin: BlockPos,
        caveCullingBitSet: ExtendedBitSet,
        channel: Channel<CullingInfo>
    ) {
        this as AccessorWorldRenderer

        val chunkArray = chunks.chunks
        val world = world

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

                            if (!builtChunk.shouldBuild(world, cameraChunkOrigin)) continue
                            if (!frustum.isVisible(builtChunk.boundingBox)) continue

                            cullingInfo.visible.addFast(index)
                            if (!builtChunk.getData().isEmpty) {
                                builtChunk.region.visible = true
                            }
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
                            if (!builtChunk.getData().isEmpty) {
                                builtChunk.region.visible = true
                            }
                        }

                        channel.trySend(cullingInfo)
                    }
                }
            )
        }
    }

    private inline fun ChunkBuilder.BuiltChunk.shouldBuild(
        world: World,
        cameraChunkOrigin: BlockPos,
    ): Boolean {
        val x = origin.x - cameraChunkOrigin.x
        val y = origin.y - cameraChunkOrigin.y
        val z = origin.z - cameraChunkOrigin.z
        if (x * x + y * y + z * z <= 576) return true

        val chunkX = origin.x shr 4
        val chunkZ = origin.z shr 4

        return world.getChunk(chunkX, chunkZ + 1, ChunkStatus.FULL, false) != null
            && world.getChunk(chunkX - 1, chunkZ, ChunkStatus.FULL, false) != null
            && world.getChunk(chunkX, chunkZ - 1, ChunkStatus.FULL, false) != null
            && world.getChunk(chunkX + 1, chunkZ, ChunkStatus.FULL, false) != null
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

                    builtChunk as IPatchedBuiltChunk
                    val data = builtChunk.chunkVertexDataArray[layerIndex]
                    if (data != null) {
                        val longOrigin = builtChunk.origin.asLong()
                        if (data.vboInfo.vertexCount != 0 && data.builtOrigin == longOrigin) {
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