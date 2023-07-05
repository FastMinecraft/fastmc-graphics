package dev.fastmc.graphics.mixin

import dev.fastmc.common.sq
import dev.fastmc.common.toRadians
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Quaternionf
import org.lwjgl.opengl.GL11.GL_MODELVIEW
import org.lwjgl.opengl.GL11.GL_PROJECTION
import org.lwjgl.util.vector.Quaternion
import java.nio.FloatBuffer
import kotlin.math.sqrt

object FixedFunctionMatrixStacks {
    @JvmField
    val PROJECTION = Matrix4fStack(32)

    @JvmField
    val MODELVIEW = Matrix4fStack(64)

    @JvmField
    var CURRENT = MODELVIEW

    @JvmStatic
    fun matrixMode(mode: Int) {
        when (mode) {
            GL_MODELVIEW -> CURRENT = MODELVIEW
            GL_PROJECTION -> CURRENT = PROJECTION
            else -> {
                //ignore
            }
        }
    }

    @JvmStatic
    fun perspective(fov: Float, aspect: Float, zNear: Float, zFar: Float) {
        check(CURRENT === PROJECTION) { "Must be in projection mode" }
        CURRENT.perspective(fov, aspect, zNear, zFar)
    }

    @JvmStatic
    fun loadIdentity() {
        CURRENT.identity()
    }

    @JvmStatic
    fun pushMatrix() {
        CURRENT.pushMatrix()
    }

    @JvmStatic
    fun popMatrix() {
        CURRENT.popMatrix()
    }

    @JvmStatic
    fun ortho(left: Double, right: Double, bottom: Double, top: Double, zNear: Double, zFar: Double) {
        CURRENT.ortho(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), zNear.toFloat(), zFar.toFloat())
    }

    @JvmStatic
    fun rotate(angle: Float, x: Float, y: Float, z: Float) {
        val invLen = 1.0 / sqrt(x.toDouble().sq + y.toDouble().sq + z.toDouble().sq)
        CURRENT.rotate(angle.toRadians(), (x * invLen).toFloat(), (y * invLen).toFloat(), (z * invLen).toFloat())
    }

    @JvmStatic
    fun scale(x: Float, y: Float, z: Float) {
        CURRENT.scale(x, y, z)
    }

    @JvmStatic
    fun scale(x: Double, y: Double, z: Double) {
        CURRENT.scale(x.toFloat(), y.toFloat(), z.toFloat())
    }

    @JvmStatic
    fun translate(x: Float, y: Float, z: Float) {
        CURRENT.translate(x, y, z)
    }

    @JvmStatic
    fun translate(x: Double, y: Double, z: Double) {
        CURRENT.translate(x.toFloat(), y.toFloat(), z.toFloat())
    }

    private val multTemp = Matrix4f()

    @JvmStatic
    fun multMatrix(matrix: FloatBuffer) {
        multTemp.set(matrix)
        CURRENT.mul(multTemp)
    }

    private val quaternionTemp = Quaternionf()

    @JvmStatic
    fun rotate(quaternion: Quaternion) {
        quaternionTemp.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
        CURRENT.rotate(quaternionTemp)
    }
}