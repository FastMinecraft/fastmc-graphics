package me.luna.fastmc.mixin

import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.util.TileEntity

interface IPatchedChunkData {
    val instancingRenderTileEntities: FastObjectArrayList<TileEntity>

    fun onComplete()
}