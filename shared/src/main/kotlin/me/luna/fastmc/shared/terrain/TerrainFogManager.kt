package me.luna.fastmc.shared.terrain

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.renderer.IRenderer
import me.luna.fastmc.shared.util.ceilToInt
import me.luna.fastmc.shared.util.sq
import kotlin.math.ln
import kotlin.math.sqrt

class TerrainFogManager(renderer: IRenderer) {
    val shader: ShaderProgram get() = activeShaderGroup.activeShader
    var fogRangeSq = Int.MAX_VALUE; private set

    private val linear = ShaderGroup(LinearShaderProgram(renderer, false), LinearShaderProgram(renderer, true))
    private val exp = ShaderGroup(ExpShaderProgram(renderer, false), ExpShaderProgram(renderer, true))
    private val exp2 = ShaderGroup(Exp2ShaderProgram(renderer, false), Exp2ShaderProgram(renderer, true))

    private var activeShaderGroup: ShaderGroup<out ShaderProgram> = linear

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

    private class ShaderGroup<T : ShaderProgram>(val normal: T, val alphaTest: T) {
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

    private class LinearShaderProgram(renderer: IRenderer, alphaTest: Boolean) :
        ShaderProgram(renderer, "Linear", alphaTest) {
        val rangeUniform = glGetUniformLocation(id, "range")
    }

    private class ExpShaderProgram(renderer: IRenderer, alphaTest: Boolean) :
        ShaderProgram(renderer, "Exp", alphaTest) {
        val densityUniform = glGetUniformLocation(id, "density")
    }

    private class Exp2ShaderProgram(renderer: IRenderer, alphaTest: Boolean) :
        ShaderProgram(renderer, "Exp2", alphaTest) {
        val densityUniform = glGetUniformLocation(id, "density")
    }

    sealed class ShaderProgram(renderer: IRenderer, fogType: String, alphaTest: Boolean) :
        me.luna.fastmc.shared.opengl.ShaderProgram(
            "Terrain$fogType${if (alphaTest) "AlphaTest" else ""}",
            ShaderSource.Vertex("/assets/shaders/terrain/Terrain.vsh") {
                define("ALPHA_TEST", alphaTest)
            },
            ShaderSource.Fragment("/assets/shaders/terrain/Terrain.fsh") {
                define("FOG_TYPE", fogType)
            }
        ) {
        init {
            glProgramUniform1i(id, glGetUniformLocation(id, "blockTexture"), 0)
            glProgramUniform1i(id, glGetUniformLocation(id, "lightMapTexture"), FastMcMod.glWrapper.lightMapUnit)
            attachUBO(renderer.matricesUBO)
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

    private inline fun <T : ShaderProgram> ShaderGroup<T>.update(block: T.() -> Unit): ShaderGroup<T> {
        block.invoke(this.normal)
        block.invoke(this.alphaTest)
        return this
    }
}