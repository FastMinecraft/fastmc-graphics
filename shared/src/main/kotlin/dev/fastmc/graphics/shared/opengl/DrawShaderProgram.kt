package dev.fastmc.graphics.shared.opengl

import dev.fastmc.graphics.shared.util.MatrixUtils
import dev.luna5ama.glwrapper.impl.ShaderProgram
import dev.luna5ama.glwrapper.impl.ShaderSource
import org.joml.Matrix4f

open class DrawShaderProgram(vertex: ShaderSource.Vert, fragment: ShaderSource.Frag) :
    ShaderProgram(vertex, fragment) {
    private val projectionUniform = locateUniform("projection")
    private val modelViewUniform = locateUniform("modelView")

    fun updateProjectionMatrix(matrix4f: Matrix4f) {
        MatrixUtils.putMatrix(matrix4f)
        MatrixUtils.uploadMatrix(id, projectionUniform)
    }

    fun updateModelViewMatrix(matrix4f: Matrix4f) {
        MatrixUtils.putMatrix(matrix4f)
        MatrixUtils.uploadMatrix(id, modelViewUniform)
    }
}