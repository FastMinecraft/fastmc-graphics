package me.luna.fastmc.shared.util

import java.util.function.Consumer

class DoubleBuffered<T>(value: T, private var swap: T, private val initAction: Consumer<T>) {
    @Suppress("UNCHECKED_CAST")
    constructor(value: T, swap: T) : this(value, swap, DEFAULT_INIT_ACTION as Consumer<T>)

    private var delegate = value

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
        val DEFAULT_INIT_ACTION = Consumer<Any?> {

        }
    }
}