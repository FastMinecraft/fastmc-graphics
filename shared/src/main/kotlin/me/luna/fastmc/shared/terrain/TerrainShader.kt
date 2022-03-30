package me.luna.fastmc.shared.terrain

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.*

object TerrainShader : DrawShader(
    "terrain",
    "/assets/shaders/Terrain.vsh",
    "/assets/shaders/Terrain.fsh"
) {
   init {
       glProgramUniform1i(id, glGetUniformLocation(id, "blockTexture"), 0)
       glProgramUniform1i(id, glGetUniformLocation(id, "lightMapTexture"), FastMcMod.glWrapper.lightMapUnit)
   }

    private val offsetUniform = glGetUniformLocation(id, "offset")
    private val alphaTestUniform = glGetUniformLocation(id, "alphaTest")

    fun setOffset(x: Float, y: Float, z: Float) {
        glProgramUniform3f(id, offsetUniform, x, y, z)
    }

    fun setAlphaTest(threshold: Float) {
        glProgramUniform1f(id, alphaTestUniform, threshold)
    }
}