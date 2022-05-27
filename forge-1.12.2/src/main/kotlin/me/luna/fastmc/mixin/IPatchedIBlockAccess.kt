package me.luna.fastmc.mixin

import me.luna.fastmc.mixin.accessor.AccessorBiomeColorHelper
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.biome.BiomeColorHelper

interface IPatchedIBlockAccess {
    fun getColor(pos: BlockPos, colorResolver: BiomeColorHelper.ColorResolver): Int {
        return AccessorBiomeColorHelper.invokeGetColorAtPos(this as IBlockAccess, pos, colorResolver)
    }
}