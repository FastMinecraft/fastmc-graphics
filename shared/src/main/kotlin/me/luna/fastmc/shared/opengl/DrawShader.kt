package me.luna.fastmc.shared.opengl

import me.luna.fastmc.shared.util.MatrixUtils
import org.joml.Matrix4f

open class DrawShader(resourceName: String, vertShaderPath: String, fragShaderPath: String) :
    Shader(resourceName, vertShaderPath, fragShaderPath) {
    private val projectionUniform = glGetUniformLocation(id, "projection")
    private val modelViewUniform = glGetUniformLocation(id, "modelView")
    private val offsetUniform = glGetUniformLocation(id, "offset")

    fun updateOffset(x: Double, y: Double, z: Double) {
        glUniform3f(offsetUniform, x.toFloat(), y.toFloat(), z.toFloat())
    }

    fun updateProjectionMatrix(matrix4f: Matrix4f) {
        MatrixUtils.putMatrix(matrix4f)
        updateProjectionMatrix()
    }

    fun updateModelViewMatrix(matrix4f: Matrix4f) {
        MatrixUtils.putMatrix(matrix4f)
        updateModelViewMatrix()
    }

    fun updateProjectionMatrix() {
        MatrixUtils.uploadMatrix(projectionUniform)
    }

    fun updateModelViewMatrix() {
        MatrixUtils.uploadMatrix(modelViewUniform)
    }
}