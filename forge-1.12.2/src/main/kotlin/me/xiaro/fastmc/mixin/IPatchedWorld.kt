package me.xiaro.fastmc.mixin

import me.xiaro.fastmc.shared.util.collection.ExtendedBitSet
import me.xiaro.fastmc.shared.util.collection.FastIntMap
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