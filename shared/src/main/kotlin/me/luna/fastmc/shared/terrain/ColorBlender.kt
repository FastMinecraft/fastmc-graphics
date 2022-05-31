@file:Suppress("NOTHING_TO_INLINE")

package me.luna.fastmc.shared.terrain

class ColorBlender {
    private val colorArray = ByteArray(48 * 48 * 3)
    private val swapArray = ByteArray(48 * 48 * 3)
    private val resultArray = IntArray(16 * 16)
    private var initialized = false

    fun init() {
        initialized = false
    }

    fun <T> getColor(
        worldSnapshot: WorldSnapshot112<*, *, T>,
        colorResolver: T,
        x: Int,
        z: Int,
        blendRadius: Int
    ): Int {
        if (!initialized) {
            GaussianKernel.VALUES[(blendRadius * 2) - 1].compute(this, worldSnapshot, colorResolver)
            initialized = true
        }
        return resultArray[((x and 15) shl 4) or (z and 15)]
    }

    private enum class GaussianKernel(vararg sequence: Int) {
        R1(1, 2),
        R2(28, 40, 45),
        R3(351204, 367024, 376855, 380190),
        R4(888418544, 901401696, 910791297, 916471908, 918373302),
        R5(540178485, 581032320, 614925872, 640336032, 656082000, 661416000),
        R6(1122648516, 1216916712, 1299888306, 1368303480, 1419359580, 1450900904, 1461569293),
        R7(553270671, 679455210, 808317405, 931620060, 1040309067, 1125580302, 1180043865, 1198774720),
        R8(473498025, 618446400, 779242464, 947314368, 1111272624, 1258044480, 1374530080, 1449504448, 1475388456),
        R9(
            349660080,
            473498025,
            618446400,
            779242464,
            947314368,
            1111272624,
            1258044480,
            1374530080,
            1449504448,
            1475388456
        ),
        R10(
            49550878,
            74326317,
            106724968,
            146746831,
            193276314,
            243896301,
            294944364,
            341867331,
            379852590,
            404625585,
            413234640
        ),
        R11(
            55772505,
            89236008,
            136332790,
            198972180,
            277513830,
            370018440,
            471773511,
            575333550,
            671222475,
            749271600,
            800358300,
            818144040
        ),
        R12(
            33267810,
            55772505,
            89236008,
            136332790,
            198972180,
            277513830,
            370018440,
            471773511,
            575333550,
            671222475,
            749271600,
            800358300,
            818144040
        ),
        R13(
            105570,
            281520,
            686205,
            1533870,
            3152955,
            5974020,
            10454535,
            16926390,
            25389585,
            35324640,
            45627660,
            54753192,
            61070868,
            63332752
        ),
        R14(
            355810,
            1094800,
            3049800,
            7726160,
            17866745,
            37835460,
            73568950,
            131649700,
            217222005,
            331004960,
            466416080,
            608368800,
            735112300,
            823325776,
            854992152
        );

        private val multiplier = intArrayOf(*sequence, *sequence.copyOfRange(0, sequence.size - 1).reversedArray())
        private val samples = multiplier.size - 1
        private val radius = sequence.size - 1
        private val divisor = multiplier.sumOf { it.toLong() }
        private val offsets = IntArray(multiplier.size) {
            it - radius
        }

        fun <T> compute(
            colorBlender: ColorBlender,
            worldSnapshot: WorldSnapshot112<*, *, T>,
            colorResolver: T
        ) {
            val context = worldSnapshot.context
            val colorArray = colorBlender.colorArray
            val swapArray = colorBlender.swapArray
            val resultArray = colorBlender.resultArray

            val originX = (context.chunkX - 1) shl 4
            val originZ = (context.chunkZ - 1) shl 4
            val y = (context.chunkY shl 4) + 8

            var startX = context.chunkX shl 4
            var startZ = context.chunkZ shl 4
            var endX = startX + 15
            var endZ = startZ + 15

            startX -= radius
            startZ -= radius
            endX += radius
            endZ += radius

            for (x in startX..endX) {
                for (z in startZ..endZ) {
                    val index = (x - originX) * 144 + (z - originZ) * 3
                    val color = worldSnapshot.getColor(x, y, z, colorResolver)
                    colorArray[index] = (color shr 16).toByte()
                    colorArray[index + 1] = ((color shr 8) and 0xFF).toByte()
                    colorArray[index + 2] = (color and 0xFF).toByte()
                }
            }

            startX += radius
            endX -= radius
            for (x in startX..endX) {
                for (z in startZ..endZ) {
                    val baseIndex = (x - originX) * 144 + (z - originZ) * 3

                    var r = 0L
                    var g = 0L
                    var b = 0L

                    for (i in 0..samples) {
                        val index = baseIndex + offsets[i] * 144
                        val multiplier = multiplier[i].toLong()
                        r += (colorArray[index].toInt() and 0xFF) * multiplier
                        g += (colorArray[index + 1].toInt() and 0xFF) * multiplier
                        b += (colorArray[index + 2].toInt() and 0xFF) * multiplier
                    }

                    swapArray[baseIndex] = (r / divisor).toByte()
                    swapArray[baseIndex + 1] = (g / divisor).toByte()
                    swapArray[baseIndex + 2] = (b / divisor).toByte()
                }
            }

            startZ += radius
            endZ -= radius
            for (x in startX..endX) {
                for (z in startZ..endZ) {
                    val baseIndex = (x - originX) * 144 + (z - originZ) * 3

                    var r = 0L
                    var g = 0L
                    var b = 0L

                    for (i in 0..samples) {
                        val index = baseIndex + offsets[i] * 3
                        val multiplier = multiplier[i].toLong()
                        r += (swapArray[index].toInt() and 0xFF) * multiplier
                        g += (swapArray[index + 1].toInt() and 0xFF) * multiplier
                        b += (swapArray[index + 2].toInt() and 0xFF) * multiplier
                    }

                    resultArray[((x and 15) shl 4) or (z and 15)] =
                        ((r / divisor).toInt() shl 16) or ((g / divisor).toInt() shl 8) or (b / divisor).toInt()
                }
            }
        }

        companion object {
            @JvmField
            val VALUES = values()
        }
    }
}
