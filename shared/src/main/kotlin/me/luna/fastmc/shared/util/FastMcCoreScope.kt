package me.luna.fastmc.shared.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

private val group = ThreadGroup("FastMinecraft").apply {
    this.isDaemon = true
}

private val extendPool0 = object : ThreadPoolExecutor(
    ParallelUtils.CPU_THREADS,
    ParallelUtils.CPU_THREADS * 4,
    3L,
    java.util.concurrent.TimeUnit.SECONDS,
    ArrayBlockingQueue(ParallelUtils.CPU_THREADS),
    object : ThreadFactory {
        private val counter = AtomicInteger(1)

        override fun newThread(r: Runnable): Thread {
            return Thread(group, r, "FastMinecraft-Extend-${counter.getAndIncrement()}")
        }
    }
) {
    private val unboundQueue = LinkedBlockingQueue<Runnable>()
    private val schedulerThread = Thread(group, {
        try {
            var lastTask: Runnable?
            var lastHead: Runnable? = null
            var headAddedTime = System.nanoTime()
            val emptyRunnable = {}

            while (!this.isShutdown) {
                lastTask = unboundQueue.poll(500_000L, java.util.concurrent.TimeUnit.NANOSECONDS)
                while (lastTask != null) {
                    if (queue.offer(lastTask, 500_000L, java.util.concurrent.TimeUnit.NANOSECONDS)) {
                        lastTask = null
                    }
                }

                // Force starting a new worker if a task has been enqueued for too long
                val head = queue.peek()
                if (head != lastHead) {
                    lastHead = head
                    headAddedTime = System.nanoTime()
                    continue
                }

                if (lastHead != null && System.nanoTime() - headAddedTime >= 2_500_000L) {
                    lastHead = null
                    while (queue.offer(emptyRunnable)) {
                        //
                    }
                }
            }
        } catch (e: InterruptedException) {
            //
        }
        unboundQueue.clear()
    }, "FastMinecraft-Extend-Scheduler")

    init {
        allowCoreThreadTimeOut(true)
        setRejectedExecutionHandler { r, _ ->
            synchronized(unboundQueue) {
                unboundQueue.add(r)
            }
        }

        schedulerThread.priority = Thread.MIN_PRIORITY
        schedulerThread.start()
    }

    override fun execute(command: Runnable) {
        super.execute(command)
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        super.afterExecute(r, t)
    }

    override fun shutdown() {
        super.shutdown()
        schedulerThread.interrupt()
    }
}

private val pool0 = ScheduledThreadPoolExecutor(
    ParallelUtils.CPU_THREADS * 2,
    object : ThreadFactory {
        private val counter = AtomicInteger(1)

        override fun newThread(r: Runnable): Thread {
            return Thread(group, r, "FastMinecraft-Core-${counter.getAndIncrement()}")
        }
    }
)

private val context0 = pool0.asCoroutineDispatcher()

object FastMcCoreScope : CoroutineScope by CoroutineScope(context0) {
    val pool = pool0
    val context = context0
}

private val context1 = extendPool0.asCoroutineDispatcher()

object FastMcExtendScope : CoroutineScope by CoroutineScope(context1) {
    val pool = extendPool0
    val context = context1
}