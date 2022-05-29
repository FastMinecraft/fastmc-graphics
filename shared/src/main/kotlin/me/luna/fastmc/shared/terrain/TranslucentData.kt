package me.luna.fastmc.shared.terrain

data class TranslucentData(
    @JvmField val indexData: ByteArray,
    @JvmField val quadCenter: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TranslucentData

        if (!indexData.contentEquals(other.indexData)) return false
        if (!quadCenter.contentEquals(other.quadCenter)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indexData.contentHashCode()
        result = 31 * result + quadCenter.contentHashCode()
        return result
    }
}