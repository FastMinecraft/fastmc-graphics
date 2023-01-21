package dev.fastmc.graphics.terrain

import dev.fastmc.graphics.shared.terrain.ChunkLoadingStatusCache
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.world.World

class ChunkLoadingStatusCacheImpl : ChunkLoadingStatusCache<WorldClient>() {
    override fun newWorld(): WorldClient? {
        return Minecraft.getMinecraft().world
    }

    override fun isChunkLoaded0(chunkX: Int, chunkZ: Int): Boolean {
        val chunk = world?.chunkProvider?.getLoadedChunk(chunkX, chunkZ)
        return chunk != null && chunk.isLoaded && chunk.isPopulated
    }
}