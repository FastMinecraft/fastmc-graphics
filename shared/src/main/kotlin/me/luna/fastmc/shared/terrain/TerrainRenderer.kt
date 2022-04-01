package me.luna.fastmc.shared.terrain

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.*
import org.joml.Matrix4f

object TerrainRenderer {
    sealed class TerrainShader(type: String, alphaTest: Boolean) : DrawShader(
        "Terrain$type",
        "/assets/shaders/terrain/Terrain$type.vsh",
        "/assets/shaders/terrain/Terrain${if (alphaTest) "AlphaTest" else ""}.fsh"
    ) {
        init {
            glProgramUniform1i(id, glGetUniformLocation(id, "blockTexture"), 0)
            glProgramUniform1i(id, glGetUniformLocation(id, "lightMapTexture"), FastMcMod.glWrapper.lightMapUnit)
        }

        private val offsetUniform = glGetUniformLocation(id, "offset")
        private val forColorUniform = glGetUniformLocation(id, "fogColor")

        fun setOffset(x: Float, y: Float, z: Float) {
            glProgramUniform3f(id, offsetUniform, x, y, z)
        }

        fun setFogColor(red: Float, green: Float, blue: Float) {
            glProgramUniform3f(id, forColorUniform, red, green, blue)
        }
    }

    private class LinearShader(alphaTest: Boolean) : TerrainShader("Linear", alphaTest) {
        val rangeUniform = glGetUniformLocation(id, "range")
    }

    private class ExpShader(alphaTest: Boolean) : TerrainShader("Exp", alphaTest) {
        val densityUniform = glGetUniformLocation(id, "density")
    }

    private class Exp2Shader(alphaTest: Boolean) : TerrainShader("Exp2", alphaTest) {
        val densityUniform = glGetUniformLocation(id, "density")
    }

    private sealed class ShaderGroup<T : TerrainShader>(val normal: T, val alphaTest: T) {
        var active = normal

        fun alphaTest(state: Boolean) {
            active = if (state) {
                alphaTest
            } else {
                normal
            }
        }
    }

    private object Linear : ShaderGroup<LinearShader>(LinearShader(false), LinearShader(true))
    private object Exp : ShaderGroup<ExpShader>(ExpShader(false), ExpShader(true))
    private object Exp2 : ShaderGroup<Exp2Shader>(Exp2Shader(false), Exp2Shader(true))

    private var shaderGroup: ShaderGroup<out TerrainShader> = Linear
    val shader: TerrainShader get() = shaderGroup.active

    fun updateMatrix(projection: Matrix4f, modelView: Matrix4f) {
        shaderGroup.update {
            updateProjectionMatrix(projection)
            updateModelViewMatrix(modelView)
        }
    }

    fun alphaTest(state: Boolean) {
        shaderGroup.alphaTest(state)
    }

    fun linear(start: Float, end: Float, red: Float, green: Float, blue: Float) {
        shaderGroup = Linear.update {
            glProgramUniform2f(id, rangeUniform, end, end - start)
            setFogColor(red, green, blue)
        }
    }

    fun exp(density: Float, red: Float, green: Float, blue: Float) {
        shaderGroup = Exp.update {
            glProgramUniform1f(id, densityUniform, density)
            setFogColor(red, green, blue)
        }
    }

    fun exp2(density: Float, red: Float, green: Float, blue: Float) {
        shaderGroup = Exp2.update {
            glProgramUniform1f(id, densityUniform, density)
            setFogColor(red, green, blue)
        }
    }

    private inline fun <T : TerrainShader> ShaderGroup<T>.update(block: T.() -> Unit): ShaderGroup<T> {
        block.invoke(this.normal)
        block.invoke(this.alphaTest)
        return this
    }
}