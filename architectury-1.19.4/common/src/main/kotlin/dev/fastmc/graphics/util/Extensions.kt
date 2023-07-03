@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")

package dev.fastmc.graphics.util

import dev.fastmc.graphics.mixin.accessor.AccessorChunkLightProvider
import dev.fastmc.graphics.mixin.accessor.AccessorLevelPropagator
import dev.fastmc.graphics.mixin.accessor.AccessorLightStorage
import net.minecraft.block.BlockState
import net.minecraft.state.property.Property
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.ChunkToNibbleArrayMap
import net.minecraft.world.chunk.light.ChunkLightProvider
import net.minecraft.world.chunk.light.LevelPropagator
import net.minecraft.world.chunk.light.LightStorage

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

inline val ChunkLightProvider<*, *>.lightStorage: LightStorage<*>
    get() = (this as AccessorChunkLightProvider<*>).lightStorage

inline val LevelPropagator.hasPendingUpdates
    get() = (this as AccessorLevelPropagator).invokeHasPendingUpdates()

inline fun <M : ChunkToNibbleArrayMap<M>> LightStorage<M>.getLightSection(
    storage: M,
    sectionPos: Long
): ChunkNibbleArray {
    return (this as AccessorLightStorage<M>).invokeGetLightSection(storage, sectionPos)
}

inline val <M : ChunkToNibbleArrayMap<M>> LightStorage<M>.uncachedStorage: M
    get() = (this as AccessorLightStorage<M>).uncachedStorage