package me.luna.fastmc.terrain

import me.luna.fastmc.shared.terrain.ChunkLoadingStatusCache
import net.minecraft.world.World

class ChunkLoadingStatusCacheImpl(private val world: World, cameraChunkX: Int, cameraChunkZ: Int, sizeXZ: Int) :
    ChunkLoadingStatusCache(cameraChunkX, cameraChunkZ, sizeXZ) {
    override fun isChunkLoaded0(chunkX: Int, chunkZ: Int): Boolean {
        val chunk = world.chunkProvider.getLoadedChunk(chunkX, chunkZ)
        return chunk != null && chunk.isLoaded && chunk.isPopulated
    }
}