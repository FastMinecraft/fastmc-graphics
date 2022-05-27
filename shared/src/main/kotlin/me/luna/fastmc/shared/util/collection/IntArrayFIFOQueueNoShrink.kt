package me.luna.fastmc.shared.util.collection

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue

class IntArrayFIFOQueueNoShrink : IntArrayFIFOQueue {
    constructor() : super()
    constructor(capacity: Int) : super(capacity)

    override fun dequeueInt(): Int {
        if (start == end) throw NoSuchElementException()
        val t = array[start]
        if (++start == length) start = 0
        return t
    }

    override fun dequeueLastInt(): Int {
        if (start == end) throw NoSuchElementException()
        if (end == 0) end = length
        return array[--end]
    }
}