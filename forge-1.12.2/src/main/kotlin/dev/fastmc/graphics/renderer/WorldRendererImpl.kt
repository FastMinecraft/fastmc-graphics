package dev.fastmc.graphics.renderer

import dev.fastmc.graphics.shared.opengl.glBindTextureUnit
import dev.fastmc.graphics.shared.opengl.glBindVertexArray
import dev.fastmc.graphics.shared.opengl.glUseProgramForce
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
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
        glUseProgramForce(0)
    }

    override fun postRender() {
        glBindVertexArray(0)
        glBindTextureUnit(0, 0)
        glUseProgramForce(0)
    }
}