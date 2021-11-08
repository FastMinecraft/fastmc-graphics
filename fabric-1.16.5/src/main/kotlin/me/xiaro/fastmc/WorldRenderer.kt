package me.xiaro.fastmc

import com.mojang.blaze3d.systems.RenderSystem
import me.xiaro.fastmc.opengl.glBindTexture
import me.xiaro.fastmc.opengl.glBindVertexArray
import me.xiaro.fastmc.opengl.glUniform1f
import me.xiaro.fastmc.opengl.glUseProgramForce
import me.xiaro.fastmc.resource.IResourceManager
import me.xiaro.fastmc.util.MatrixUtils
import org.lwjgl.opengl.GL11.*

class WorldRenderer(private val mc: Minecraft, override val resourceManager: IResourceManager) :
    AbstractWorldRenderer() {
    override fun onPostTick() {
        mc.profiler.startSection("tileEntityRenderer")

        tileEntityRenderer.onPostTick()

        mc.profiler.endSection()
    }

    override fun preRender(partialTicks: Float) {
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glUseProgramForce(0)

        MatrixUtils.putMatrix(projectionMatrix)

        resourceManager.tileEntityShader.resources.forEach {
            it.bind()
            glUniform1f(it.partialTicksUniform, partialTicks)
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