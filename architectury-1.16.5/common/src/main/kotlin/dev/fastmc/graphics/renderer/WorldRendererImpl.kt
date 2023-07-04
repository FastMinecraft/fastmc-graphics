package dev.fastmc.graphics.renderer

import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.util.Minecraft
import dev.luna5ama.glwrapper.api.glBindTextureUnit
import dev.luna5ama.glwrapper.api.glBindVertexArray
import dev.luna5ama.glwrapper.api.glBlendFuncSeparate
import dev.luna5ama.glwrapper.api.glUseProgram
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
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun postRender() {
        glBindVertexArray(0)
        glBindTextureUnit(0, 0)
        glUseProgram(0)
    }
}