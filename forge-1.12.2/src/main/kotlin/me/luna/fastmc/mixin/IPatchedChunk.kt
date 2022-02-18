package me.luna.fastmc.mixin

import net.minecraft.util.math.BlockPos
import net.minecraft.world.EnumSkyBlock

interface IPatchedChunk {
    fun getLightFor(type: EnumSkyBlock, x: Int, y: Int, z: Int): Int

    fun canSeeSky(x: Int, y: Int, z: Int): Boolean
}