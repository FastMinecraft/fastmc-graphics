package me.luna.fastmc.util

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.LightType
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.ChunkProvider
import net.minecraft.world.chunk.light.LightingProvider
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class OffThreadLightingProvider(
    chunkProvider: ChunkProvider,
    hasBlockLight: Boolean, hasSkyLight: Boolean
) : LightingProvider(chunkProvider, hasBlockLight, hasSkyLight) {
    override fun setSectionStatus(pos: ChunkSectionPos, notReady: Boolean) {
        if (checkThread()) {
            super.setSectionStatus(pos, notReady)
        } else {
            queue.offer {
                super.setSectionStatus(pos, notReady)
            }
        }
    }

    override fun setSectionStatus(pos: BlockPos, notReady: Boolean) {
        if (checkThread()) {
            super.setSectionStatus(pos, notReady)
        } else {
            queue.offer {
                super.setSectionStatus(pos, notReady)
            }
        }
    }

    override fun checkBlock(pos: BlockPos?) {
        if (checkThread()) {
            super.checkBlock(pos)
        } else {
            queue.offer {
                super.checkBlock(pos)
            }
        }
    }

    override fun addLightSource(pos: BlockPos, level: Int) {
        if (checkThread()) {
            super.addLightSource(pos, level)
        } else {
            queue.offer {
                super.addLightSource(pos, level)
            }
        }
    }

    override fun setColumnEnabled(pos: ChunkPos, lightEnabled: Boolean) {
        if (checkThread()) {
            super.setColumnEnabled(pos, lightEnabled)
        } else {
            queue.offer {
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
        if (checkThread()) {
            super.enqueueSectionData(lightType, pos, nibbles, bl)
        } else {
            queue.offer {
                super.enqueueSectionData(lightType, pos, nibbles, bl)
            }
        }
    }

    override fun setRetainData(pos: ChunkPos, retainData: Boolean) {
        if (checkThread()) {
            super.setRetainData(pos, retainData)
        } else {
            queue.offer {
                super.setRetainData(pos, retainData)
            }
        }
    }

    fun scheduleUpdate(runnable: Runnable) {
        if (checkThread()) {
            runnable.run()
        } else {
            queue.offer(runnable)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkThread(): Boolean {
        return Thread.currentThread() === thread
    }

    private companion object {
        val queue = LinkedBlockingQueue<Runnable>()
        val thread = Thread({
            while (true) {
                try {
                    queue.poll(1L, TimeUnit.SECONDS)?.run()
                } catch (e: InterruptedException) {
                    queue.clear()
                }
            }
        }, "Lighting Update").apply {
            isDaemon = true
            start()
        }
    }
}