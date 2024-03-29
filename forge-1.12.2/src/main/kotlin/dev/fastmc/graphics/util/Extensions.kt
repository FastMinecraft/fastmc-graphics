@file:Suppress("NOTHING_TO_INLINE")

package dev.fastmc.graphics.util

import dev.fastmc.graphics.mixin.accessor.AccessorMinecraft
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.tileentity.TileEntity

inline val Minecraft.partialTicks: Float
    get() = if (this.isGamePaused) (this as AccessorMinecraft).renderPartialTicksPaused
    else this.renderPartialTicks

@Suppress("UNNECESSARY_SAFE_CALL")
inline val TileEntity.blockState: IBlockState?
    get() = this.world?.getBlockState(this.pos)

@Suppress("UNCHECKED_CAST")
inline fun <T : Comparable<T>> IBlockState?.getPropertyOrNull(property: IProperty<T>): T? {
    return this?.properties?.get(property) as T?
}

@Suppress("UNCHECKED_CAST")
inline fun <T : Comparable<T>> IBlockState?.getPropertyOrDefault(property: IProperty<T>, default: T): T {
    return this?.properties?.get(property) as? T? ?: default
}