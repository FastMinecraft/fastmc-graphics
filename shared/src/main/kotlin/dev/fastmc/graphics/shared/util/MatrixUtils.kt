package dev.fastmc.graphics.shared.util

import dev.luna5ama.glwrapper.api.glProgramUniformMatrix4fv
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.copyFrom
import dev.luna5ama.kmogus.copyTo
import org.joml.Matrix4f

object MatrixUtils {
    private val buffer = Arr.malloc(16 * 4L)
    val ptr=  buffer.ptr
    val ptrLong=  ptr.address

    fun putMatrix(matrix: Matrix4f): MatrixUtils {
        matrix.copyTo(ptr)
        return this
    }

    fun getMatrix(): Matrix4f {
        return Matrix4f().apply { copyFrom(ptr) }
    }

    fun uploadMatrix(programID: Int, location: Int) {
        glProgramUniformMatrix4fv(programID, location, 1, false, ptr)
    }
}