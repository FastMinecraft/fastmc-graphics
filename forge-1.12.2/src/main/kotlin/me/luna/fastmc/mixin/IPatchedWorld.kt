package me.luna.fastmc.mixin

import it.unimi.dsi.fastutil.ints.IntSet
import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastIntMap
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity

interface IPatchedWorld {
    val unloadedEntitiesOverride: IntSet
    val removingEntities: IntSet
    val removingEntitiesList: MutableList<Entity>

    fun markRemoving(entity: Entity) {
        removingEntities.add(entity.entityId)
        removingEntitiesList.add(entity)
    }

    fun batchRemoveEntities()
}