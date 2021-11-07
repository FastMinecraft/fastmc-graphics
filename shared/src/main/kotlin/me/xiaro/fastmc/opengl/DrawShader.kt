package me.xiaro.fastmc.opengl

import me.xiaro.fastmc.FastMcMod
import me.xiaro.fastmc.util.MatrixUtils
import org.joml.Matrix4f
import java.nio.FloatBuffer

open class DrawShader(resourceName: String, vertShaderPath: String, fragShaderPath: String) : Shader(resourceName, vertShaderPath, fragShaderPath) {
    private val projectionUniform = glGetUniformLocation(id, "projection")
    private val modelViewUniform = glGetUniformLocation(id, "modelView")

    fun updateProjectionMatrix(matrix4f: Matrix4f) {
        matrix4f.get(MatrixUtils.matrixBuffer)
        updateProjectionMatrix(MatrixUtils.matrixBuffer)
    }

    fun updateModelViewMatrix(matrix4f: Matrix4f) {
        matrix4f.get(MatrixUtils.matrixBuffer)
        updateModelViewMatrix(MatrixUtils.matrixBuffer)
    }

    fun updateProjectionMatrix(buffer: FloatBuffer) {
        glUniformMatrix4fv(projectionUniform, FastMcMod.glWrapper.rowMajor, buffer)
    }

    fun updateModelViewMatrix(buffer: FloatBuffer) {
        glUniformMatrix4fv(modelViewUniform, FastMcMod.glWrapper.rowMajor, buffer)
    }
}