package me.luna.fastmc.terrain

import me.luna.fastmc.shared.util.collection.AtomicByteArray
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.ChunkStatus

@Suppress("NOTHING_TO_INLINE")
class NonEmptyChunkSet(private val world: World, viewDistance: Int, cameraChunkOrigin: BlockPos) {
    private val size = viewDistance * 2 + 3
    private val states = AtomicByteArray(size * size)
    private val originX = (cameraChunkOrigin.x shr 4) - viewDistance - 1
    private val originZ = (cameraChunkOrigin.z shr 4) - viewDistance - 1

    fun isNotEmpty(chunkX: Int, chunkZ: Int): Boolean {
        val index = getIndex(chunkX - originX, chunkZ - originZ)
        if (index < 0 || index >= states.size) return false

        return when (states.get(index)) {
            FALSE -> false
            TRUE -> true
            else -> {
                if (world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null) {
                    states.set(index, TRUE)
                    true
                } else {
                    states.set(index, FALSE)
                    false
                }
            }
        }
    }

    private inline fun getIndex(x: Int, y: Int): Int {
        return x + y * size
    }

    private companion object {
        const val NULL: Byte = 0
        const val FALSE: Byte = 1
        const val TRUE: Byte = 2
    }
}