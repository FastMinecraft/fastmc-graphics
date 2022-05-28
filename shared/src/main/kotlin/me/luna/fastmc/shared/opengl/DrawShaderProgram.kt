package me.luna.fastmc.shared.opengl

import me.luna.fastmc.shared.util.MatrixUtils
import org.joml.Matrix4f

open class DrawShaderProgram(resourceName: String, vertShaderPath: String, fragShaderPath: String) :
    ShaderProgram(resourceName, vertShaderPath, fragShaderPath) {
    private val projectionUniform = glGetUniformLocation(id, "projection")
    private val modelViewUniform = glGetUniformLocation(id, "modelView")

    fun updateProjectionMatrix(matrix4f: Matrix4f) {
        MatrixUtils.putMatrix(matrix4f)
        updateProjectionMatrix()
    }

    fun updateModelViewMatrix(matrix4f: Matrix4f) {
        MatrixUtils.putMatrix(matrix4f)
        updateModelViewMatrix()
    }

    fun updateProjectionMatrix() {
        MatrixUtils.uploadMatrix(id, projectionUniform)
    }

    fun updateModelViewMatrix() {
        MatrixUtils.uploadMatrix(id, modelViewUniform)
    }
}