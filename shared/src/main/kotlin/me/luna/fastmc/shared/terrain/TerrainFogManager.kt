package me.luna.fastmc.shared.terrain

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.opengl.impl.UniformBufferObject
import me.luna.fastmc.shared.renderer.IRenderer
import me.luna.fastmc.shared.util.EnumMap
import me.luna.fastmc.shared.util.ceilToInt
import me.luna.fastmc.shared.util.skip
import me.luna.fastmc.shared.util.sq
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

class TerrainFogManager(private val renderer: IRenderer) {
    val shader: ShaderProgram get() = activeShaderGroup.activeShader
    var fogRangeSq = Int.MAX_VALUE; private set

    private val fogParametersUBO = UniformBufferObject("FogParameters", 24)
    private val shaderMap = EnumMap<FogShape, EnumMap<FogType, ShaderGroup>>()

    private var activeShaderGroup = getShader(FogShape.SPHERE, FogType.LINEAR)

    fun alphaTest(state: Boolean) {
        activeShaderGroup.alphaTest(state)
    }

    fun linearFog(fogShape: FogShape, start: Float, end: Float, red: Float, green: Float, blue: Float) {
        updateFogParametersUBO(end, 1.0f / (end - start), red, green, blue)
        activeShaderGroup = getShader(fogShape, FogType.LINEAR)
        fogRangeSq = (end.toDouble() + 8.0).ceilToInt().sq
    }

    fun expFog(fogShape: FogShape, density: Float, red: Float, green: Float, blue: Float) {
        updateFogParametersUBO(density, 420.69f, red, green, blue)
        activeShaderGroup = getShader(fogShape, FogType.EXP)
        fogRangeSq = (-ln(0.001) / density.toDouble() + 8.0).ceilToInt().sq
    }

    fun exp2Fog(fogShape: FogShape, density: Float, red: Float, green: Float, blue: Float) {
        updateFogParametersUBO(density, 420.69f, red, green, blue)
        activeShaderGroup = getShader(fogShape, FogType.EXP2)
        fogRangeSq = (sqrt(-ln(0.001)) / density.toDouble() + 8.0).ceilToInt().sq
    }

    private fun getShader(fogShape: FogShape, fogType: FogType): ShaderGroup {
        return shaderMap.getOrPut(fogShape, ::EnumMap).getOrPut(fogType) {
            ShaderGroup(renderer.matricesUBO, fogParametersUBO, fogShape, fogType)
        }
    }

    private fun updateFogParametersUBO(
        densityOrEnd: Float,
        inverseRange: Float,
        red: Float,
        green: Float,
        blue: Float
    ) {
        fogParametersUBO.update {
            it.putFloat(red)
            it.putFloat(green)
            it.putFloat(blue)
            it.skip(4)
            it.putFloat(densityOrEnd)
            it.putFloat(inverseRange)
        }
    }

    fun destroy() {
        fogParametersUBO.destroy()
        shaderMap.values.forEach { map ->
            map.values.forEach {
                it.destroy()
            }
        }
    }

    private class ShaderGroup(
        matricesUBO: UniformBufferObject,
        fogParameterUBO: UniformBufferObject,
        fogShape: FogShape,
        fogType: FogType,
    ) {
        private val normal = ShaderProgram(matricesUBO, fogParameterUBO, fogType, fogShape, false)
        private val alphaTest = ShaderProgram(matricesUBO, fogParameterUBO, fogType, fogShape, true)

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

    class ShaderProgram(
        matricesUBO: UniformBufferObject,
        fogParameterUBO: UniformBufferObject,
        fogType: FogType,
        val fogShape: FogShape,
        alphaTest: Boolean
    ) :
        me.luna.fastmc.shared.opengl.ShaderProgram(
            "Terrain$fogType${if (alphaTest) "AlphaTest" else ""}",
            ShaderSource.Vertex("/assets/shaders/terrain/Terrain.vsh") {
                FogType.values().forEach {
                    define(it.fullName, it.ordinal)
                }
                FogShape.values().forEach {
                    define(it.fullName, it.ordinal)
                }
                define("FOG_TYPE", fogType.fullName)
                define("FOG_SHAPE", fogShape.fullName)
            },
            ShaderSource.Fragment("/assets/shaders/terrain/Terrain.fsh") {
                if (alphaTest) define("ALPHA_TEST")
            }
        ) {
        init {
            glProgramUniform1i(id, glGetUniformLocation(id, "blockTexture"), 0)
            glProgramUniform1i(id, glGetUniformLocation(id, "lightMapTexture"), FastMcMod.glWrapper.lightMapUnit)
            attachUBO(matricesUBO)
            attachUBO(fogParameterUBO)
        }

        private val offsetUniform = glGetUniformLocation(id, "offset")

        internal fun setOffset(x: Float, y: Float, z: Float) {
            glProgramUniform3f(id, offsetUniform, x, y, z)
        }
    }

    enum class FogType {
        LINEAR,
        EXP,
        EXP2;

        val fullName = "FOG_TYPE_$name"
    }

    enum class FogShape {
        SPHERE {
            override fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
                return me.luna.fastmc.shared.util.distanceSq(x1, y1, z1, x2, y2, z2)
            }
        },
        CYLINDER{
            override fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
                return me.luna.fastmc.shared.util.distanceSq(x1, z1, x2, z2)
            }
        };

        val fullName = "FOG_SHAPE_$name"

        abstract fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int
    }
}