package me.luna.fastmc.shared.util

class MD5Hash(array: ByteArray) {
    val a: Long
    val b: Long
    val hash: Int

    init {
        var a = array[0].toLong() shl 56
        a = a or array[1].toLong() shl 48
        a = a or array[2].toLong() shl 40
        a = a or array[3].toLong() shl 32
        a = a or array[4].toLong() shl 24
        a = a or array[5].toLong() shl 16
        a = a or array[6].toLong() shl 8
        a = a or array[7].toLong()
        this.a = a

        var b = array[8].toLong() shl 56
        b = b or array[9].toLong() shl 48
        b = b or array[10].toLong() shl 40
        b = b or array[11].toLong() shl 32
        b = b or array[12].toLong() shl 24
        b = b or array[13].toLong() shl 16
        b = b or array[14].toLong() shl 8
        b = b or array[15].toLong()
        this.b = b

        hash = a.hashCode() * 31 + b.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MD5Hash) return false

        if (a != other.a) return false
        if (b != other.b) return false

        return true
    }

    override fun hashCode(): Int {
        return hash
    }
}