package me.xiaro.fastmc.utils

import me.xiaro.fastmc.opengl.glUniformMatrix4
import org.joml.Matrix4f
import java.nio.FloatBuffer

object MatrixUtils {
    val matrixBuffer: FloatBuffer = BufferUtils.float(16)

    fun putMatrix(matrix: Matrix4f): MatrixUtils {
        matrix.get(matrixBuffer)
        return this
    }

    fun getMatrix(): Matrix4f {
        return Matrix4f(matrixBuffer)
    }

    fun getMatrix(matrix: Matrix4f) {
        matrix.set(matrixBuffer)
    }

    fun uploadMatrix(location: Int) {
        glUniformMatrix4(location, false, matrixBuffer)
    }
}