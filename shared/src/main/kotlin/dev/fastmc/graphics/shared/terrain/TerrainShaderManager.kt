package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.EnumMap
import dev.fastmc.common.MemoryStack
import dev.fastmc.common.ceilToInt
import dev.fastmc.common.sq
import dev.fastmc.graphics.FastMcMod
import dev.fastmc.graphics.shared.opengl.*
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

class TerrainShaderManager(private val renderer: TerrainRenderer) {
    val drawShader get() = activeDrawShader
    val cullShader = TerrainShaderProgram(
        ShaderSource.Vertex("/assets/shaders/terrain/Cull.vert"),
        ShaderSource.Fragment("/assets/shaders/terrain/Cull.frag"),
        fog = false
    )
    val indirectShader = Array(3) {
        IndirectComputeShaderProgram(it)
    }

    var fogRangeSq = Int.MAX_VALUE; private set

    private val fogParametersUBO = BufferObject.Immutable().allocate(24, GL_DYNAMIC_STORAGE_BIT)

    private val shaderMap = EnumMap<FogShape, EnumMap<FogType, TerrainShaderProgram>>()
    private var activeFogShape = FogShape.SPHERE
    private var activeDrawShader = getShader(FogShape.SPHERE, FogType.LINEAR)

    fun checkFogRange(x: Int, y: Int, z: Int): Boolean {
        return activeFogShape.distanceSq(
            renderer.cameraBlockX, renderer.cameraBlockY, renderer.cameraBlockZ,
            x, y, z
        ) <= renderer.shaderManager.fogRangeSq
    }

    fun linearFog(fogShape: FogShape, start: Float, end: Float, red: Float, green: Float, blue: Float) {
        val m = -(1.0f / (end - start))
        val b = end * -m
        updateFogParametersUBO(m, b, red, green, blue)
        fogRangeSq = (end.toDouble() + 8.0).ceilToInt().sq
        updateActiveShader(fogShape, FogType.LINEAR)
    }

    fun expFog(fogShape: FogShape, density: Float, red: Float, green: Float, blue: Float) {
        updateFogParametersUBO(density, 420.69f, red, green, blue)
        fogRangeSq = (-ln(0.001) / density.toDouble() + 8.0).ceilToInt().sq
        updateActiveShader(fogShape, FogType.EXP)
    }

    fun exp2Fog(fogShape: FogShape, density: Float, red: Float, green: Float, blue: Float) {
        updateFogParametersUBO(density, 420.69f, red, green, blue)
        fogRangeSq = (sqrt(-ln(0.001)) / density.toDouble() + 8.0).ceilToInt().sq
        updateActiveShader(fogShape, FogType.EXP2)
    }

    fun updateActiveShader(fogShape: FogShape, fogType: FogType) {
        activeFogShape = fogShape
        activeDrawShader = getShader(fogShape, fogType)
        indirectShader.forEach {
            glProgramUniform1i(it.id, it.minChunkYUniform, renderer.minChunkY)
        }
    }

    fun destroy() {
        fogParametersUBO.destroy()
        shaderMap.values.forEach { map -> map.values.forEach { it.destroy() } }
        indirectShader.forEach { it.destroy() }
    }

    private fun getShader(
        fogShape: FogShape,
        fogType: FogType
    ): TerrainShaderProgram {
        return shaderMap.getOrPut(fogShape, ::EnumMap).getOrPut(fogType) {
            val vertex = ShaderSource.Vertex("/assets/shaders/terrain/Terrain.vert") {
                FogShape.values().forEach {
                    define(it.toString(), it.ordinal)
                }
                FogType.values().forEach {
                    define(it.toString(), it.ordinal)
                }
                define("FOG_SHAPE", fogShape)
                define("FOG_TYPE", fogType)
            }
            val fragment = ShaderSource.Fragment("/assets/shaders/terrain/Terrain.frag") {
                define("LIGHT_MAP_UNIT", FastMcMod.glWrapper.lightMapUnit)
            }

            TerrainShaderProgram(vertex, fragment)
        }
    }

    private fun updateFogParametersUBO(
        densityOrM: Float,
        b: Float,
        red: Float,
        green: Float,
        blue: Float
    ) {
        MemoryStack.use {
            withMalloc(fogParametersUBO.size) {
                it.putFloat(red)
                it.putFloat(green)
                it.putFloat(blue)
                it.putFloat(1.0f)
                it.putFloat(densityOrM)
                it.putFloat(b)
                it.flip()
                fogParametersUBO.invalidate()
                glNamedBufferSubData(fogParametersUBO.id, 0, it)
            }
        }
    }

    inner class IndirectComputeShaderProgram(pass: Int) : TerrainShaderProgram(
        ShaderSource.Compute("/assets/shaders/terrain/IndirectCompute.comp") {
            define("PASS", pass)
        },
        global = false,
        fog = false
    ) {
        internal val minChunkYUniform = glGetUniformLocation(id, "minChunkY")
    }

    open inner class TerrainShaderProgram(
        vararg sources: ShaderSource,
        private val global: Boolean = true,
        private val fog: Boolean = true
    ) : ShaderProgram(*sources) {
        private val regionOffsetUniform = glGetUniformLocation(id, "regionOffset")

        override fun bind() {
            super.bind()
            if (global) attachBuffer(GL_UNIFORM_BUFFER, renderer.globalUBO, "Global")
            if (fog) attachBuffer(GL_UNIFORM_BUFFER, fogParametersUBO, "FogParameters")
        }

        internal fun setRegionOffset(x: Float, y: Float, z: Float) {
            glProgramUniform3f(id, regionOffsetUniform, x - 0.25f, y - 0.25f, z - 0.25f)
        }
    }

    enum class FogType {
        LINEAR,
        EXP,
        EXP2;

        val fullName = "FOG_TYPE_$name"

        override fun toString(): String {
            return fullName
        }
    }

    enum class FogShape {
        SPHERE {
            override fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
                return dev.fastmc.common.distanceSq(x1, y1, z1, x2, y2, z2)
            }
        },
        CYLINDER {
            override fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
                return dev.fastmc.common.distanceSq(x1, z1, x2, z2)
            }
        };

        val fullName = "FOG_SHAPE_$name"

        abstract fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int

        override fun toString(): String {
            return fullName
        }
    }
}