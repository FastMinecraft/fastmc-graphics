package me.luna.fastmc.shared.terrain

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.opengl.ShaderSource.Companion.invoke
import me.luna.fastmc.shared.terrain.TerrainShaderManager.AlphaTestShaderGroup.Companion.update
import me.luna.fastmc.shared.util.EnumMap
import me.luna.fastmc.shared.util.MemoryStack
import me.luna.fastmc.shared.util.ceilToInt
import me.luna.fastmc.shared.util.sq
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

class TerrainShaderManager(private val renderer: TerrainRenderer) {
    val shader get() = activeShaderGroup.activeShader

    var fogRangeSq = Int.MAX_VALUE; private set

    private val fogParametersUBO = BufferObject.Immutable().allocate(24, GL_DYNAMIC_STORAGE_BIT)

    private val shaderMap = EnumMap<FogShape, EnumMap<FogType, AlphaTestShaderGroup<DrawShaderProgram>>>()
    private var activeFogShape = FogShape.SPHERE
    private var activeShaderGroup = getShader(FogShape.SPHERE, FogType.LINEAR)

    fun checkFogRange(x: Int, y: Int, z: Int): Boolean {
        return activeFogShape.distanceSq(
            renderer.cameraBlockX, renderer.cameraBlockY, renderer.cameraBlockZ,
            x, y, z
        ) <= renderer.shaderManager.fogRangeSq
    }

    fun alphaTest(state: Boolean) {
        activeShaderGroup.alphaTest(state)
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
    ): AlphaTestShaderGroup<DrawShaderProgram> {
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
            val fragment = ShaderSource.Fragment("/assets/shaders/terrain/Terrain.frag")
            AlphaTestShaderGroup(
                DrawShaderProgram(vertex, fragment, false),
                DrawShaderProgram(vertex, fragment, true)
            ).update {
                attachBufferBinding(GL_UNIFORM_BUFFER, renderer.globalUBO, "Global")
                attachBufferBinding(GL_UNIFORM_BUFFER, fogParametersUBO, "FogParameters")
            }
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

    fun destroy() {
        fogParametersUBO.destroy()
    }

    private class AlphaTestShaderGroup<T : ShaderProgram>(
        private val normal: T,
        private val alphaTest: T
    ) {
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

        companion object {
            inline fun <T : ShaderProgram> AlphaTestShaderGroup<T>.update(block: T.() -> Unit): AlphaTestShaderGroup<T> {
                block.invoke(normal)
                block.invoke(alphaTest)
                return this
            }
        }
    }


    class DrawShaderProgram(
        vertex: ShaderSource.Vertex,
        fragment: ShaderSource.Fragment,
        alphaTest: Boolean
    ) : ShaderProgram(
        vertex,
        fragment {
            define("LIGHT_MAP_UNIT", FastMcMod.glWrapper.lightMapUnit)
            if (alphaTest) define("ALPHA_TEST")
        }
    ) {
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

        override fun toString(): String {
            return fullName
        }
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

        override fun toString(): String {
            return fullName
        }
    }
}