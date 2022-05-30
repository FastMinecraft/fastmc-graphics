package me.luna.fastmc.shared.util

import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
class DoubleBufferedCollection<T : MutableCollection<*>> {
    constructor(delegate: T, swap: T) {
        this.delegate = delegate
        this.swap = swap
        this.initAction = DEFAULT_INIT_ACTION as Consumer<T>
    }

    constructor(delegate: T, swap: T, initAction: Consumer<T>) {
        this.delegate = delegate
        this.swap = swap
        this.initAction = initAction
    }

    @Volatile
    private var delegate: T

    @Volatile
    private var swap: T
    private val initAction: Consumer<T>

    fun get(): T {
        return delegate
    }

    fun getSwap(): T {
        return swap
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


    companion object {
        @JvmField
        val DEFAULT_INIT_ACTION = Consumer<MutableCollection<*>> {
            it.clear()
        }

        @JvmStatic
        private val EMPTY_INIT_ACTION = Consumer<MutableCollection<*>> {

        }

        @JvmStatic
        fun <T> emptyInitAction(): Consumer<T> {
            return EMPTY_INIT_ACTION as Consumer<T>
        }
    }
}