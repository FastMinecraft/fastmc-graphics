package me.luna.fastmc.terrain

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.common.primitives.Longs
import me.luna.fastmc.shared.util.Splitmix64Random
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.random.RandomSplitter
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom

class WrappedSplitmix64Random(val javaRandom: Splitmix64Random = Splitmix64Random()): net.minecraft.util.math.random.Random {
    override fun split(): net.minecraft.util.math.random.Random {
        return WrappedSplitmix64Random(Splitmix64Random(javaRandom.nextLong()))
    }

    override fun nextSplitter(): RandomSplitter {
        return Splitter(javaRandom.nextLong())
    }

    override fun setSeed(seed: Long) {
        javaRandom.setSeed(seed)
    }

    override fun nextInt(): Int {
        return javaRandom.nextInt()
    }

    override fun nextInt(bound: Int): Int {
        return javaRandom.nextInt(bound)
    }

    override fun nextLong(): Long {
        return javaRandom.nextLong()
    }

    override fun nextBoolean(): Boolean {
        return javaRandom.nextBoolean()
    }

    override fun nextFloat(): Float {
        return javaRandom.nextFloat()
    }

    override fun nextDouble(): Double {
        return javaRandom.nextDouble()
    }

    override fun nextGaussian(): Double {
        return javaRandom.nextGaussian()
    }

    @Suppress("UnstableApiUsage", "DEPRECATION")
    class Splitter(private val seed: Long) : RandomSplitter {
        override fun split(x: Int, y: Int, z: Int): net.minecraft.util.math.random.Random {
            return WrappedSplitmix64Random(Splitmix64Random(MathHelper.hashCode(x, y, z) xor seed))
        }

        override fun split(seed: String): net.minecraft.util.math.random.Random {
            val bs = MD5_HASHER.hashString(seed, Charsets.UTF_8).asBytes()
            val l = Longs.fromBytes(
                bs[0],
                bs[1], bs[2], bs[3], bs[4], bs[5], bs[6], bs[7]
            )
            val m = Longs.fromBytes(
                bs[8],
                bs[9], bs[10], bs[11], bs[12], bs[13], bs[14], bs[15]
            )
            return Xoroshiro128PlusPlusRandom(l xor m xor this.seed)
        }

        @VisibleForTesting
        override fun addDebugInfo(info: StringBuilder) {
            info.append("seed: ").append(seed)
        }

        companion object {
            private val MD5_HASHER = Hashing.md5()
        }
    }
}