@file:Suppress("NOTHING_TO_INLINE")

package me.luna.fastmc.shared.terrain

import it.unimi.dsi.fastutil.objects.ObjectCollections
import kotlinx.coroutines.*
import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo
import me.luna.fastmc.shared.renderer.cameraChunkX
import me.luna.fastmc.shared.renderer.cameraChunkY
import me.luna.fastmc.shared.renderer.cameraChunkZ
import me.luna.fastmc.shared.util.*
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import java.lang.ref.WeakReference
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator
import kotlin.coroutines.cancellation.CancellationException

sealed class ChunkBuilderTask(val renderer: TerrainRenderer, val scheduler: ChunkBuilder.TaskScheduler) :
    Cancellable {
    @JvmField
    internal var renderChunkNullable: RenderChunk? = null
    val renderChunk get() = renderChunkNullable!!

    private val id0 = AtomicInteger(nextId())
    val id get() = id0.get()

    private var job: Job? = null
    private var thread: Thread? = null
    private val resources = FastObjectArrayList<Context>()

    var relativeCameraX = Float.MAX_VALUE; private set
    var relativeCameraY = Float.MAX_VALUE; private set
    var relativeCameraZ = Float.MAX_VALUE; private set

    var originX = Int.MAX_VALUE; private set
    var originY = Int.MAX_VALUE; private set
    var originZ = Int.MAX_VALUE; private set

    val chunkX get() = originX shr 4
    val chunkY get() = originY shr 4
    val chunkZ get() = originZ shr 4

    override val isCancelled get() = (renderChunkNullable?.isDestroyed ?: false) || (job?.isCancelled ?: false)
    val isCompleted get() = job.isCompletedOrNull

    init {
        @Suppress("LeakingThis")
        globalTaskList.add(WeakReference(this))
    }

    internal fun init(renderChunk: RenderChunk): Boolean {
        return try {
            init0(renderChunk)
            true
        } catch (e: CancellationException) {
            reset()
            false
        }
    }

    protected open fun init0(renderChunk: RenderChunk) {
        this.renderChunkNullable = renderChunk
        id0.set(nextId())

        relativeCameraX = (renderer.cameraX - renderChunk.renderRegion.originX).toFloat()
        relativeCameraY = (renderer.cameraY - renderChunk.renderRegion.originY).toFloat()
        relativeCameraZ = (renderer.cameraZ - renderChunk.renderRegion.originZ).toFloat()

        originX = renderChunk.originX
        originY = renderChunk.originY
        originZ = renderChunk.originZ
    }

    open fun reset() {
        this.renderChunkNullable = null

        job = null
        resources.clear()

        relativeCameraX = Float.MAX_VALUE
        relativeCameraY = Float.MAX_VALUE
        relativeCameraZ = Float.MAX_VALUE

        originX = Int.MAX_VALUE
        originY = Int.MAX_VALUE
        originZ = Int.MAX_VALUE
    }

    internal fun registerResource(resource: Context) {
        resources.add(resource)
    }

    internal fun releaseResource() {
        for (i in resources.indices) {
            resources[i].release(this@ChunkBuilderTask)
        }
    }

    internal fun run() {
        job = scope.launch(start = CoroutineStart.LAZY) {
            val renderChunk = renderChunk
            scheduler.onTaskStart(this@ChunkBuilderTask, renderChunk)
            try {
                checkCancelled()
                run0()
            } catch (e: TaskFinishedException) {
                //
            } catch (e: CancellationException) {
                onFinish()
                throw e
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        job!!.start()
    }

    protected abstract suspend fun run0()

    internal fun onUpload() {
        scheduler.onTaskUpload()
    }

    internal fun onFinish() {
        scheduler.onTaskFinish(this)
    }

    fun checkCancelled() {
        job?.ensureActive()
    }

    internal fun cancel() {
        job?.cancel()
    }

    internal fun finish() {
        throw TaskFinishedException
    }

    protected object CancelInitException : CancellationException() {
        init {
            stackTrace = emptyArray()
        }
    }

    private object TaskFinishedException : RuntimeException() {
        init {
            stackTrace = emptyArray()
        }
    }

    companion object {
        private val scope = CoroutineScope(ThreadPoolExecutor(
            ParallelUtils.CPU_THREADS,
            ParallelUtils.CPU_THREADS,
            114514,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            object : ThreadFactory {
                private val counter = AtomicInteger(0)
                private val group = ThreadGroup(threadGroupMain, "ChunkBuilder")

                override fun newThread(r: Runnable): Thread {
                    return Thread(group, r, "FastMinecraft-ChunkBuilder-${counter.incrementAndGet()}").apply {
                        priority = 2
                    }
                }
            }
        ).asCoroutineDispatcher())

        private val globalTaskList =
            ObjectCollections.synchronize(FastObjectArrayList<WeakReference<ChunkBuilderTask>>())

        @JvmStatic
        fun cancelAllAndJoin() {
            val tempList = FastObjectArrayList<ChunkBuilderTask>()

            synchronized(globalTaskList) {
                globalTaskList.removeIf {
                    val task = it.get()
                    if (task == null) {
                        true
                    } else {
                        task.cancel()
                        tempList.add(task)
                        false
                    }
                }
            }

            if (tempList.isNotEmpty()) {
                runBlocking {
                    val job = launch(FastMcCoreScope.context) {
                        for (i in tempList.indices) {
                            val task = tempList[i]
                            task.job?.cancelAndJoin()
                        }
                    }
                    while (job.isActive) {
                        FastMcMod.worldRenderer.terrainRenderer.contextProvider.update()
                    }
                }
            }
        }

        private val idCounter = AtomicInteger(Int.MIN_VALUE + 1)
        private val updateFunc = IntUnaryOperator {
            var value = it + 1
            if (value == Int.MIN_VALUE) {
                value++
            }
            value
        }

        @JvmStatic
        private fun nextId(): Int {
            return idCounter.getAndUpdate(updateFunc)
        }

        @JvmStatic
        fun unsynchronizedComparator(): Comparator<ChunkBuilderTask> {
            return UnsynchronizedComparator
        }

        @JvmStatic
        fun synchronizedComparator(renderer: TerrainRenderer): Comparator<ChunkBuilderTask> {
            return SynchronizedComparator(renderer)
        }

        private object UnsynchronizedComparator : Comparator<ChunkBuilderTask> {
            override fun compare(o1: ChunkBuilderTask, o2: ChunkBuilderTask): Int {
                val visible1 = o1.renderChunk.frustumCull.isInFrustum()
                val visible2 = o2.renderChunk.frustumCull.isInFrustum()

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

        private class SynchronizedComparator(terrainRenderer: TerrainRenderer) : Comparator<ChunkBuilderTask> {
            private val frustum = terrainRenderer.frustum
            private val matrixHash = terrainRenderer.matrixPosHash

            private val cameraChunkX = terrainRenderer.cameraChunkX
            private val cameraChunkY = terrainRenderer.cameraChunkY
            private val cameraChunkZ = terrainRenderer.cameraChunkZ

            override fun compare(o1: ChunkBuilderTask, o2: ChunkBuilderTask): Int {
                val visible1 = o1.renderChunk.frustumCull.isInFrustum(frustum, matrixHash)
                val visible2 = o2.renderChunk.frustumCull.isInFrustum(frustum, matrixHash)

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

abstract class RebuildTask(renderer: TerrainRenderer, scheduler: ChunkBuilder.TaskScheduler) :
    ChunkBuilderTask(renderer, scheduler) {
    final override suspend fun run0() {
        val bufferArray: Array<BufferContext?>
        val occlusionData: ChunkOcclusionData
        val translucentData: TranslucentData?
        val tileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?
        val instancingTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?
        val globalTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?

        val rebuildContext = renderer.contextProvider.getRebuildContext(this@RebuildTask)
        if (rebuildContext.worldSnapshot.init()) {
            rebuildContext.renderChunk(this@RebuildTask)

            bufferArray = Array(rebuildContext.layerVertexBuilderArray.size) { i ->
                val bufferContext = rebuildContext.layerVertexBuilderArray[i].popBuffer()
                if (bufferContext != null) {
                    val buffer = bufferContext.region.buffer
                    if (buffer.position() == 0) {
                        bufferContext.release(this@RebuildTask)
                        null
                    } else {
                        buffer.flip()
                        bufferContext
                    }
                } else {
                    null
                }
            }

            occlusionData = rebuildContext.occlusionDataBuilder.build()
            tileEntityList = rebuildContext.tileEntityList.copyOrNull()
            instancingTileEntityList = rebuildContext.instancingTileEntityList.copyOrNull()
            globalTileEntityList = rebuildContext.globalTileEntityList.copyOrNull()

            val translucentBuffer = bufferArray[3]?.region?.buffer
            translucentData = if (translucentBuffer != null) {
                val sortContext = renderer.contextProvider.getSortContext(this@RebuildTask)
                val vertexSize = translucentBuffer.remaining()
                val quadCount = vertexSize / 16 / 4

                sortContext.tempQuadCenter.ensureCapacity(quadCount * 3)
                val quadCenter = sortContext.tempQuadCenter.elements()
                val posArray = rebuildContext.translucentChunkVertexBuilder.posArrayList.elements()

                for (i in 0 until quadCount) {
                    val centerIndex = i * 3
                    quadCenter[centerIndex] = (posArray[i] + posArray[i + 3] + posArray[i + 6] + posArray[i + 9]) / 4.0f
                    quadCenter[centerIndex + 1] =
                        (posArray[i + 1] + posArray[i + 4] + posArray[i + 7] + posArray[i + 10]) / 4.0f
                    quadCenter[centerIndex + 2] =
                        (posArray[i + 2] + posArray[i + 5] + posArray[i + 8] + posArray[i + 11]) / 4.0f
                }
                rebuildContext.release(this@RebuildTask)

                sortContext.tempVertexData.ensureCapacity(vertexSize)
                val vertexData = sortContext.tempVertexData.elements()
                translucentBuffer.get(vertexData, 0, vertexSize)

                val data = sortContext.sortQuads(this@RebuildTask, vertexData, quadCenter, quadCount)
                sortContext.release(this@RebuildTask)

                translucentBuffer.clear()
                translucentBuffer.put(data.vertexData)
                translucentBuffer.flip()
                data
            } else {
                rebuildContext.release(this@RebuildTask)
                null
            }

            renderer.chunkBuilder.scheduleUpload(this@RebuildTask) {
                occlusionData(occlusionData)
                translucentData(translucentData)
                updateTileEntity(tileEntityList, instancingTileEntityList, globalTileEntityList)
                for (i in bufferArray.indices) {
                    updateLayer(i, bufferArray[i])
                }
            }
        } else {
            rebuildContext.release(this@RebuildTask)

            renderer.chunkBuilder.scheduleUpload(this@RebuildTask) {
                occlusionData(null)
                translucentData(null)
                for (i in 0 until renderer.layerCount) {
                    updateLayer(i, null)
                }
            }
        }
    }

    private inline fun <reified E> FastObjectArrayList<E>.copyOrNull(): FastObjectArrayList<E>? {
        return if (this.isNotEmpty()) {
            val list = FastObjectArrayList<E>(this.size)
            list.addAll(this)
            list
        } else {
            null
        }
    }
}

class SortTask(renderer: TerrainRenderer, scheduler: ChunkBuilder.TaskScheduler) :
    ChunkBuilderTask(renderer, scheduler) {
    private var data: TranslucentData? = null

    override fun init0(renderChunk: RenderChunk) {
        super.init0(renderChunk)
        data = renderChunk.translucentData ?: throw CancelInitException
    }

    override fun reset() {
        super.reset()
        data = null
    }

    override suspend fun run0() {
        val newData: TranslucentData
        val bufferContext: BufferContext

        val sortContext = renderer.contextProvider.getSortContext(this@SortTask)
        newData = sortContext.sortQuads(this@SortTask, data!!)
        sortContext.release(this@SortTask)

        bufferContext = renderer.contextProvider.getBufferContext(this@SortTask)
        while (bufferContext.region.length < newData.vertexData.size) {
            bufferContext.region.expand(this@SortTask)
        }
        val buffer = bufferContext.region.buffer
        buffer.put(newData.vertexData)
        buffer.flip()

        renderer.chunkBuilder.scheduleUpload(this) {
            translucentData(newData)
            updateLayer(3, bufferContext)
        }
    }
}