package me.luna.fastmc.util

import me.luna.fastmc.TileEntity
import net.minecraft.block.BlockState
import net.minecraft.state.property.Property

val TileEntity.blockState: BlockState?
    get() = if (this.hasWorld()) cachedState else null

@Suppress("UNCHECKED_CAST")
fun <T : Comparable<T>> BlockState?.getPropertyOrNull(property: Property<T>): T? {
    return this?.entries?.get(property) as T?
}

@Suppress("UNCHECKED_CAST")
fun <T : Comparable<T>> BlockState?.getPropertyOrDefault(property: Property<T>, default: T): T {
    return this?.entries?.get(property) as T? ?: default
}