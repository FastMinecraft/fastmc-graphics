package me.luna.fastmc.mixin

import me.luna.fastmc.TileEntity
import me.luna.fastmc.shared.util.collection.FastObjectArrayList

interface IPatchedChunkData {
    val instancingRenderTileEntities: FastObjectArrayList<TileEntity>
}