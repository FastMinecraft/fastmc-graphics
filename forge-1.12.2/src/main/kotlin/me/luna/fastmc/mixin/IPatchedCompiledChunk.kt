package me.luna.fastmc.mixin

import net.minecraft.tileentity.TileEntity

interface IPatchedCompiledChunk {
    val instancingRenderTileEntities: MutableList<TileEntity>
}