package me.luna.fastmc.shared.util

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Supplier

class ConcurrentObjectPool<T>(private val newInstance: Supplier<T>) {
    private val stack = ConcurrentLinkedQueue<T>()

    fun get(): T {
        return stack.poll() ?: newInstance.get()
    }

    fun put(obj: T) {
        stack.add(obj)
    }
}