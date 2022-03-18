package me.luna.fastmc.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.luna.fastmc.FastMcMod
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
        channel.trySend {
            super.setSectionStatus(pos, notReady)
        }
    }

    override fun setSectionStatus(pos: BlockPos, notReady: Boolean) {
        channel.trySend {
            super.setSectionStatus(pos, notReady)
        }
    }

    override fun checkBlock(pos: BlockPos?) {
        channel.trySend {
            super.checkBlock(pos)
        }
    }

    override fun addLightSource(pos: BlockPos, level: Int) {
        channel.trySend {
            super.addLightSource(pos, level)
        }
    }

    override fun setColumnEnabled(pos: ChunkPos, lightEnabled: Boolean) {
        channel.trySend {
            super.setColumnEnabled(pos, lightEnabled)
        }
    }

    override fun enqueueSectionData(
        lightType: LightType?,
        pos: ChunkSectionPos?,
        nibbles: ChunkNibbleArray?,
        bl: Boolean
    ) {
        channel.trySend {
            super.enqueueSectionData(lightType, pos, nibbles, bl)
        }
    }

    override fun setRetainData(pos: ChunkPos, retainData: Boolean) {
        channel.trySend {
            super.setRetainData(pos, retainData)
        }
    }

    suspend fun doLightUpdates(doSkylight: Boolean, skipEdgeLightPropagation: Boolean) {
        coroutineScope {
            launch(FastMcExtendScope.context) {
                blockLightProvider?.doLightUpdates(Int.MAX_VALUE, doSkylight, skipEdgeLightPropagation)
            }
            launch(FastMcExtendScope.context) {
                skyLightProvider?.doLightUpdates(Int.MAX_VALUE, doSkylight, skipEdgeLightPropagation)
            }
        }
    }

    fun scheduleUpdate(block: suspend () -> Unit) {
        channel.trySend(block)
    }

    private companion object {
        val channel = Channel<suspend () -> Unit>(capacity = Channel.UNLIMITED)
        val thread = FastMcCoreScope.launch {
            while (true) {
                try {
                    for (block in channel) {
                        block.invoke()
                    }
                } catch (e: Exception) {
                    FastMcMod.logger.error("Lighting update error", e)
                }
            }
        }
    }
}