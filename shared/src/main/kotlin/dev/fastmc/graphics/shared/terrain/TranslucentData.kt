package dev.fastmc.graphics.shared.terrain

data class TranslucentData(
    @JvmField val quadIndices: IntArray,
    @JvmField val quadCenter: FloatArray
) {
    val quadCount get() = quadIndices.size / 6

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TranslucentData

        if (!quadIndices.contentEquals(other.quadIndices)) return false
        if (!quadCenter.contentEquals(other.quadCenter)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quadIndices.contentHashCode()
        result = 31 * result + quadCenter.contentHashCode()
        return result
    }
}