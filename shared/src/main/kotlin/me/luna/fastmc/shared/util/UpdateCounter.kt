package me.luna.fastmc.shared.util

class UpdateCounter {
    @Volatile
    private var updateCount = Int.MIN_VALUE
    private var lastUpdateCount = Int.MIN_VALUE

    fun check(): Boolean {
        val last = lastUpdateCount
        lastUpdateCount = updateCount
        return last != updateCount
    }

    fun reset() {
        lastUpdateCount = updateCount
    }

    fun update() {
        updateCount++
    }
}