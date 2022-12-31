package dev.fastmc.graphics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class DistanceSortTest {
    private fun nlogn(n: Double): Double {
        return n * log2(n)
    }

    private fun calcTime(n: Int): Int {
        return min((nlogn(1_000_000.0) * 100.0 / max(nlogn(n.toDouble()), 1.0)).roundToInt(), 1_000)
    }

    private val sizes = listOf(
        1,
        2,
        3,
        4,
        5,
        10,
        100,
        1_000,
        10_000,
        100_000,
        500_000,
        1_000_000
    )

    private val times = sizes.map(::calcTime)

    @Test
    fun allRandom() {
        test { n ->
            IntArray(n) { it }.apply { shuffle() } to FloatArray(n) { Random.nextFloat() }
        }
    }

    @Test
    fun randomIndices() {
        test { n ->
            IntArray(n) { it }.apply { shuffle() } to FloatArray(n) { it.toFloat() }
        }
    }

    @Test
    fun randomDistance() {
        test { n ->
            IntArray(n) { it } to FloatArray(n) { Random.nextFloat() }
        }
    }

    @Test
    fun sorted() {
        test { n ->
            IntArray(n) { it } to FloatArray(n) { it.toFloat() }
        }
    }

    @Test
    fun sameDistances() {
        test { n ->
            val dist = Random.nextFloat()
            IntArray(n) { it } to FloatArray(n) { dist }
        }
    }

    @Test
    fun almostSorted() {
        test { n ->
            val random = java.util.Random()
            IntArray(n) { it } to FloatArray(n) { (it + random.nextGaussian() * 10.0).toFloat() }
        }
    }

    private fun test(
        dataSupplier: (Int) -> Pair<IntArray, FloatArray>,
    ) {
        runBlocking {
            for ((size, n) in sizes zip times) {
                coroutineScope {
                    repeat(n) {
                        launch(Dispatchers.Default) {
                            val (indices, distance) = dataSupplier(size)
                            assert(indices.distinct().size == size)
                            assert(indices.size == size)
                            val sorted = indices.copyOf()
                            sorted.sort()

                            launch {
                                val i = indices.copyOf()
                                val d = distance.copyOf()
                                DistanceSort.sort(i, d, 0, size)
                                assert(d.contentEquals(distance)) { "Distance array changed" }
                                checkSorted(i, d)

                                i.sort()
                                assert(i.contentEquals(sorted)) { "Indices changed" }
                            }

                            launch {
                                val i = indices.copyOf()
                                val d = distance.copyOf()
                                val from = Random.nextInt(size)
                                val to = Random.nextInt(from, size)
                                assert(d.contentEquals(distance)) { "Distance array changed" }
                                checkSorted(i, d)

                                assert(equals(indices, i, 0, from)) { "Indices modified before from" }
                                assert(equals(indices, i, to, size)) { "Indices modified after to" }

                                i.sort()
                                assert(i.contentEquals(sorted)) { "Indices changed" }
                            }
                        }
                    }
                }
                println("Passed testing for size %,d".format(size))
            }
        }
    }

    private fun equals(a: IntArray, b: IntArray, from: Int, to: Int): Boolean {
        for (i in from until to) {
            if (a[i] != b[i]) {
                return false
            }
        }
        return true
    }

    private fun checkSorted(indices: IntArray, distance: FloatArray): Boolean {
        for (i in 1 until indices.size) {
            if (distance[indices[i - 1]] > distance[indices[i]]) {
                return false
            }
        }
        return true
    }
}