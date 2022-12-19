package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.FastIntMap
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.opengl.impl.RenderBufferPool
import dev.fastmc.graphics.shared.renderer.cameraChunkX
import dev.fastmc.graphics.shared.renderer.cameraChunkY
import dev.fastmc.graphics.shared.renderer.cameraChunkZ
import dev.fastmc.graphics.shared.util.threadGroupMain
import java.util.concurrent.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class ChunkBuilder(
    protected val renderer: TerrainRenderer
) {
    val taskFactoryPool = ObjectPool { TaskFactory() }
    private val taskScheduler = TaskScheduler()

    private val uploadTaskQueue = ConcurrentLinkedQueue<UploadTask>()
    private val pendingUploadQueueMap = FastIntMap<PendingUploadQueue>()

    private val uploadTaskCount0 = AtomicInteger(0)
    private val totalTaskCount0 = AtomicInteger(0)

    val totalTaskCount get() = totalTaskCount0.get() + taskScheduler.queuedTaskCount
    val uploadTaskCount get() = uploadTaskCount0.get()

    private val visibleClearCount = AtomicInteger(0)
    var uploadCount = 0; private set
    var visibleUploadCount = 0; private set

    inline fun scheduleTasks(block: TaskFactory.() -> Unit) {
        val taskScheduler = synchronized(taskFactoryPool) {
            taskFactoryPool.get()
        }
        taskScheduler.init()
        block.invoke(taskScheduler)
        synchronized(taskFactoryPool) {
            taskFactoryPool.put(taskScheduler)
        }
    }

    fun update() {
        taskScheduler.update()

        uploadCount = 0
        visibleUploadCount = visibleClearCount.getAndSet(0)

        if (uploadTaskCount != 0) {
            uploadTaskQueue.removeIf {
                if (it.renderChunk.isDestroyed) {
                    it.cancel()
                    return@removeIf true
                }

                val region = it.renderChunk.renderRegion
                var queue = pendingUploadQueueMap[region.index]
                if (queue == null || queue.vertexBufferPool !== region.vertexBufferPool) {
                    queue?.clear()
                    queue = PendingUploadQueue(region.vertexBufferPool, region.indexBufferPool)
                    pendingUploadQueueMap[region.index] = queue
                }
                queue.add(it)
            }
            pendingUploadQueueMap.values.maxByOrNull { it.taskCount }?.flush()
        }

        renderer.contextProvider.update()
        for (region in renderer.chunkStorage.regionArray) {
            region.updateVao()
        }
    }

    fun clear() {
        taskScheduler.clear()

        uploadTaskQueue.removeIf {
            it.cancel()
            true
        }
        pendingUploadQueueMap.values.forEach {
            it.clear()
        }
        pendingUploadQueueMap.clear()
    }

    protected abstract fun newRebuildTask(scheduler: TaskFactory): RebuildTask

    internal inline fun scheduleUpload(task: ChunkBuilderTask, block: UploadTask.Builder.() -> Unit) {
        task.checkCancelled()
        val builder = UploadTask.Builder(task).apply(block)
        task.renderChunk.onTaskFinish(task)
        uploadTaskQueue.add(builder.build())
        uploadTaskCount0.incrementAndGet()
        task.finish()
    }

    private inner class PendingUploadQueue(
        val vertexBufferPool: RenderBufferPool,
        val indexBufferPool: RenderBufferPool
    ) {
        private val list = FastObjectArrayList.wrap(arrayOfNulls<UploadTask>(4096), 0)

        val taskCount get() = list.size

        fun add(uploadTask: UploadTask): Boolean {
            if (list.size >= 4096) return false
            list.add(uploadTask)
            return true
        }

        fun flush() {
            if (list.isEmpty) return
            var newLength = 0L

            for (i in list.indices) {
                newLength += list[i].runClear()
            }

            vertexBufferPool.update()
            indexBufferPool.update()
            vertexBufferPool.ensureCapacity((newLength ushr 32).toInt())
            indexBufferPool.ensureCapacity(newLength.toInt())

            for (i in list.indices) {
                val task = list[i]
                if (task.runUpdate() && task.renderChunk.frustumCull.isInFrustum()) {
                    visibleUploadCount += 1
                }
            }

            uploadCount += list.size
            list.clear()
        }

        fun clear() {
            for (i in list.indices) {
                list[i].cancel()
            }
            list.clear()
        }
    }

    inner class TaskFactory {
        private val rebuildTaskPool = ObjectPool { newRebuildTask(this) }
        private val sortTaskPool = ObjectPool { SortTask(renderer, this) }
        private val pendingReleaseBuildTask = FastObjectArrayList<RebuildTask>()
        private val pendingReleaseSortTask = FastObjectArrayList<SortTask>()

        fun init() {
            synchronized(pendingReleaseBuildTask) {
                for (i in pendingReleaseBuildTask.indices) {
                    rebuildTaskPool.put(pendingReleaseBuildTask[i])
                }
                pendingReleaseBuildTask.clear()
            }
            synchronized(pendingReleaseSortTask) {
                for (i in pendingReleaseSortTask.indices) {
                    sortTaskPool.put(pendingReleaseSortTask[i])
                }
                pendingReleaseSortTask.clear()
            }
        }

        fun createRebuild(renderChunk: RenderChunk): Boolean {
            val task = rebuildTaskPool.get()

            return if (task.init(renderChunk)) {
                renderChunk.isDirty = false
                taskScheduler.schedule(task)
                true
            } else {
                renderChunk.isDirty = false
                for (i in renderChunk.layers.indices) {
                    val layer = renderChunk.layers[i]
                    layer.vertexRegion = null
                    layer.indexRegion = null
                }
                renderChunk.onUpdate()
                if (renderChunk.frustumCull.isInFrustum()) {
                    visibleClearCount.incrementAndGet()
                }
                rebuildTaskPool.put(task)
                false
            }
        }

        fun createSort(renderChunk: RenderChunk): Boolean {
            if (renderChunk.translucentData == null) return false
            val task = sortTaskPool.get()

            return if (task.init(renderChunk)) {
                taskScheduler.schedule(task)
                true
            } else {
                sortTaskPool.put(task)
                false
            }
        }

        internal fun onTaskStart(task: ChunkBuilderTask, renderChunk: RenderChunk) {
            renderChunk.onTaskStart(task)
            totalTaskCount0.incrementAndGet()
        }

        internal fun onTaskUpload() {
            uploadTaskCount0.decrementAndGet()
        }

        internal fun onTaskFinish(task: ChunkBuilderTask) {
            totalTaskCount0.decrementAndGet()
            task.releaseResource()
            task.reset()
            when (task) {
                is RebuildTask -> synchronized(pendingReleaseBuildTask) {
                    pendingReleaseBuildTask.add(task)
                }
                is SortTask -> synchronized(pendingReleaseSortTask) {
                    pendingReleaseSortTask.add(task)
                }
            }
        }
    }

    inner class TaskScheduler : Comparator<ChunkBuilderTask> {
        @Suppress("UNCHECKED_CAST")
        private val threadPool = ThreadPoolExecutor(
            ParallelUtils.CPU_THREADS,
            ParallelUtils.CPU_THREADS,
            114514,
            TimeUnit.SECONDS,
            PriorityBlockingQueue(16, this) as BlockingQueue<Runnable>,
            object : ThreadFactory {
                private val counter = AtomicInteger(0)
                private val group = ThreadGroup(threadGroupMain, "ChunkBuilder")

                override fun newThread(r: Runnable): Thread {
                    return Thread(group, r, "FastMinecraft-ChunkBuilder-${counter.incrementAndGet()}").apply {
                        priority = 4
                    }
                }
            }
        )

        private var frustum = renderer.frustum
        private var matrixHash = renderer.matrixPosHash
        private var cameraChunkX = renderer.cameraChunkX
        private var cameraChunkY = renderer.cameraChunkY
        private var cameraChunkZ = renderer.cameraChunkZ

        val queuedTaskCount get() = threadPool.queue.size

        fun schedule(task: ChunkBuilderTask) {
            threadPool.execute(task)
        }

        fun update() {
            frustum = renderer.frustum
            matrixHash = renderer.matrixPosHash
            cameraChunkX = renderer.cameraChunkX
            cameraChunkY = renderer.cameraChunkY
            cameraChunkZ = renderer.cameraChunkZ
        }

        fun clear() {
            threadPool.queue.pollEach {
                (it as ChunkBuilderTask).cancel()
                it.run()
            }
            while (threadPool.activeCount != 0) {
                renderer.contextProvider.update()
                Thread.sleep(5)
            }
        }

        override fun compare(o1: ChunkBuilderTask, o2: ChunkBuilderTask): Int {
            val visible1 = o1.renderChunk.frustumCull.isInFrustum(frustum, matrixHash)
            val visible2 = o2.renderChunk.frustumCull.isInFrustum(frustum, matrixHash)

            if (visible1 != visible2) {
                return if (visible1) -1 else 1
            }

            val distance1 = distanceSq(
                cameraChunkX, cameraChunkY, cameraChunkZ,
                o1.chunkX, o1.chunkY, o1.chunkZ
            )
            val distance2 = distanceSq(
                cameraChunkX, cameraChunkY, cameraChunkZ,
                o2.chunkX, o2.chunkY, o2.chunkZ
            )
            return distance1.compareTo(distance2)
        }
    }
}