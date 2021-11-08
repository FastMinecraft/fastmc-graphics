package me.xiaro.fastmc.renderer

import me.xiaro.fastmc.shared.opengl.glBindTexture
import me.xiaro.fastmc.shared.opengl.glBindVertexArray
import me.xiaro.fastmc.shared.opengl.glUniform1f
import me.xiaro.fastmc.shared.opengl.glUseProgramForce
import me.xiaro.fastmc.shared.renderer.AbstractWorldRenderer
import me.xiaro.fastmc.shared.resource.IResourceManager
import me.xiaro.fastmc.shared.util.MathUtils
import me.xiaro.fastmc.shared.util.MatrixUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11.*

class WorldRenderer(private val mc: Minecraft, override val resourceManager: IResourceManager) :
    AbstractWorldRenderer() {
    override fun onPostTick() {
        mc.profiler.startSection("tileEntityRenderer")

        tileEntityRenderer.onPostTick()

        mc.profiler.endSection()
    }

    override fun preRender(partialTicks: Float) {
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glUseProgramForce(0)

        MatrixUtils.putMatrix(projectionMatrix)

        resourceManager.tileEntityShader.resources.forEach {
            it.bind()
            glUniform1f(it.partialTicksUniform, partialTicks)
            it.updateProjectionMatrix()
        }

        val entity = mc.renderViewEntity ?: mc.player
        if (entity != null) {
            renderPosX = MathUtils.lerp(entity.lastTickPosX, entity.posX, partialTicks)
            renderPosY = MathUtils.lerp(entity.lastTickPosY, entity.posY, partialTicks)
            renderPosZ = MathUtils.lerp(entity.lastTickPosZ, entity.posZ, partialTicks)
        }
    }

    override fun postRender() {
        glBindVertexArray(0)
        glBindTexture(0)
        glUseProgramForce(0)
    }
}