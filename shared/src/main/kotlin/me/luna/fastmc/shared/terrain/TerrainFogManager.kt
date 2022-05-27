package me.luna.fastmc.shared.terrain

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.renderer.IRenderer
import me.luna.fastmc.shared.util.ceilToInt
import me.luna.fastmc.shared.util.sq
import kotlin.math.ln
import kotlin.math.sqrt

class TerrainFogManager(private val renderer: IRenderer) {
    val shader: Shader
        get() {
            updateMatrix()
            return activeShaderGroup.activeShader
        }
    var fogRangeSq = Int.MAX_VALUE; private set

    private val linear = ShaderGroup(LinearShader(false), LinearShader(true))
    private val exp = ShaderGroup(ExpShader(false), ExpShader(true))
    private val exp2 = ShaderGroup(Exp2Shader(false), Exp2Shader(true))

    private var activeShaderGroup: ShaderGroup<out Shader> = linear
    private var lastMatrixHash = 0L

    fun alphaTest(state: Boolean) {
        activeShaderGroup.alphaTest(state)
    }

    fun linearFog(start: Float, end: Float, red: Float, green: Float, blue: Float) {
        activeShaderGroup = linear.update {
            glProgramUniform2f(id, rangeUniform, end, 1.0f / (end - start))
            setFogColor(red, green, blue)
        }
        fogRangeSq = (end.toDouble() + 8.0).ceilToInt().sq
    }

    fun expFog(density: Float, red: Float, green: Float, blue: Float) {
        activeShaderGroup = exp.update {
            glProgramUniform1f(id, densityUniform, density)
            setFogColor(red, green, blue)
        }
        fogRangeSq = (-ln(0.001) / density.toDouble() + 8.0).ceilToInt().sq
    }

    fun exp2Fog(density: Float, red: Float, green: Float, blue: Float) {
        activeShaderGroup = exp2.update {
            glProgramUniform1f(id, densityUniform, density)
            setFogColor(red, green, blue)
        }
        fogRangeSq = (sqrt(-ln(0.001)) / density.toDouble() + 8.0).ceilToInt().sq
    }

    fun destroy() {
        linear.destroy()
        exp.destroy()
        exp2.destroy()
    }

    private fun updateMatrix() {
        if (lastMatrixHash != renderer.matrixHash) {
            lastMatrixHash = renderer.matrixHash
            activeShaderGroup.update {
                updateProjectionMatrix(renderer.projectionMatrix)
                updateModelViewMatrix(renderer.modelViewMatrix)
            }
        }
    }

    private class ShaderGroup<T : Shader>(val normal: T, val alphaTest: T) {
        var activeShader = normal

        fun alphaTest(state: Boolean) {
            activeShader = if (state) {
                alphaTest
            } else {
                normal
            }
        }

        fun destroy() {
            normal.destroy()
            alphaTest.destroy()
        }
    }

    private class LinearShader(alphaTest: Boolean) : Shader("Linear", alphaTest) {
        val rangeUniform = glGetUniformLocation(id, "range")
    }

    private class ExpShader(alphaTest: Boolean) : Shader("Exp", alphaTest) {
        val densityUniform = glGetUniformLocation(id, "density")
    }

    private class Exp2Shader(alphaTest: Boolean) : Shader("Exp2", alphaTest) {
        val densityUniform = glGetUniformLocation(id, "density")
    }

    sealed class Shader(type: String, alphaTest: Boolean) : DrawShader(
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

        internal fun setOffset(x: Float, y: Float, z: Float) {
            glProgramUniform3f(id, offsetUniform, x, y, z)
        }

        internal fun setFogColor(red: Float, green: Float, blue: Float) {
            glProgramUniform3f(id, forColorUniform, red, green, blue)
        }
    }

    private inline fun <T : Shader> ShaderGroup<T>.update(block: T.() -> Unit): ShaderGroup<T> {
        block.invoke(this.normal)
        block.invoke(this.alphaTest)
        return this
    }
}