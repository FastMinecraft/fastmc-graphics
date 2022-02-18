package me.luna.fastmc.mixin

import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastIntMap
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity

interface IPatchedWorld {
    val unloadedEntitiesOverride: ExtendedBitSet
    val removingEntities: ExtendedBitSet
    val removingEntitiesList: MutableList<Entity>
    val groupedTickableTileEntity: FastIntMap<List<TileEntity>>
    val groupedTileEntity: FastIntMap<List<TileEntity>>

    fun markRemoving(entity: Entity) {
        removingEntities.add(entity.entityId)
        removingEntitiesList.add(entity)
    }

    fun batchRemoveEntities()
}