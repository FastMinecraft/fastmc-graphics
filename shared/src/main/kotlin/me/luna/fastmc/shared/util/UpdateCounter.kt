package me.luna.fastmc.shared.util

class UpdateCounter {
    @Volatile
    private var updateCount = Int.MIN_VALUE
    private var lastUpdateCount = Int.MIN_VALUE

    fun check(): Boolean {
        val current = updateCount
        val last = lastUpdateCount
        lastUpdateCount = current
        return last != current
    }

    fun update() {
        updateCount++
    }
}