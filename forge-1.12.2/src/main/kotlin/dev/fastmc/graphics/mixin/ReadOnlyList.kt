package dev.fastmc.graphics.mixin

import java.util.*

@Suppress("UNUSED_PARAMETER")
class ReadOnlyList<T> private constructor(val list: MutableList<T>, dummy: Boolean) : List<T> by list, MutableList<T> {
    constructor(list: MutableList<T>) : this(Collections.unmodifiableList(list), true)

    override fun add(element: T): Boolean {
        return false
    }

    override fun add(index: Int, element: T) {

    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        return false
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return false
    }

    override fun clear() {

    }

    override fun removeAt(index: Int): T {
        throw UnsupportedOperationException("Read only list")
    }

    override fun set(index: Int, element: T): T {
        throw UnsupportedOperationException("Read only list")
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

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        return list.subList(fromIndex, toIndex)
    }

    override fun listIterator(): MutableListIterator<T> {
        return list.listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        return list.listIterator(index)
    }

    override fun iterator(): MutableIterator<T> {
        return list.iterator()
    }
}