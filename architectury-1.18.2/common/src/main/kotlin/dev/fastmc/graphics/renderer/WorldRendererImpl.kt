package dev.fastmc.graphics.renderer

import com.mojang.blaze3d.systems.RenderSystem
import dev.fastmc.graphics.mixin.accessor.AccessorBufferRenderer
import dev.fastmc.graphics.shared.opengl.glBindTexture
import dev.fastmc.graphics.shared.opengl.glBindVertexArray
import dev.fastmc.graphics.shared.opengl.glUseProgramForce
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.util.Minecraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.lwjgl.opengl.GL11.*
import kotlin.coroutines.CoroutineContext

class WorldRendererImpl(private val mc: Minecraft, override val resourceManager: IResourceManager) :
    WorldRenderer() {

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        parentScope.launch(FastMcCoreScope.context) {
            entityRenderer.onPostTick(mainThreadContext, this)
        }
        parentScope.launch(FastMcCoreScope.context) {
            tileEntityRenderer.onPostTick(mainThreadContext, this)
        }
    }

    override fun preRender(partialTicks: Float) {
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
    }

    override fun postRender() {
        AccessorBufferRenderer.setCurrentVertexArray(0)
        glBindVertexArray(0)
        glBindTexture(0)
        glUseProgramForce(0)
    }
}