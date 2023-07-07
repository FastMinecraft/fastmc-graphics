package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.EnumMap
import dev.fastmc.common.ceilToInt
import dev.fastmc.common.sq
import dev.fastmc.graphics.FastMcMod
import dev.fastmc.graphics.shared.opengl.*
import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.glwrapper.impl.BufferObject
import dev.luna5ama.glwrapper.impl.ShaderProgram
import dev.luna5ama.glwrapper.impl.ShaderSource
import dev.luna5ama.glwrapper.impl.ShaderSource.Companion.invoke
import dev.luna5ama.kmogus.MemoryStack
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

class TerrainShaderManager(private val renderer: TerrainRenderer) {
    val shader get() = activeShaderGroup

    var fogRangeSq = Int.MAX_VALUE; private set

    private val fogParametersUBO = BufferObject.Immutable().allocate(24, GL_DYNAMIC_STORAGE_BIT)

    private val shaderMap = EnumMap<FogShape, EnumMap<FogType, TerrainShaderProgram>>()
    private var activeFogShape = FogShape.SPHERE
    private var activeShaderGroup = getShader(FogShape.SPHERE, FogType.LINEAR)

    fun checkFogRange(x: Int, y: Int, z: Int): Boolean {
        return activeFogShape.distanceSq(
            renderer.camera.blockX, renderer.camera.blockY, renderer.camera.blockZ,
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
        activeShaderGroup = getShader(fogShape, fogType)
    }

    private fun getShader(
        fogShape: FogShape,
        fogType: FogType
    ): TerrainShaderProgram {
        return shaderMap.getOrPut(fogShape, ::EnumMap).getOrPut(fogType) {
            val vertex = ShaderSource.Vert("/assets/shaders/terrain/Terrain.vert") {
                FogShape.values().forEach {
                    define(it.toString(), it.ordinal)
                }
                FogType.values().forEach {
                    define(it.toString(), it.ordinal)
                }
                define("FOG_SHAPE", fogShape)
                define("FOG_TYPE", fogType)
            }
            val fragment = ShaderSource.Frag("/assets/shaders/terrain/Terrain.frag")

            TerrainShaderProgram(this, vertex, fragment)
        }
    }

    private fun updateFogParametersUBO(
        densityOrM: Float,
        b: Float,
        red: Float,
        green: Float,
        blue: Float
    ) {
        MemoryStack {
            val arr = malloc(fogParametersUBO.size)
            val ptr = arr.ptr

            ptr.setFloatInc(red)
                .setFloatInc( green)
                .setFloatInc(blue)
                .setFloatInc(1.0f)
                .setFloatInc(densityOrM)
                .setFloatInc(b)

            glInvalidateBufferData(fogParametersUBO.id)
            glNamedBufferSubData(fogParametersUBO.id, 0, arr.len, ptr)
        }
    }

    fun destroy() {
        fogParametersUBO.destroy()
    }

    class TerrainShaderProgram(
        private val manager: TerrainShaderManager,
        vertex: ShaderSource.Vert,
        fragment: ShaderSource.Frag
    ) : ShaderProgram(
        vertex,
        fragment {
            define("LIGHT_MAP_UNIT", FastMcMod.lightMapUnit)
        }
    ) {
        private val regionOffsetUniform = glGetUniformLocation(id, "regionOffset")

        override fun bind() {
            super.bind()
            bindBuffer(GL_UNIFORM_BUFFER, manager.renderer.camera.ubo, "Camera")
            bindBuffer(GL_UNIFORM_BUFFER, manager.fogParametersUBO, "FogParameters")
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