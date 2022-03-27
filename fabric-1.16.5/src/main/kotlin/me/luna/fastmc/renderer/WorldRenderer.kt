package me.luna.fastmc.renderer

import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.luna.fastmc.mixin.accessor.AccessorWorldRenderer
import me.luna.fastmc.shared.opengl.glBindTexture
import me.luna.fastmc.shared.opengl.glBindVertexArray
import me.luna.fastmc.shared.opengl.glProgramUniform1f
import me.luna.fastmc.shared.opengl.glUseProgramForce
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer
import me.luna.fastmc.shared.resource.IResourceManager
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.MatrixUtils
import me.luna.fastmc.terrain.RegionBuiltChunkStorage
import me.luna.fastmc.util.Minecraft
import me.luna.fastmc.util.OffThreadLightingProvider
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.LightType
import org.lwjgl.opengl.GL11.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class WorldRenderer(private val mc: Minecraft, override val resourceManager: IResourceManager) :
    AbstractWorldRenderer() {
    private val lightUpdate = Long2LongLinkedOpenHashMap()
    private val pendingSkyLightUpdate = LongArrayList()
    private val pendingBlockLightUpdate = LongArrayList()
    private val updating = AtomicBoolean(true)

    fun scheduleLightUpdate(type: LightType, pos: ChunkSectionPos) {
        val longPos = pos.asLong()
        val list = when (type) {
            LightType.SKY -> pendingSkyLightUpdate
            LightType.BLOCK -> pendingBlockLightUpdate
        }
        list.add(longPos)
        list.add(System.currentTimeMillis() + 30000L)
    }

    fun runLightUpdates(): LongArrayList? {
        return if (lightUpdate.isNotEmpty()) {
            val list = LongArrayList()
            val worldRenderer = mc.worldRenderer
            val chunks = (worldRenderer as AccessorWorldRenderer).chunks as RegionBuiltChunkStorage
            val iterator = lightUpdate.long2LongEntrySet().iterator()
            val current = System.currentTimeMillis()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val longPos = entry.longKey
                val x = BlockPos.unpackLongX(longPos)
                val y = BlockPos.unpackLongY(longPos)
                val z = BlockPos.unpackLongZ(longPos)
                val builtChunk = chunks.getRenderedChunk(x, y, z)
                if (builtChunk == null || builtChunk.origin.x shr 4 != x || builtChunk.origin.y shr 4 != y || builtChunk.origin.z shr 4 != z) {
                    iterator.remove()
                } else if (entry.longValue < current || !builtChunk.getData().isEmpty) {
                    for (ix in -1..1) {
                        for (iy in -1..1) {
                            for (iz in -1..1) {
                                list.add(BlockPos.asLong(x + ix, y + iy, z + iz))
                            }
                        }
                    }
                    iterator.remove()
                }
            }
            list
        } else {
            null
        }
    }

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        val world = mc.world
        if (world == null) {
            lightUpdate.clear()
            updating.set(true)
        }

        parentScope.launch(FastMcCoreScope.context) {
            entityRenderer.onPostTick(mainThreadContext, this)
        }
        parentScope.launch(FastMcCoreScope.context) {
            tileEntityRenderer.onPostTick(mainThreadContext, this)
        }

        if (world != null) {
            if (updating.getAndSet(false)) {
                for (i in pendingSkyLightUpdate.indices step 2) {
                    lightUpdate.put(pendingSkyLightUpdate.getLong(i), pendingSkyLightUpdate.getLong(i + 1))
                }
                pendingSkyLightUpdate.clear()

                for (i in pendingBlockLightUpdate.indices step 2) {
                    lightUpdate.put(pendingBlockLightUpdate.getLong(i), pendingBlockLightUpdate.getLong(i + 1))
                }
                pendingBlockLightUpdate.clear()

                val provider = world.chunkManager.lightingProvider as OffThreadLightingProvider
                provider.scheduleUpdate {
                    provider.doLightUpdates()
                    updating.set(true)
                }
            }
        }
    }

    override fun preRender(partialTicks: Float) {
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glUseProgramForce(0)

        MatrixUtils.putMatrix(projectionMatrix)

        resourceManager.entityShader.resources.forEach {
            glProgramUniform1f(it.id, it.partialTicksUniform, partialTicks)
            it.updateProjectionMatrix()
        }

        val cameraPos = mc.gameRenderer.camera.pos
        renderPosX = cameraPos.getX()
        renderPosY = cameraPos.getY()
        renderPosZ = cameraPos.getZ()
    }

    override fun postRender() {
        glBindVertexArray(0)
        glBindTexture(0)
        glUseProgramForce(0)
    }
}