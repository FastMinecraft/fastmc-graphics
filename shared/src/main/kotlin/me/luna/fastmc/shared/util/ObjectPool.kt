package me.luna.fastmc.shared.util

import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import java.util.function.Supplier

class ObjectPool<T>(private val newInstance: Supplier<T>) {
    private val stack = FastObjectArrayList<T>()

    fun get(): T {
        return stack.removeLastOrNull() ?: newInstance.get()
    }

    fun put(obj: T) {
        stack.add(obj)
    }
}