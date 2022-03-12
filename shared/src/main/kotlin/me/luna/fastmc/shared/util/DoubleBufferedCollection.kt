package me.luna.fastmc.shared.util

import java.util.function.Consumer

class DoubleBufferedCollection<T : MutableCollection<*>>(value: T, private val initAction: Consumer<T>) {
    @Suppress("UNCHECKED_CAST")
    constructor(value: T) : this(value, DEFAULT_INIT_ACTION as Consumer<T>)

    private var delegate = value
    private var swap: T = delegate.javaClass.newInstance()

    fun get(): T {
        return delegate
    }

    fun getAndSwap(): T {
        val temp = delegate
        initAction.accept(swap)
        delegate = swap
        swap = temp
        return swap
    }

    fun swapAndGet(): T {
        val temp = delegate
        initAction.accept(swap)
        delegate = swap
        swap = temp
        return delegate
    }


    private companion object {
        val DEFAULT_INIT_ACTION = Consumer<MutableCollection<*>> {
            it.clear()
        }
    }
}