package me.xiaro.fastmc.utils

@JvmInline
value class ColorARGB(val argb: Int) {
    constructor(r: Int, g: Int, b: Int) :
        this(r, g, b, 255)

    constructor(r: Int, g: Int, b: Int, a: Int) :
        this(
            (r and 255 shl 16) or
                (g and 255 shl 8) or
                (b and 255) or
                (a and 255 shl 24)
        )

    constructor(r: Float, g: Float, b: Float) :
        this((r * 255.0f).toInt(), (g * 255.0f).toInt(), (b * 255.0f).toInt())

    constructor(r: Float, g: Float, b: Float, a: Float) :
        this((r * 255.0f).toInt(), (g * 255.0f).toInt(), (b * 255.0f).toInt(), (a * 255.0f).toInt())


    // Int color
    val r: Int
        get() = argb shr 16 and 255

    val g: Int
        get() = argb shr 8 and 255

    val b: Int
        get() = argb and 255

    val a: Int
        get() = argb shr 24 and 255


    // Float color
    val rFloat: Float
        get() = r / 255.0f

    val gFloat: Float
        get() = g / 255.0f

    val bFloat: Float
        get() = b / 255.0f

    val aFloat: Float
        get() = a / 255.0f


    // Modification
    fun red(r: Int): ColorARGB {
        return ColorARGB(argb and 0xFFFFFF or (r shl 24))
    }

    fun green(g: Int): ColorARGB {
        return ColorARGB(argb and -16711681 or (g shl 16))
    }

    fun blue(b: Int): ColorARGB {
        return ColorARGB(argb and -65281 or (b shl 8))
    }

    fun alpha(a: Int): ColorARGB {
        return ColorARGB(argb and -256 or a)
    }

    // Misc
    fun mix(other: ColorARGB, ratio: Float): ColorARGB {
        val rationSelf = 1.0f - ratio
        return ColorARGB(
            (r * rationSelf + other.r * ratio).toInt(),
            (g * rationSelf + other.g * ratio).toInt(),
            (b * rationSelf + other.b * ratio).toInt(),
            (a * rationSelf + other.a * ratio).toInt()
        )
    }

    infix fun mix(other: ColorARGB): ColorARGB {
        return ColorARGB(
            (r + other.r) / 2,
            (g + other.g) / 2,
            (b + other.b) / 2,
            (a + other.a) / 2
        )
    }

    override fun toString(): String {
        return "$r, $g, $b, $a"
    }
}
