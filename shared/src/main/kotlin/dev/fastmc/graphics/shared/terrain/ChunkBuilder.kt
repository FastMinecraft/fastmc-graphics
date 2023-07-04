package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.ObjectPool
import dev.fastmc.common.ParallelUtils
import dev.fastmc.common.UNSAFE
import dev.fastmc.common.collection.FastIntMap
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.distanceSq
import dev.fastmc.common.sort.ObjectIntrosort
import dev.fastmc.graphics.shared.renderer.cameraChunkX
import dev.fastmc.graphics.shared.renderer.cameraChunkY
import dev.fastmc.graphics.shared.renderer.cameraChunkZ
import dev.fastmc.graphics.shared.util.threadGroupMain
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.math.max

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
            vertexBufferPool.ensureCapacity(newLength ushr 32 and 0x7FFFFFFFL)
            indexBufferPool.ensureCapacity(newLength and 0x7FFFFFFFL)

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
        private val threadGroup = ThreadGroup(threadGroupMain, "chunk")
        private val taskQueue = FastObjectArrayList<ChunkBuilderTask>()

        val activeTaskCount get() = activeCount.get()
        val queuedTaskCount get() = taskQueue.size

        private val dirtyFlag = AtomicBoolean(false)
        private val sortFlag = AtomicBoolean(true)
        private var taskComparator = TaskComparator()
        private val activeCount = AtomicInteger(0)

        private val awaitArray = AtomicReferenceArray<Thread?>(max(ParallelUtils.CPU_THREADS - 1, 1))
        private val threads = Array(awaitArray.length()) { id ->
            Thread(threadGroup, {
                val self = Thread.currentThread()
                while (active.get()) {
                    try {
                        val task = awaitTask(id, self) ?: continue

                        try {
                            activeCount.incrementAndGet()
                            task.run()
                        } finally {
                            activeCount.decrementAndGet()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, "fastmc-graphics-chunk-${id + 1}").apply {
                priority = 1
                start()
            }
        }

        private fun awaitTask(
            id: Int,
            self: Thread
        ): ChunkBuilderTask? {
            while (taskQueue.isEmpty || dirtyFlag.get()) {
                if (!taskQueue.isEmpty && dirtyFlag.get() && sortFlag.getAndSet(false)) {
                    try {
                        updateTaskQueueOrder()?.let {
                            return it
                        }
                    } finally {
                        sortFlag.set(true)
                    }
                } else {
                    try {
                        awaitArray[id] = self
                        UNSAFE.park(false, 1145141919810L)
                    } finally {
                        awaitArray[id] = null
                    }
                }
            }

            if (dirtyFlag.get()) return null
            return synchronized(taskQueue) {
                taskQueue.removeLastOrNull()
            }
        }

        private fun updateTaskQueueOrder(): ChunkBuilderTask? {
            dirtyFlag.set(false)
            if (taskQueue.isEmpty) return null
            var task: ChunkBuilderTask? = null

            synchronized(taskQueue) {
                if (taskQueue.isEmpty) return null

                taskQueue.removeIf {
                    if (it.isCancelled) {
                        it.onFinish()
                        true
                    } else {
                        false
                    }
                }

                if (taskQueue.isEmpty) return null

                ObjectIntrosort.sort(taskQueue.elements(), 0, taskQueue.size, taskComparator)
                task = taskQueue.removeLast()
            }

            for (i in 0 until awaitArray.length()) {
                awaitArray.getAndSet(i, null)?.let { UNSAFE.unpark(it) }
            }

            return task
        }

        fun schedule(task: ChunkBuilderTask) {
            dirtyFlag.set(true)
            synchronized(taskQueue) {
                taskQueue.add(task)
            }
        }

        fun update() {
            val prev = taskComparator
            taskComparator = TaskComparator()
            if (prev != taskComparator) {
                dirtyFlag.set(true)
            }
            if (taskQueue.isNotEmpty() && dirtyFlag.get()) {
                for (i in 0 until awaitArray.length()) {
                    val thread = awaitArray.getAndSet(i, null) ?: continue
                    UNSAFE.unpark(thread)
                    break
                }
            }
        }

        fun clear() {
            synchronized(taskQueue) {
                taskQueue.forEach {
                    it.cancel()
                }
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

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is TaskComparator) return false

                if (matrixHash != other.matrixHash) return false
                if (cameraChunkX != other.cameraChunkX) return false
                if (cameraChunkY != other.cameraChunkY) return false
                if (cameraChunkZ != other.cameraChunkZ) return false

                return true
            }

            override fun hashCode(): Int {
                var result = matrixHash.hashCode()
                result = 31 * result + cameraChunkX
                result = 31 * result + cameraChunkY
                result = 31 * result + cameraChunkZ
                return result
            }
        }
    }
}