package me.luna.fastmc.shared.util

class DoubleBufferedCollection<T : MutableCollection<*>>(value: T) {
    private var delegate = value
    private var swap: T = delegate.javaClass.newInstance()

    fun get(): T {
        return delegate
    }

    fun swap(): T {
        val temp = delegate
        swap.clear()
        delegate = swap
        swap = temp
        return temp
    }
}