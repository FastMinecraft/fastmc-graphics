package dev.fastmc.graphics.mixin

import java.util.*

@Suppress("UNUSED_PARAMETER")
class ReadOnlySet<T> private constructor(val set: MutableSet<T>, dummy: Boolean) : Set<T> by set, MutableSet<T> {
    constructor(set: MutableSet<T>) : this(Collections.unmodifiableSet(set), true)

    override fun add(element: T): Boolean {
        return false
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return false
    }

    override fun clear() {

    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return false
    }

    override fun remove(element: T): Boolean {
        return false
    }

    override fun iterator(): MutableIterator<T> {
        return set.iterator()
    }
}