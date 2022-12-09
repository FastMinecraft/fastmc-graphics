package me.luna.fastmc.shared.terrain

import dev.fastmc.common.ObjectPool
import dev.fastmc.common.collection.FastIntMap
import dev.fastmc.common.collection.FastObjectArrayList
import me.luna.fastmc.shared.opengl.impl.RenderBufferPool
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

abstract class ChunkBuilder(
    protected val renderer: TerrainRenderer
) {
    val taskSchedulerPool = ObjectPool { TaskScheduler() }
    private val uploadTaskQueue = ConcurrentLinkedQueue<UploadTask>()
    private val pendingUploadQueueMap = FastIntMap<PendingUploadQueue>()

    private val uploadTaskCount0 = AtomicInteger(0)
    private val totalTaskCount0 = AtomicInteger(0)

    val totalTaskCount get() = totalTaskCount0.get()
    val uploadTaskCount get() = uploadTaskCount0.get()

    private val visibleClearCount = AtomicInteger(0)
    var uploadCount = 0; private set
    var visibleUploadCount = 0; private set

    inline fun scheduleTasks(block: TaskScheduler.() -> Unit) {
        val taskScheduler = synchronized(taskSchedulerPool) {
            taskSchedulerPool.get()
        }
        taskScheduler.init()
        block.invoke(taskScheduler)
        synchronized(taskSchedulerPool) {
            taskSchedulerPool.put(taskScheduler)
        }
    }

    fun update() {
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
        ChunkBuilderTask.cancelAllAndJoin()

        uploadTaskQueue.removeIf {
            it.cancel()
            true
        }
        pendingUploadQueueMap.values.forEach {
            it.clear()
        }
        pendingUploadQueueMap.clear()
    }

    protected abstract fun newRebuildTask(scheduler: TaskScheduler): RebuildTask

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

    inner class TaskScheduler {
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

        fun scheduleRebuild(renderChunk: RenderChunk): Boolean {
            val task = rebuildTaskPool.get()

            return if (task.init(renderChunk)) {
                renderChunk.isDirty = false
                task.run()
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

        fun scheduleSort(renderChunk: RenderChunk): Boolean {
            if (renderChunk.translucentData == null) return false
            val task = sortTaskPool.get()

            return if (task.init(renderChunk)) {
                task.run()
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
}