@file:Suppress("NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER")

package me.xiaro.fastmc.util

import kotlin.math.hypot
import kotlin.math.sqrt

inline fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return hypot(x2 - x1, y2 - y1)
}

inline fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
    return hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
}

inline fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    return hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
}


inline fun distanceSq(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return (x2 - x1).sq + (y2 - y1).sq
}

inline fun distanceSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return (x2 - x1).sq + (y2 - y1).sq
}

inline fun distanceSq(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    return (x2 - x1).sq + (y2 - y1).sq
}


inline fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2))
}

inline fun distance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2).toDouble())
}

inline fun distance(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2).toDouble())
}


inline fun distanceSq(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}

inline fun distanceSq(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}

inline fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
    return (x2 - x1).sq + (y2 - y1).sq + (z2 - z1).sq
}