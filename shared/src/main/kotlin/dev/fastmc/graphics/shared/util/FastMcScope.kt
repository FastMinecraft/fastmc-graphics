package dev.fastmc.graphics.shared.util

import dev.fastmc.common.ParallelUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

@JvmField
val threadGroupMain = ThreadGroup("FastMinecraft")

private val corePool = ScheduledThreadPoolExecutor(
    max(ParallelUtils.CPU_THREADS - 1, 1),
    object : ThreadFactory {
        private val counter = AtomicInteger(0)
        private val group = ThreadGroup(threadGroupMain, "Core")

        override fun newThread(r: Runnable): Thread {
            return Thread(group, r, "FastMinecraft-Core-${counter.incrementAndGet()}").apply {
                priority = 6
                isDaemon = true
            }
        }
    }
)

private val coreContext = corePool.asCoroutineDispatcher()

object FastMcCoreScope : CoroutineScope by CoroutineScope(coreContext) {
    val pool = corePool
    val context = coreContext
    val threadCount = max(ParallelUtils.CPU_THREADS - 1, 1)
}

private val extendPool = ForkJoinPool(
    max(ParallelUtils.CPU_THREADS - 1, 1),
    object : ForkJoinPool.ForkJoinWorkerThreadFactory {
        private val idRegistry = IDRegistry()
        override fun newThread(pool: ForkJoinPool): ForkJoinWorkerThread {
            return object : ForkJoinWorkerThread(pool) {
                private val threadID = idRegistry.register()

                init {
                    priority = 3
                    name = "FastMinecraft-Extend-${threadID + 1}"
                }

                override fun onTermination(exception: Throwable?) {
                    idRegistry.unregister(threadID)
                }
            }
        }
    },
    null,
    true
)

private val extendContext = extendPool.asCoroutineDispatcher()

object FastMcExtendScope : CoroutineScope by CoroutineScope(extendContext) {
    val pool = extendPool
    val context = extendContext
}