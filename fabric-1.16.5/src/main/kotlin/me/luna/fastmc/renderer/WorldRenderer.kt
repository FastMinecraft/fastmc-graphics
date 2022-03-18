package me.luna.fastmc.renderer

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.luna.fastmc.shared.opengl.glBindTexture
import me.luna.fastmc.shared.opengl.glBindVertexArray
import me.luna.fastmc.shared.opengl.glProgramUniform1f
import me.luna.fastmc.shared.opengl.glUseProgramForce
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer
import me.luna.fastmc.shared.resource.IResourceManager
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.MatrixUtils
import me.luna.fastmc.util.Minecraft
import me.luna.fastmc.util.OffThreadLightingProvider
import net.minecraft.util.math.ChunkSectionPos
import org.lwjgl.opengl.GL11.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class WorldRenderer(private val mc: Minecraft, override val resourceManager: IResourceManager) :
    AbstractWorldRenderer() {
    private val lightUpdateQueue = ConcurrentLinkedQueue<ChunkSectionPos>()
    private val updating = AtomicBoolean(true)

    fun scheduleLightUpdate(pos: ChunkSectionPos) {
        lightUpdateQueue.add(pos)
    }

    fun runLightUpdates() {
        val world = mc.world ?: return

        if (updating.getAndSet(false)) {
            var pos = lightUpdateQueue.poll()
            while (pos != null) {
                mc.worldRenderer.scheduleBlockRender(pos.sectionX, pos.sectionY, pos.sectionZ)
                pos = lightUpdateQueue.poll()
            }

            val provider = world.chunkManager.lightingProvider as OffThreadLightingProvider
            provider.scheduleUpdate {
                try {
                    provider.doLightUpdates(doSkylight = true, skipEdgeLightPropagation = false)
                } finally {
                    updating.set(true)
                }
            }
        }
    }

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        val world = mc.world
        if (world == null) {
            lightUpdateQueue.clear()
            updating.set(true)
        }

        parentScope.launch(FastMcCoreScope.context) {
            entityRenderer.onPostTick(mainThreadContext, this)
        }
        parentScope.launch(FastMcCoreScope.context) {
            tileEntityRenderer.onPostTick(mainThreadContext, this)
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