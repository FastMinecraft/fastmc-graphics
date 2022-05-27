package me.luna.fastmc.shared.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ChannelObjectPool<T> constructor(capacity: Int) : SuspendObjectPool<T> {
    constructor(capacity: Int, init: (ChannelObjectPool<T>) -> T) : this(capacity) {
        repeat(capacity) {
            put(init.invoke(this))
        }
    }

    private val channel = Channel<T>(capacity)

    override suspend fun get(): T {
        return channel.receive()
    }

    override fun tryGet(): T? {
        return channel.tryReceive().getOrNull()
    }

    override fun put(element: T) {
        channel.trySend(element)
    }
}

open class PriorityObjectPool<K : Cancellable, E>(
    private val pool: SuspendObjectPool<E>,
    comparator: Comparator<K>? = null
) : AbstractPriorityObjectPool<K, E>(comparator) {
    private val queue = ConcurrentLinkedQueue<QueueEntry>()
    private val notifier = AtomicReference<Continuation<Unit>?>(null)

    override suspend fun get(key: K): E {
        return pool.tryGet() ?: suspendCoroutine {
            val queueEntry = QueueEntry(key, it)
            queue.add(queueEntry)
            notifier.getAndSet(null)?.resume()
        }
    }

    override fun put(element: E) {
        pool.put(element)
    }

    init {
        FastMcExtendScope.launch {
            while (isActive) {
                if (queue.isEmpty()) {
                    suspendCoroutine<Unit> {
                        notifier.set(it)
                    }
                }
                val element = pool.get()

                if (queue.isNotEmpty()) {
                    var queueEntry: QueueEntry? = null

                    queue.removeIf {
                        if (it.value.isCancelled) {
                            it.continuation.resumeWithException(CancellationException())
                            true
                        } else {
                            if (queueEntry == null || compare(it.value, queueEntry!!.value) > 0) {
                                queueEntry = it
                            }
                            false
                        }
                    }

                    if (queueEntry != null) {
                        queue.remove(queueEntry)
                        queueEntry!!.continuation.resume(element)
                        continue
                    }
                }

                pool.put(element)
            }
        }
    }

    private inner class QueueEntry(val value: K, val continuation: Continuation<E>)
}

class ArrayPriorityObjectPool<K : Cancellable, E>(
    capacity: Int,
    comparator: Comparator<K>? = null,
) : AbstractPriorityObjectPool<K, E>(comparator) {
    constructor(capacity: Int, comparator: Comparator<K>? = null, init: (ArrayPriorityObjectPool<K, E>) -> E) : this(
        capacity,
        comparator
    ) {
        repeat(capacity) {
            put(init.invoke(this))
        }
    }

    private val queue = ConcurrentLinkedQueue<QueueEntry>()
    private val array = arrayOfNulls<Any>(capacity)

    @Volatile
    private var index = 0

    override suspend fun get(key: K): E {
        var result: E? = null

        if (index != 0) {
            synchronized(array) {
                if (index != 0) {
                    @Suppress("UNCHECKED_CAST")
                    result = array[--index] as E
                }
            }
        }

        if (result == null) {
            result = suspendCoroutine<E> {
                val queueEntry = QueueEntry(key, it)
                queue.add(queueEntry)
            }
        }

        return result!!
    }

    override fun put(element: E) {
        if (queue.isNotEmpty()) {
            synchronized(queue) {
                if (queue.isNotEmpty()) {
                    var queueEntry: QueueEntry? = null
                    queue.removeIf {
                        if (it.value.isCancelled) {
                            it.continuation.resumeWithException(CancellationException())
                            true
                        } else {
                            if (queueEntry == null || compare(it.value, queueEntry!!.value) > 0) {
                                queueEntry = it
                            }
                            false
                        }
                    }

                    if (queueEntry != null) {
                        queue.remove(queueEntry)
                        queueEntry!!.continuation.resume(element)
                        return
                    }
                }
            }
        }

        synchronized(array) {
            array[index++] = element
        }
    }

    private inner class QueueEntry(val value: K, val continuation: Continuation<E>)
}

abstract class AbstractPriorityObjectPool<K : Cancellable, E>(protected val comparator: Comparator<K>?) {
    abstract suspend fun get(key: K): E

    abstract fun put(element: E)

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    protected inline fun compare(o1: K, o2: K): Int {
        return comparator?.compare(o1, o2) ?: (o1 as Comparable<K>).compareTo(o2)
    }
}

interface SuspendObjectPool<T> {
    suspend fun get(): T
    fun tryGet(): T?
    fun put(element: T)
}