package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.FastIntMap
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.sort.ObjectIntrosort
import dev.fastmc.graphics.shared.renderer.cameraChunkX
import dev.fastmc.graphics.shared.renderer.cameraChunkY
import dev.fastmc.graphics.shared.renderer.cameraChunkZ
import dev.fastmc.graphics.shared.util.threadGroupMain
import java.util.concurrent.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class ChunkBuilder(
    protected val renderer: TerrainRenderer
) {
    val taskFactoryPool = ObjectPool { TaskFactory() }
    val taskScheduler = TaskScheduler()

    private val uploadTaskQueue = ConcurrentLinkedQueue<UploadTask>()
    private val pendingUploadQueueMap = FastIntMap<PendingUploadQueue>()

    private val uploadTaskCount0 = AtomicInteger(0)

    val activeTaskCount get() = taskScheduler.activeTaskCount
    val totalTaskCount get() = taskScheduler.activeTaskCount + taskScheduler.queuedTaskCount
    val uploadTaskCount get() = uploadTaskCount0.get()

    private val visibleClearCount = AtomicInteger(0)
    var uploadCount = 0; private set
    var visibleUploadCount = 0; private set

    private var distanceFlush = false

    inline fun scheduleTasks(block: TaskFactory.() -> Unit) {
        val taskFactory = synchronized(taskFactoryPool) {
            taskFactoryPool.get()
        }
        taskFactory.init()
        block.invoke(taskFactory)
        synchronized(taskFactoryPool) {
            taskFactoryPool.put(taskFactory)
        }
        taskScheduler.update()
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
                    queue = PendingUploadQueue(region)
                    pendingUploadQueueMap[region.index] = queue
                }
                queue.add(it)
            }
            pendingUploadQueueMap.values.asSequence()
                .filter { it.taskCount > 0 }
                .run {
                    if (distanceFlush) {
                        maxByOrNull { renderer.chunkStorage.regionOrder[it.region.index] }
                    } else {
                        maxByOrNull { it.taskCount }
                    }
                }?.flush()

            distanceFlush = !distanceFlush
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
        val builder = UploadTask.Builder(task).apply(block)
        task.renderChunk.onTaskFinish(task)
        uploadTaskQueue.add(builder.build())
        uploadTaskCount0.incrementAndGet()
        task.finish()
    }

    private inner class PendingUploadQueue(
        val region: RenderRegion,
    ) {
        val vertexBufferPool = region.vertexBufferPool
        val indexBufferPool = region.indexBufferPool
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
        }

        internal fun onTaskUpload() {
            uploadTaskCount0.decrementAndGet()
        }

        internal fun onTaskFinish(task: ChunkBuilderTask) {
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

    inner class TaskScheduler {
        private val active = AtomicBoolean(true)
        private val threadGroup = ThreadGroup(threadGroupMain, "ChunkBuilder")
        private val taskQueue = FastObjectArrayList<ChunkBuilderTask>()
        @Volatile
        private var dirty = false
        private val awaitLock = ReentrantLock(true)
        private val awaitCondition = awaitLock.newCondition()
        private val activeCount = AtomicInteger(0)

        private val threads = run {
            val r = Runnable {
                while (active.get()) {
                    try {
                        while (taskQueue.isEmpty || dirty) {
                            awaitLock.withLock { awaitCondition.await() }
                        }
                        val task = synchronized(taskQueue) { taskQueue.removeLastOrNull() } ?: continue

                        try {
                            activeCount.incrementAndGet()
                            task.run()
                        } finally {
                            activeCount.decrementAndGet()
                        }
                    } catch (e: InterruptedException) {
                        continue
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            Array(ParallelUtils.CPU_THREADS) {
                Thread(threadGroup, r, "FastMinecraft-ChunkBuilder-${it + 1}").apply {
                    priority = 4
                    start()
                }
            }
        }

        private var taskComparator = TaskComparator()
        private val orderDirty = AtomicBoolean(true)

        val activeTaskCount get() = activeCount.get()
        val queuedTaskCount get() = taskQueue.size

        fun schedule(task: ChunkBuilderTask) {
            synchronized(taskQueue) {
                taskQueue.add(task)
                dirty = true
            }
        }

        fun update() {
            val prev = taskComparator
            taskComparator = TaskComparator()
            if (prev.matrixHash != taskComparator.matrixHash) {
                orderDirty.set(true)
            }
            if (!taskQueue.isEmpty) {
                synchronized(taskQueue) {
                    taskQueue.removeIf {
                        if (it.isCancelled) {
                            it.onFinish()
                            true
                        } else {
                            false
                        }
                    }
                    ObjectIntrosort.sort(taskQueue.elements(), 0, taskQueue.size, taskComparator)
                    dirty = false
                }
                awaitLock.withLock {
                    awaitCondition.signalAll()
                }
            }
        }

        fun clear() {
            synchronized(taskQueue) {
                taskQueue.clear()
            }
            while (activeCount.get() != 0) {
                renderer.contextProvider.update()
                Thread.sleep(5)
            }
        }

        fun shutDown() {
            active.set(false)
            threads.forEach {
                it.interrupt()
            }
            threads.forEach {
                it.join()
            }
        }

        private inner class TaskComparator : Comparator<ChunkBuilderTask> {
            val frustum = renderer.frustum
            val matrixHash = renderer.matrixPosHash
            private val cameraChunkX = renderer.cameraChunkX
            private val cameraChunkY = renderer.cameraChunkY
            private val cameraChunkZ = renderer.cameraChunkZ

            override fun compare(o1: ChunkBuilderTask, o2: ChunkBuilderTask): Int {
                val visible1 = o1.renderChunk.frustumCull.isInFrustum(frustum, matrixHash)
                val visible2 = o2.renderChunk.frustumCull.isInFrustum(frustum, matrixHash)

                if (visible1 != visible2) {
                    return if (visible1) 1 else -1
                }

                val distance1 = distanceSq(
                    cameraChunkX, cameraChunkY, cameraChunkZ,
                    o1.chunkX, o1.chunkY, o1.chunkZ
                )
                val distance2 = distanceSq(
                    cameraChunkX, cameraChunkY, cameraChunkZ,
                    o2.chunkX, o2.chunkY, o2.chunkZ
                )
                return distance2.compareTo(distance1)
            }
        }

    }
}