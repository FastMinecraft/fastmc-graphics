package dev.fastmc.graphics.terrain

import dev.fastmc.common.isDoneOrNull
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.shared.util.threadGroupMain
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
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class OffThreadLightingProvider(
    chunkProvider: ChunkProvider,
    hasBlockLight: Boolean, hasSkyLight: Boolean
) : LightingProvider(chunkProvider, hasBlockLight, hasSkyLight) {
    @JvmField
    val readWriteLock = ReentrantReadWriteLock(true)

    private var lastLightUpdateFuture: Future<*>? = null

    override fun doLightUpdates(maxUpdateCount: Int, doSkylight: Boolean, skipEdgeLightPropagation: Boolean): Int {
        throw UnsupportedOperationException()
    }

    override fun setSectionStatus(pos: ChunkSectionPos, notReady: Boolean) {
        executor.execute {
            readWriteLock.writeLock().withLock {
                super.setSectionStatus(pos, notReady)
            }
        }
    }

    override fun setSectionStatus(pos: BlockPos, notReady: Boolean) {
        executor.execute {
            readWriteLock.writeLock().withLock {
                super.setSectionStatus(pos, notReady)
            }
        }
    }

    override fun checkBlock(pos: BlockPos?) {
        executor.execute {
            readWriteLock.writeLock().withLock {
                super.checkBlock(pos)
            }
        }
    }

    override fun addLightSource(pos: BlockPos, level: Int) {
        executor.execute {
            readWriteLock.writeLock().withLock {
                super.addLightSource(pos, level)
            }
        }
    }

    override fun setColumnEnabled(pos: ChunkPos, lightEnabled: Boolean) {
        executor.execute {
            readWriteLock.writeLock().withLock {
                super.setColumnEnabled(pos, lightEnabled)
            }
        }
    }

    override fun enqueueSectionData(
        lightType: LightType?,
        pos: ChunkSectionPos?,
        nibbles: ChunkNibbleArray?,
        bl: Boolean
    ) {
        executor.execute {
            readWriteLock.writeLock().withLock {
                super.enqueueSectionData(lightType, pos, nibbles, bl)
            }
        }
    }

    override fun setRetainData(pos: ChunkPos, retainData: Boolean) {
        executor.execute {
            readWriteLock.writeLock().withLock {
                super.setRetainData(pos, retainData)
            }
        }
    }

    fun doLightUpdates(doSkylight: Boolean) {
        if (lastLightUpdateFuture.isDoneOrNull) {
            val hasBlockLightUpdate = hasPendingUpdate(blockLightProvider)
            val hasSkyLightUpdate = hasPendingUpdate(skyLightProvider)
            if (!hasBlockLightUpdate && !hasSkyLightUpdate) return

            lastLightUpdateFuture = executor.submit {
                readWriteLock.writeLock().withLock {
                    val block = FastMcCoreScope.pool.submit {
                        blockLightProvider?.doLightUpdates(Int.MAX_VALUE, doSkylight, true)
                    }
                    val sky = FastMcCoreScope.pool.submit {
                        skyLightProvider?.doLightUpdates(Int.MAX_VALUE, doSkylight, true)
                    }
                    block.get()
                    sky.get()
                }

            }
        }
    }

    private fun hasPendingUpdate(provider: ChunkLightProvider<*, *>?): Boolean {
        return provider != null && (provider.hasPendingUpdates || provider.lightStorage.hasPendingUpdates)
    }

    companion object {
        @JvmField
        val executor = ThreadPoolExecutor(
            0,
            1,
            1L,
            TimeUnit.MINUTES,
            LinkedBlockingQueue(),
            ThreadFactory {
                Thread(threadGroupMain, it, "FastMinecraft-Lighting").apply {
                    priority = 7
                }
            }
        )
    }
}