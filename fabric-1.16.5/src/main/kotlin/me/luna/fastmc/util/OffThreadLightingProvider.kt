package me.luna.fastmc.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.FastMcExtendScope
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.LightType
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.ChunkProvider
import net.minecraft.world.chunk.light.LightingProvider

class OffThreadLightingProvider(
    chunkProvider: ChunkProvider,
    hasBlockLight: Boolean, hasSkyLight: Boolean
) : LightingProvider(chunkProvider, hasBlockLight, hasSkyLight) {
    override fun setSectionStatus(pos: ChunkSectionPos, notReady: Boolean) {
        scheduleUpdate {
            super.setSectionStatus(pos, notReady)
        }
    }

    override fun setSectionStatus(pos: BlockPos, notReady: Boolean) {
        scheduleUpdate {
            super.setSectionStatus(pos, notReady)
        }
    }

    override fun checkBlock(pos: BlockPos?) {
        scheduleUpdate {
            super.checkBlock(pos)
        }
    }

    override fun addLightSource(pos: BlockPos, level: Int) {
        scheduleUpdate {
            super.addLightSource(pos, level)
        }
    }

    override fun setColumnEnabled(pos: ChunkPos, lightEnabled: Boolean) {
        scheduleUpdate {
            super.setColumnEnabled(pos, lightEnabled)
        }
    }

    override fun enqueueSectionData(
        lightType: LightType?,
        pos: ChunkSectionPos?,
        nibbles: ChunkNibbleArray?,
        bl: Boolean
    ) {
        scheduleUpdate {
            super.enqueueSectionData(lightType, pos, nibbles, bl)
        }
    }

    override fun setRetainData(pos: ChunkPos, retainData: Boolean) {
        scheduleUpdate {
            super.setRetainData(pos, retainData)
        }
    }

    suspend fun doLightUpdates() {
        coroutineScope {
            launch(FastMcExtendScope.context) {
                blockLightProvider?.doLightUpdates(Int.MAX_VALUE, true, false)
            }
            launch(FastMcExtendScope.context) {
                skyLightProvider?.doLightUpdates(Int.MAX_VALUE, true, false)
            }
        }
    }

    fun scheduleUpdate(block: suspend OffThreadLightingProvider.() -> Unit): Job {
        return FastMcCoreScope.launch {
            mutex.withLock {
                block.invoke(this@OffThreadLightingProvider)
            }
        }
    }

    private companion object {
        val mutex = Mutex()
    }
}