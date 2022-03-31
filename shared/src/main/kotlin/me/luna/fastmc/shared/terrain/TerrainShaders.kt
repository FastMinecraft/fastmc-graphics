package me.luna.fastmc.shared.terrain

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.*

object TerrainShaders {
    sealed class Shader(type: String) : DrawShader(
        "Terrain$type",
        "/assets/shaders/terrain/Terrain$type.vsh",
        "/assets/shaders/terrain/Terrain.fsh"
    ) {
        init {
            glProgramUniform1i(id, glGetUniformLocation(id, "blockTexture"), 0)
            glProgramUniform1i(id, glGetUniformLocation(id, "lightMapTexture"), FastMcMod.glWrapper.lightMapUnit)
        }

        private val offsetUniform = glGetUniformLocation(id, "offset")
        private val alphaTestUniform = glGetUniformLocation(id, "alphaTest")
        private val forColorUniform = glGetUniformLocation(id, "fogColor")

        fun setOffset(x: Float, y: Float, z: Float) {
            glProgramUniform3f(id, offsetUniform, x, y, z)
        }

        fun setAlphaTest(threshold: Float) {
            glProgramUniform1f(id, alphaTestUniform, threshold)
        }

        fun setFogColor(red: Float, green: Float, blue: Float) {
            glProgramUniform3f(id, forColorUniform, red, green, blue)
        }
    }

    private object Linear : Shader("Linear") {
        val rangeUniform = glGetUniformLocation(id, "range")
    }
    private object Exp : Shader("Exp") {
        val densityUniform = glGetUniformLocation(id, "density")
    }
    private object Exp2 : Shader("Exp2") {
        val densityUniform = glGetUniformLocation(id, "density")
    }

    var shader: Shader = Linear; private set

    fun linear(start: Float, end: Float, red: Float, green: Float, blue: Float) {
        shader = Linear.apply {
            glProgramUniform2f(id, rangeUniform, end, end - start)
            setFogColor(red, green, blue)
        }
    }

    fun exp(density: Float, red: Float, green: Float, blue: Float) {
        shader = Exp.apply {
            glProgramUniform1f(id, densityUniform, density)
            setFogColor(red, green, blue)
        }
    }

    fun exp2(density: Float, red: Float, green: Float, blue: Float) {
        shader = Exp2.apply {
            glProgramUniform1f(id, Exp.densityUniform, density)
            setFogColor(red, green, blue)
        }
    }
}