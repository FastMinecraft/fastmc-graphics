@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")

package me.luna.fastmc.shared.terrain

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import java.util.*

abstract class WorldSnapshot112<T_Chunk, T_BlockState, T_ColorResolver>(
    @JvmField protected val chunkArray: Array<T_Chunk>,
    @JvmField protected val blockStateArray: Array<T_BlockState>,
    @JvmField protected val emptyBlockState: T_BlockState
) {
    abstract val context: RebuildContext

    var minChunkY = 0; protected set
    var maxChunkY = 16; protected set

    protected var biomeBlendRadius = 0
    private val colorCacheMap = Object2ObjectArrayMap<T_ColorResolver, ColorBlender>()
    private var lastKey: T_ColorResolver? = null
    private var lastValue: ColorBlender? = null

    protected val lightArray = IntArray(20 * 20 * 20) { 0b0000_1111_0000_1111_0000_1111_0000 }
    private val blockColorCache = IntArray(16 * 16 * 16) { Int.MAX_VALUE }

    init {
        blockStateArray.fill(emptyBlockState)
    }

    abstract fun init(): Boolean

    open fun clear() {
        minChunkY = 0
        maxChunkY = 16
        biomeBlendRadius = 0
        Arrays.fill(chunkArray, null)
        blockStateArray.fill(emptyBlockState)
        colorCacheMap.values.forEach {
            it.init()
        }
        lightArray.fill(0b0000_1111_0000_1111_0000_1111_0000)
        blockColorCache.fill(Int.MAX_VALUE)
    }

    fun getChunk(x: Int, z: Int): T_Chunk {
        return chunkArray[(x - (context.chunkX - 1)) * 3 + (z - (context.chunkZ - 1))]
    }

    fun getBlockState0(x: Int, y: Int, z: Int): T_BlockState {
        return blockStateArray[(x - ((context.chunkX shl 4) - 2)) * 400 + (z - ((context.chunkZ shl 4) - 2)) * 20 + (y - ((context.chunkY shl 4) - 2))]
    }

    abstract fun getWorldBrightness(direction: Int, shaded: Boolean): Int

    fun getLightBits(x: Int, y: Int, z: Int): Int {
        return lightArray[indexBlockExt2(x, y, z)]
    }

    inline fun unpackIsOpaqueFullCube(bits: Int): Boolean {
        return bits and 0b0001_0000_0000_0000_0000_0000_0000 != 0
    }

    inline fun unpackAoLight(bits: Int): Int {
        return bits shr 16 and 0xFF
    }

    inline fun unpackLight(bits: Int): Int {
        return bits and 0xFFFF
    }

    inline fun getBlockColor(x: Int, y: Int, z: Int): Int {
        return getBlockColor(x, y, z, getBlockState0(x, y, z))
    }

    fun getBlockColor(x: Int, y: Int, z: Int, blockState: T_BlockState): Int {
        val index = (x and 15 shl 8) or (z and 15 shl 4) or (y and 15)
        var value = blockColorCache[index]
        if (value == Int.MAX_VALUE) {
            value = getRawBlockColor(x, y, z, blockState)
            blockColorCache[index] = value
        }
        return value
    }

    protected abstract fun getRawBlockColor(x: Int, y: Int, z: Int, blockState: T_BlockState): Int

    fun getBlendedColor(x: Int, y: Int, z: Int, colorResolver: T_ColorResolver): Int {
        var colorBlender: ColorBlender?
        if (colorResolver === lastKey) {
            colorBlender = lastValue
        } else {
            colorBlender = colorCacheMap[colorResolver]
            if (colorBlender == null) {
                colorBlender = ColorBlender()
                colorBlender.init()
                colorCacheMap[colorResolver] = colorBlender
            }
        }

        lastKey = colorResolver
        lastValue = colorBlender

        return if (biomeBlendRadius == 0) {
            getColor(x, y, z, colorResolver)
        } else {
            colorBlender!!.getColor(this, colorResolver, x, z, biomeBlendRadius)
        }
    }

    abstract fun getColor(x: Int, y: Int, z: Int, colorResolver: T_ColorResolver): Int

    protected inline fun indexChunk(x: Int, z: Int): Int {
        return (x - (context.chunkX - 1)) * 3 + (z - (context.chunkZ - 1))
    }

    protected inline fun indexBlock(x: Int, y: Int, z: Int): Int {
        return (x and 15 shl 8) or (z and 15 shl 4) or (y and 15)
    }

    protected inline fun indexBlockExt2(x: Int, y: Int, z: Int): Int {
        return (x - ((context.chunkX shl 4) - 2)) * 400 + (z - ((context.chunkZ shl 4) - 2)) * 20 + (y - (((context.chunkY) shl 4) - 2))
    }

    protected inline fun indexBlockExt16(x: Int, z: Int): Int {
        return (x - ((context.chunkX shl 4) - 16)) * 48 + (z - ((context.chunkZ shl 4) - 16))
    }

    companion object {
        inline fun <reified T> chunkArray(): Array<T> {
            return arrayOfNulls<T>(9) as Array<T>
        }

        inline fun <reified T> stateArray(): Array<T> {
            return arrayOfNulls<T>(20 * 20 * 20) as Array<T>
        }

        @JvmField
        val sectionLocalInfo = arrayOf(
            IntArray(8),
            IntArray(64),
            IntArray(8),

            IntArray(64),
            IntArray(512),
            IntArray(64),

            IntArray(8),
            IntArray(64),
            IntArray(8),


            IntArray(64),
            IntArray(512),
            IntArray(64),

            IntArray(512),
            IntArray(4096),
            IntArray(512),

            IntArray(64),
            IntArray(512),
            IntArray(64),


            IntArray(8),
            IntArray(64),
            IntArray(8),

            IntArray(64),
            IntArray(512),
            IntArray(64),

            IntArray(8),
            IntArray(64),
            IntArray(8)
        )

        init {
            val indices = IntArray(sectionLocalInfo.size)

            for (x in 14..33) {
                val sx = x shr 4
                for (z in 14..33) {
                    val sz = z shr 4
                    for (y in 14..33) {
                        val sectionIndex = sx * 9 + sz * 3 + (y shr 4)
                        val packedPos = ((x and 15) shl 8) or ((y and 15) shl 4) or (z and 15)
                        val blockIndex = (x - 14) * 400 + (z - 14) * 20 + (y - 14)
                        sectionLocalInfo[sectionIndex][indices[sectionIndex]++] = (packedPos shl 16) or blockIndex
                    }
                }
            }
        }
    }
}

abstract class WorldSnapshot113<T_Chunk, T_BlockState, T_FluidState, T_ColorResolver>(
    chunkArray: Array<T_Chunk>,
    blockStateArray: Array<T_BlockState>,
    @JvmField protected val fluidStateArray: Array<T_FluidState>,
    emptyBlockState: T_BlockState,
    @JvmField protected val emptyFluidState: T_FluidState
) : WorldSnapshot112<T_Chunk, T_BlockState, T_ColorResolver>(chunkArray, blockStateArray, emptyBlockState) {

    init {
        fluidStateArray.fill(emptyFluidState)
    }

    override fun clear() {
        super.clear()
        fluidStateArray.fill(emptyFluidState)
    }

    fun getFluidState(x: Int, y: Int, z: Int): T_FluidState {
        return fluidStateArray[(x - ((context.chunkX shl 4) - 2)) * 400 + (z - ((context.chunkZ shl 4) - 2)) * 20 + (y - ((context.chunkY shl 4) - 2))]
    }
}