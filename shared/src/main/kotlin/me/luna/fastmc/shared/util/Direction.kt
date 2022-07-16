@file:Suppress("NOTHING_TO_INLINE")

package me.luna.fastmc.shared.util

enum class Direction(val idOpposite: Int, val offsetX: Int, val offsetY: Int, val offsetZ: Int) {
    DOWN(1, 0, -1, 0),
    UP(0, 0, 1, 0),
    NORTH(3, 0, 0, -1),
    SOUTH(2, 0, 0, 1),
    WEST(5, -1, 0, 0),
    EAST(4, 1, 0, 0);

    val opposite get() = VALUES[idOpposite]
    val bit = 1 shl ordinal
    val bitOpposite = 1 shl idOpposite

    companion object {
        const val I_DOWN = 0
        const val I_UP = 1
        const val I_NORTH = 2
        const val I_SOUTH = 3
        const val I_WEST = 4
        const val I_EAST = 5

        const val B_DOWN = 1 shl I_DOWN
        const val B_UP = 1 shl I_UP
        const val B_NORTH = 1 shl I_NORTH
        const val B_SOUTH = 1 shl I_SOUTH
        const val B_WEST = 1 shl I_WEST
        const val B_EAST = 1 shl I_EAST

        @JvmField
        val VALUES = arrayOf(DOWN, UP, NORTH, SOUTH, WEST, EAST)

        @JvmStatic
        inline operator fun get(id: Int): Direction {
            return VALUES[id]
        }
    }
}