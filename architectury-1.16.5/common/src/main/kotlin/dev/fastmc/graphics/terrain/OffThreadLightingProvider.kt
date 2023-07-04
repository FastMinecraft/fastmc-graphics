package dev.fastmc.graphics.terrain

import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.util.hasPendingUpdates
import dev.fastmc.graphics.util.lightStorage
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.LightType
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.ChunkProvider
import net.minecraft.world.chunk.light.ChunkLightProvider
import net.minecraft.world.chunk.light.LightingProvider
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class OffThreadLightingProvider(
    chunkProvider: ChunkProvider,
    hasBlockLight: Boolean, hasSkyLight: Boolean
) : LightingProvider(chunkProvider, hasBlockLight, hasSkyLight) {
    @JvmField
    val readWriteLock = ReentrantReadWriteLock()

    override fun setSectionStatus(pos: ChunkSectionPos, notReady: Boolean) {
        readWriteLock.writeLock().withLock {
            super.setSectionStatus(pos, notReady)
        }
    }

    override fun setSectionStatus(pos: BlockPos, notReady: Boolean) {
        readWriteLock.writeLock().withLock {
            super.setSectionStatus(pos, notReady)
        }
    }

    override fun checkBlock(pos: BlockPos?) {
        readWriteLock.writeLock().withLock {
            super.checkBlock(pos)
        }
    }

    override fun addLightSource(pos: BlockPos, level: Int) {
        readWriteLock.writeLock().withLock {
            super.addLightSource(pos, level)
        }
    }

    override fun setColumnEnabled(pos: ChunkPos, lightEnabled: Boolean) {
        readWriteLock.writeLock().withLock {
            super.setColumnEnabled(pos, lightEnabled)
        }
    }

    override fun enqueueSectionData(
        lightType: LightType?,
        pos: ChunkSectionPos?,
        nibbles: ChunkNibbleArray?,
        bl: Boolean
    ) {
        readWriteLock.writeLock().withLock {
            super.enqueueSectionData(lightType, pos, nibbles, bl)
        }
    }

    override fun setRetainData(pos: ChunkPos, retainData: Boolean) {
        readWriteLock.writeLock().withLock {
            super.setRetainData(pos, retainData)
        }
    }

    override fun doLightUpdates(maxUpdateCount: Int, doSkylight: Boolean, skipEdgeLightPropagation: Boolean): Int {
        val hasBlockLightUpdate = hasPendingUpdate(blockLightProvider)
        val hasSkyLightUpdate = hasPendingUpdate(skyLightProvider)
        if (!hasBlockLightUpdate && !hasSkyLightUpdate) return maxUpdateCount

        readWriteLock.writeLock().withLock {
            val block = FastMcCoreScope.pool.submit {
                blockLightProvider?.doLightUpdates(maxUpdateCount, doSkylight, skipEdgeLightPropagation)
            }
            val sky = FastMcCoreScope.pool.submit {
                skyLightProvider?.doLightUpdates(maxUpdateCount, doSkylight, skipEdgeLightPropagation)
            }
            block.get()
            sky.get()
        }

        return maxUpdateCount
    }

    private fun hasPendingUpdate(provider: ChunkLightProvider<*, *>?): Boolean {
        return provider != null && (provider.hasPendingUpdates || provider.lightStorage.hasPendingUpdates)
    }
}