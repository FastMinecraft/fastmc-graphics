package dev.fastmc.graphics.terrain

import dev.fastmc.graphics.shared.terrain.ChunkLoadingStatusCache
import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.world.chunk.ChunkStatus

class ChunkLoadingStatusCacheImpl : ChunkLoadingStatusCache<ClientWorld>() {
    override fun newWorld(): ClientWorld? {
        return MinecraftClient.getInstance().world
    }

    override fun isChunkLoaded0(chunkX: Int, chunkZ: Int): Boolean {
        return world?.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null
    }
}