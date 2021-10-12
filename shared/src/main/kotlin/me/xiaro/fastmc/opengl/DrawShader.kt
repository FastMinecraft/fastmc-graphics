package me.xiaro.fastmc.opengl

import me.xiaro.fastmc.utils.BufferUtils
import org.joml.Matrix4f
import java.nio.FloatBuffer

open class DrawShader(resourceName: String, vertShaderPath: String, fragShaderPath: String) : Shader(resourceName, vertShaderPath, fragShaderPath) {
    val projectionUniform = glGetUniformLocation(id, "projection")
    val modelViewUniform = glGetUniformLocation(id, "modelView")

    fun updateProjectionMatrix(matrix4f: Matrix4f) {
        matrix4f.get(buffer)
        updateProjectionMatrix(buffer)
    }

    fun updateModelViewMatrix(matrix4f: Matrix4f) {
        matrix4f.get(buffer)
        updateModelViewMatrix(buffer)
    }

    fun updateProjectionMatrix(buffer: FloatBuffer) {
        glUniformMatrix4(projectionUniform, false, buffer)
    }

    fun updateModelViewMatrix(buffer: FloatBuffer) {
        glUniformMatrix4(modelViewUniform, false, buffer)
    }

    companion object {
        val buffer = BufferUtils.float(16)
    }
}