package dev.fastmc.graphics.shared.opengl

import dev.fastmc.graphics.shared.util.MatrixUtils
import org.joml.Matrix4f

open class DrawShaderProgram(vertex: ShaderSource.Vertex, fragment: ShaderSource.Fragment) :
    ShaderProgram(vertex, fragment) {
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