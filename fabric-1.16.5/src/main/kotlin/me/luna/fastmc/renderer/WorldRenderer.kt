package me.luna.fastmc.renderer

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.luna.fastmc.util.Minecraft
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer
import me.luna.fastmc.shared.resource.IResourceManager
import me.luna.fastmc.shared.util.MatrixUtils
import org.lwjgl.opengl.GL11.*
import kotlin.coroutines.CoroutineContext

class WorldRenderer(private val mc: Minecraft, override val resourceManager: IResourceManager) :
    AbstractWorldRenderer() {

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        parentScope.launch(Dispatchers.Default) {
            entityRenderer.onPostTick(mainThreadContext, this)
        }
        parentScope.launch(Dispatchers.Default) {
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