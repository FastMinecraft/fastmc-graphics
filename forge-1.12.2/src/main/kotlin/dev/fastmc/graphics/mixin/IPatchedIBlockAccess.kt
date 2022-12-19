package dev.fastmc.graphics.mixin

import dev.fastmc.graphics.mixin.accessor.AccessorBiomeColorHelper
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.biome.BiomeColorHelper

interface IPatchedIBlockAccess {
    val thisRef: IBlockAccess; get() = this as IBlockAccess

    fun getBlockState(x: Int, y: Int, z: Int): IBlockState {
        return thisRef.getBlockState(BlockPos(x, y, z))
    }

    fun getColor(pos: BlockPos, colorResolver: BiomeColorHelper.ColorResolver): Int {
        return AccessorBiomeColorHelper.invokeGetColorAtPos(this as IBlockAccess, pos, colorResolver)
    }
}