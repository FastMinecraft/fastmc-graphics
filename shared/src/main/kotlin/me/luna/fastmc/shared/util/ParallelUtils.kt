package me.luna.fastmc.shared.util

import kotlin.math.max

object ParallelUtils {
    val CPU_THREADS = Runtime.getRuntime().availableProcessors()
    const val DEFAULT_MIN_SIZE = 128

    inline fun splitListIndex(
        total: Int,
        parallelism: Int = CPU_THREADS,
        minSize: Int = DEFAULT_MIN_SIZE,
        blockForEach: (Int, Int) -> Unit
    ) {
        splitListIndex(total, parallelism, minSize, blockForEach, blockForEach)
    }

    inline fun splitListIndex(
        total: Int,
        parallelism: Int = CPU_THREADS,
        minSize: Int = DEFAULT_MIN_SIZE,
        blockForEach: (Int, Int) -> Unit,
        blockForRemaining: (Int, Int) -> Unit
    ) {
        val parallelSize = max(total / parallelism, minSize)
        val combineThreshold = parallelSize / 2

        if (total <= parallelSize + combineThreshold) {
            blockForRemaining.invoke(0, total)
        } else {
            var index = 0

            while (index < total) {
                val start = index
                val end = index + parallelSize
                val remaining = total - end

                if (remaining < 0 || remaining < combineThreshold) {
                    blockForRemaining.invoke(start, total)
                    break
                } else {
                    blockForEach.invoke(start, end)
                    index = end
                }
            }
        }
    }
}