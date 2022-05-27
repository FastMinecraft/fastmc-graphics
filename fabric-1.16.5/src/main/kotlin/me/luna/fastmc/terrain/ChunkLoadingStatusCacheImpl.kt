package me.luna.fastmc.terrain

import me.luna.fastmc.shared.terrain.ChunkLoadingStatusCache
import net.minecraft.world.World
import net.minecraft.world.chunk.ChunkStatus

class ChunkLoadingStatusCacheImpl(private val world: World, cameraChunkX: Int, cameraChunkZ: Int, sizeXZ: Int) :
    ChunkLoadingStatusCache(cameraChunkX, cameraChunkZ, sizeXZ) {
    override fun isChunkLoaded0(chunkX: Int, chunkZ: Int): Boolean {
        val chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false)
        return chunk != null && chunk.sectionArray.any { it != null && !it.isEmpty }
    }
}