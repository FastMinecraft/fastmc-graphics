package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.BYTE_FALSE
import dev.fastmc.common.BYTE_TRUE
import dev.fastmc.common.BYTE_UNCHECKED

abstract class ChunkLoadingStatusCache<T_World> {
    protected var world: T_World? = null
    private var array: ByteArray? = null
    private var originX = Int.MIN_VALUE
    private var originZ = Int.MIN_VALUE
    private var sizeXZ = Int.MIN_VALUE

    abstract fun newWorld(): T_World?

    fun init(cameraChunkX: Int, cameraChunkZ: Int, sizeXZ: Int, force: Boolean) {
        val world = newWorld()
        val offset = (sizeXZ shr 1)
        val originX = cameraChunkX - offset
        val originZ = cameraChunkZ - offset

        if (world != this.world || sizeXZ != this.sizeXZ || originX != this.originX || originZ != this.originZ) {
            this.world = world
            this.originX = originX
            this.originZ = originZ
            this.array = ByteArray(sizeXZ * sizeXZ)
            return
        }

        val array = array ?: return
        if (force) {
            array.fill(BYTE_UNCHECKED)
        } else {
            for (i in array.indices) {
                if (array[i] == BYTE_FALSE) {
                    array[i] = BYTE_UNCHECKED
                }
            }
        }
    }

    fun isChunkLoaded(chunkX: Int, chunkZ: Int): Boolean {
        val array = array ?: return false
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