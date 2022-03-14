package me.luna.fastmc.mixin

import me.luna.fastmc.util.TileEntity
import me.luna.fastmc.shared.util.collection.FastObjectArrayList

interface IPatchedChunkData {
    val instancingRenderTileEntities: FastObjectArrayList<TileEntity>

    fun onComplete()
}