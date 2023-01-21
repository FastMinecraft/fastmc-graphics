package dev.fastmc.graphics.shared.mixin

import dev.fastmc.graphics.FastMcMod
import dev.fastmc.graphics.FastMcMod.glWrapper
import dev.fastmc.graphics.shared.opengl.*

interface ICoreWorldRenderer {
    val terrainRenderer get() = FastMcMod.worldRenderer.terrainRenderer
    val lightMapTexture: Int

    fun renderLayerPass(layerIndex: Int) {
        val terrainRenderer = terrainRenderer
        preRenderLayer(layerIndex)
        terrainRenderer.renderLayer(layerIndex)
        postRenderTerrain()
    }

    fun preRenderLayer(layerIndex: Int) {
        terrainRenderer.shaderManager.shader.bind()
        bindLightMapTexture()
        bindBlockTexture()
    }

    fun bindBlockTexture()

    fun bindLightMapTexture() {
        glTextureParameteri(lightMapTexture, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTextureParameteri(lightMapTexture, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTextureParameteri(lightMapTexture, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTextureParameteri(lightMapTexture, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glBindTextureUnit(glWrapper.lightMapUnit, lightMapTexture)
    }

    fun postRenderTerrain() {
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0)
        glBindVertexArray(0)
        glUseProgramForce(0)
    }

   fun renderTileEntityFastMc(tickDelta: Float) {
        val worldRenderer = FastMcMod.worldRenderer
        worldRenderer.preRender(tickDelta)
        worldRenderer.tileEntityRenderer.render()
        worldRenderer.postRender()
    }
}