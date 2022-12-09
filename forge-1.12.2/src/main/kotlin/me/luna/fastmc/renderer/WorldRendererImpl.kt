package me.luna.fastmc.renderer

import dev.fastmc.common.FastMcCoreScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.luna.fastmc.shared.opengl.glBindTexture
import me.luna.fastmc.shared.opengl.glBindVertexArray
import me.luna.fastmc.shared.opengl.glUseProgramForce
import me.luna.fastmc.shared.renderer.WorldRenderer
import me.luna.fastmc.shared.resource.IResourceManager
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
        glBindTexture(0)
        glUseProgramForce(0)
    }
}