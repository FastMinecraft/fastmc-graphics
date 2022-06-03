package me.luna.fastmc.shared.terrain

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.opengl.impl.UniformBufferObject
import me.luna.fastmc.shared.util.EnumMap
import me.luna.fastmc.shared.util.ceilToInt
import me.luna.fastmc.shared.util.skip
import me.luna.fastmc.shared.util.sq
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

class TerrainFogManager(private val renderer: TerrainRenderer) {
    val shader: ShaderProgram get() = activeShaderGroup.activeShader
    var fogRangeSq = Int.MAX_VALUE; private set

    private val terrainParametersUBO = UniformBufferObject("TerrainParameters", 24)
    private val shaderMap = EnumMap<FogShape, EnumMap<FogType, ShaderGroup>>()

    private var activeShaderGroup = getShader(FogShape.SPHERE, FogType.LINEAR)

    fun alphaTest(state: Boolean) {
        activeShaderGroup.alphaTest(state)
    }

    fun linearFog(fogShape: FogShape, start: Float, end: Float, red: Float, green: Float, blue: Float) {
        val m = -(1.0f / (end - start))
        val b = end * -m
        updateFogParametersUBO(m, b, red, green, blue)
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
            ShaderGroup(renderer.globalUBO, terrainParametersUBO, fogShape, fogType)
        }
    }

    private fun updateFogParametersUBO(
        densityOrM: Float,
        b: Float,
        red: Float,
        green: Float,
        blue: Float
    ) {
        terrainParametersUBO.update {
            it.putFloat(red)
            it.putFloat(green)
            it.putFloat(blue)
            it.skip(4)
            it.putFloat(densityOrM)
            it.putFloat(b)
        }
    }

    fun destroy() {
        terrainParametersUBO.destroy()
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
                define("LIGHT_MAP_UNIT", FastMcMod.glWrapper.lightMapUnit)
                if (alphaTest) define("ALPHA_TEST")
            }
        ) {
        init {
            attachUBO(matricesUBO)
            attachUBO(fogParameterUBO)
        }

        private val regionOffsetUniform = glGetUniformLocation(id, "regionOffset")

        internal fun setRegionOffset(x: Float, y: Float, z: Float) {
            glProgramUniform3f(id, regionOffsetUniform, x - 0.25f, y - 0.25f, z - 0.25f)
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
        CYLINDER {
            override fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
                return me.luna.fastmc.shared.util.distanceSq(x1, z1, x2, z2)
            }
        };

        val fullName = "FOG_SHAPE_$name"

        abstract fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int
    }
}