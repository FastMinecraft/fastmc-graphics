package me.luna.fastmc.terrain

import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.util.TileEntity

class CullingInfo(capacity: Int) {
    val updating = ExtendedBitSet(capacity)
    val visible = ExtendedBitSet(capacity)
    val tileEntity = FastObjectArrayList<TileEntity>()
}