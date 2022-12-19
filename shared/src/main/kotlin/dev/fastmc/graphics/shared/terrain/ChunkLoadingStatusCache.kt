package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.BYTE_FALSE
import dev.fastmc.common.BYTE_TRUE

abstract class ChunkLoadingStatusCache(
    cameraChunkX: Int,
    cameraChunkZ: Int,
    private val sizeXZ: Int
) {
    private val array = ByteArray(sizeXZ * sizeXZ)
    private val originX: Int
    private val originZ: Int

    init {
        val offset = (sizeXZ shr 1)
        originX = cameraChunkX - offset
        originZ = cameraChunkZ - offset
    }

    fun isChunkLoaded(chunkX: Int, chunkZ: Int): Boolean {
        val x = chunkX - originX
        val z = chunkZ - originZ
        if (x < 0 || x >= sizeXZ || z < 0 || z >= sizeXZ) {
            return isChunkLoaded0(chunkX, chunkZ)
        }
        val index = x + z * sizeXZ
        return when (array[index]) {
            BYTE_FALSE -> {
                false
            }
            BYTE_TRUE -> {
                true
            }
            else -> {
                val state = isChunkLoaded0(chunkX, chunkZ)
                array[index] = if (state) BYTE_TRUE else BYTE_FALSE
                state
            }
        }
    }

    protected abstract fun isChunkLoaded0(chunkX: Int, chunkZ: Int): Boolean
}