package me.luna.fastmc.mixin

import dev.fastmc.common.collection.FastObjectArrayList
import net.minecraft.tileentity.TileEntity

interface IPatchedCompiledChunk {
    val instancingRenderTileEntities: FastObjectArrayList<TileEntity>
}