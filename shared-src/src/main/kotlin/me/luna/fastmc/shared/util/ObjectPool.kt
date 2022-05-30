package me.luna.fastmc.shared.util

import java.util.function.Supplier

class ObjectPool<T>(private val newInstance: Supplier<T>) {
    private val stack = java.util.ArrayDeque<T>()

    fun get(): T {
        return stack.pollLast() ?: newInstance.get()
    }

    fun put(obj: T) {
        stack.addLast(obj)
    }
}