@file:Suppress("NOTHING_TO_INLINE")

package me.luna.fastmc.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.FastObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectCollections
import kotlinx.coroutines.*
import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.instancing.tileentity.info.ITileEntityInfo
import me.luna.fastmc.shared.renderer.cameraChunkX
import me.luna.fastmc.shared.renderer.cameraChunkY
import me.luna.fastmc.shared.renderer.cameraChunkZ
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
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    protected open fun init0(renderChunk: RenderChunk) {
        this.renderChunkNullable = renderChunk
        id0.set(nextId())

        relativeCameraX = (renderer.cameraX - renderChunk.originX).toFloat()
        relativeCameraY = (renderer.cameraY - renderChunk.originY).toFloat()
        relativeCameraZ = (renderer.cameraZ - renderChunk.originZ).toFloat()

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
                        priority = 1
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
        val bufferGroupArray: Array<TerrainVertexBuilder.BufferGroup?>
        val occlusionData: ChunkOcclusionData
        val translucentData: TranslucentData?
        val tileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?
        val instancingTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?
        val globalTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?

        val rebuildContext = renderer.contextProvider.getRebuildContext(this@RebuildTask)
        if (rebuildContext.worldSnapshot.init()) {
            rebuildContext.renderChunk(this@RebuildTask)

            bufferGroupArray = Array(rebuildContext.vertexBuilderArray.size) { i ->
                rebuildContext.vertexBuilderArray[i].finish()
            }

            occlusionData = rebuildContext.occlusionDataBuilder.build()
            tileEntityList = rebuildContext.tileEntityList.copyOrNull()
            instancingTileEntityList = rebuildContext.instancingTileEntityList.copyOrNull()
            globalTileEntityList = rebuildContext.globalTileEntityList.copyOrNull()

            val translucentBufferPair = bufferGroupArray[3]
            translucentData = if (translucentBufferPair != null) {
                val indexBuffer = translucentBufferPair.indexBuffers[0]!!.region.buffer
                val sortContext = renderer.contextProvider.getSortContext(this@RebuildTask)
                val indexSize = indexBuffer.remaining()
                val quadCount = rebuildContext.translucentVertexBuilder.posArrayList.size / 12

                sortContext.tempQuadCenter.ensureCapacity(quadCount * 3)
                val quadCenterArray = sortContext.tempQuadCenter.elements()
                val posArray = rebuildContext.translucentVertexBuilder.posArrayList.elements()

                for (i in 0 until quadCount) {
                    val centerIndex = i * 3
                    val posIndex = i * 12
                    quadCenterArray[centerIndex] =
                        (posArray[posIndex] + posArray[posIndex + 3] + posArray[posIndex + 6] + posArray[posIndex + 9]) / 4.0f
                    quadCenterArray[centerIndex + 1] =
                        (posArray[posIndex + 1] + posArray[posIndex + 4] + posArray[posIndex + 7] + posArray[posIndex + 10]) / 4.0f
                    quadCenterArray[centerIndex + 2] =
                        (posArray[posIndex + 2] + posArray[posIndex + 5] + posArray[posIndex + 8] + posArray[posIndex + 11]) / 4.0f
                }
                rebuildContext.release(this@RebuildTask)

                sortContext.tempIndexData.ensureCapacity(indexSize)
                val indexDataArray = sortContext.tempIndexData.elements()
                indexBuffer.get(indexDataArray, 0, indexSize)

                val data = sortContext.sortQuads(this@RebuildTask, indexDataArray, quadCenterArray, quadCount)
                sortContext.release(this@RebuildTask)

                indexBuffer.clear()
                indexBuffer.put(data.indexData)
                indexBuffer.flip()
                data
            } else {
                rebuildContext.release(this@RebuildTask)
                null
            }

            renderer.chunkBuilder.scheduleUpload(this@RebuildTask) {
                occlusionData(occlusionData)
                translucentData(translucentData)
                updateTileEntity(tileEntityList, instancingTileEntityList, globalTileEntityList)
                for (i in bufferGroupArray.indices) {
                    val bufferGroup = bufferGroupArray[i]
                    var faceData: FaceData? = null
                    var vertexBuffer: BufferContext? = null
                    var indexBuffer: BufferContext? = null

                    if (bufferGroup != null) {
                        combineBuffers(bufferGroup)?.let {
                            faceData = it.first
                            vertexBuffer = it.second
                            indexBuffer = it.third
                        }
                    }

                    updateLayer(i, vertexBuffer, indexBuffer, faceData)
                }
            }
        } else {
            rebuildContext.release(this@RebuildTask)

            renderer.chunkBuilder.scheduleUpload(this@RebuildTask) {
                occlusionData(null)
                translucentData(null)
                for (i in 0 until renderer.layerCount) {
                    updateLayer(i, null, null, null)
                }
            }
        }
    }

    private fun combineBuffers(
        bufferGroup: TerrainVertexBuilder.BufferGroup
    ): Triple<FaceData, BufferContext, BufferContext>? {
        return if (bufferGroup.vertexBuffers.size == 1) {
            val vertexBuffer = bufferGroup.vertexBuffers[0]!!
            val indexBuffer = bufferGroup.indexBuffers[0]!!
            Triple(FaceData.Singleton(indexBuffer.region.buffer.remaining()), vertexBuffer, indexBuffer)
        } else {
            var dataArray: IntArray? = null

            var vertexOffset = 0
            var indexOffset = 0

            var resultVertexBuffer: BufferContext? = null
            var resultIndexBuffer: BufferContext? = null

            for (i in bufferGroup.vertexBuffers.indices) {
                val vertexBuffer = bufferGroup.vertexBuffers[i] ?: continue
                val indexBuffer = bufferGroup.indexBuffers[i]!!

                val vertexLength = vertexBuffer.region.buffer.remaining()
                val indexLength = indexBuffer.region.buffer.remaining()

                if (resultVertexBuffer == null) {
                    resultVertexBuffer = vertexBuffer
                    vertexBuffer.region.buffer.position(vertexBuffer.region.buffer.limit())
                    vertexBuffer.region.buffer.limit(vertexBuffer.region.buffer.capacity())

                    resultIndexBuffer = indexBuffer
                    indexBuffer.region.buffer.position(indexBuffer.region.buffer.limit())
                    indexBuffer.region.buffer.limit(indexBuffer.region.buffer.capacity())
                } else {
                    while (resultVertexBuffer.region.buffer.remaining() < vertexLength) {
                        resultVertexBuffer.region.expand(this)
                    }

                    while (resultIndexBuffer!!.region.buffer.remaining() < indexLength) {
                        resultIndexBuffer.region.expand(this)
                    }

                    resultVertexBuffer.region.buffer.put(vertexBuffer.region.buffer)
                    resultIndexBuffer.region.buffer.put(indexBuffer.region.buffer)

                    vertexBuffer.release(this)
                    indexBuffer.release(this)
                }

                if (dataArray == null) {
                    dataArray = IntArray(63 * 3) { -1 }
                }

                val dataIndex = i * 3
                dataArray[dataIndex] = vertexOffset
                dataArray[dataIndex + 1] = indexOffset
                dataArray[dataIndex + 2] = indexLength

                vertexOffset += vertexLength
                indexOffset += indexLength
            }

            if (dataArray != null) {
                resultVertexBuffer!!.region.buffer.flip()
                resultIndexBuffer!!.region.buffer.flip()
                Triple(FaceData.Multiple(dataArray), resultVertexBuffer, resultIndexBuffer)
            } else {
                null
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
        val sortContext = renderer.contextProvider.getSortContext(this@SortTask)
        val newData = sortContext.sortQuads(this@SortTask, data!!)
        sortContext.release(this@SortTask)

        val bufferContext = renderer.contextProvider.getBufferContext(this@SortTask)
        while (bufferContext.region.length < newData.indexData.size) {
            bufferContext.region.expand(this@SortTask)
        }
        val buffer = bufferContext.region.buffer
        buffer.put(newData.indexData)
        buffer.flip()

        renderer.chunkBuilder.scheduleUpload(this) {
            translucentData(newData)
            updateLayer(3, null, bufferContext, null)
        }
    }
}