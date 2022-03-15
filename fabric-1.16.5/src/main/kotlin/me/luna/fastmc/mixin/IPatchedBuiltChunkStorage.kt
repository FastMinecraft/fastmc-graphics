package me.luna.fastmc.mixin

import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.util.math.BlockPos

interface IPatchedBuiltChunkStorage {
    fun getRenderedChunk(pos: BlockPos): ChunkBuilder.BuiltChunk?
    fun getRenderedChunk(chunkX: Int, chunkY: Int, chunkZ: Int): ChunkBuilder.BuiltChunk?
}