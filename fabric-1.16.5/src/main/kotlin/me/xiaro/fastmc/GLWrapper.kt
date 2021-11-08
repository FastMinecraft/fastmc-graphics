package me.xiaro.fastmc

import com.mojang.blaze3d.platform.GlStateManager
import me.xiaro.fastmc.opengl.IGLWrapper
import org.lwjgl.opengl.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class GLWrapper : IGLWrapper {
    override val rowMajor = true
    override val lightMapUnit = 2

    // GL11
    override fun glGenTextures(): Int = GL11.glGenTextures()
    override fun glDeleteTextures(texture: Int) = GL11.glDeleteTextures(texture)
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = GL11.glTexParameteri(target, pname, param)
    override fun glTexParameterf(target: Int, pname: Int, param: Float) = GL11.glTexParameterf(target, pname, param)
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteBuffer?) =
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)
    override fun glBindTexture(texture: Int) = GlStateManager.bindTexture(texture)

    override fun glDrawElements(mode: Int, indices_count: Int, type: Int, indices_buffer_offset: Long) =
        GL11.glDrawElements(mode, indices_count, type, indices_buffer_offset)

    // GL15
    override fun glGenBuffers(): Int = GL15.glGenBuffers()
    override fun glDeleteBuffers(buffer: Int) = GL15.glDeleteBuffers(buffer)
    override fun glBindBuffer(target: Int, buffer: Int) = GL15.glBindBuffer(target, buffer)
    override fun glBufferData(target: Int, data: ByteBuffer, usage: Int) = GL15.glBufferData(target, data, usage)

    // GL20
    override fun glEnableVertexAttribArray(index: Int) = GL20.glEnableVertexAttribArray(index)
    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long) = GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer)

    override fun glCreateShader(type: Int): Int = GL20.glCreateShader(type)
    override fun glDeleteShader(shader: Int) = GL20.glDeleteShader(shader)
    override fun glShaderSource(shader: Int, string: String) = GL20.glShaderSource(shader, string)
    override fun glCompileShader(shader: Int) = GL20.glCompileShader(shader)
    override fun glGetShaderi(shader: Int, pname: Int): Int = GL20.glGetShaderi(shader, pname)
    override fun glGetShaderInfoLog(shader: Int, maxLength: Int): String = GL20.glGetShaderInfoLog(shader, maxLength)
    override fun glAttachShader(program: Int, shader: Int) = GL20.glAttachShader(program, shader)
    override fun glDetachShader(program: Int, shader: Int) = GL20.glDetachShader(program, shader)

    override fun glCreateProgram(): Int = GL20.glCreateProgram()
    override fun glDeleteProgram(program: Int) = GL20.glDeleteProgram(program)
    override fun glLinkProgram(program: Int) = GL20.glLinkProgram(program)
    override fun glGetProgrami(program: Int, pname: Int): Int = GL20.glGetProgrami(program, pname)
    override fun glGetProgramInfoLog(program: Int, maxLength: Int): String = GL20.glGetProgramInfoLog(program, maxLength)
    override fun glUseProgram(program: Int) = GL20.glUseProgram(program)

    override fun glGetUniformLocation(program: Int, name: CharSequence): Int = GL20.glGetUniformLocation(program, name)
    override fun glUniform1i(location: Int, v0: Int) = GL20.glUniform1i(location, v0)
    override fun glUniform1f(location: Int, v0: Float) = GL20.glUniform1f(location, v0)
    override fun glUniform3f(location: Int, v0: Float, v1: Float, v2: Float) = GL20.glUniform3f(location, v0, v1, v2)
    override fun glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float) = GL20.glUniform4f(location, v0, v1, v2, v3)
    override fun glUniformMatrix4fv(location: Int, transpose: Boolean, matrices: FloatBuffer) = GL20.glUniformMatrix4fv(location, transpose, matrices)


    // GL30
    override fun glGenVertexArrays(): Int = GL30.glGenVertexArrays()
    override fun glDeleteVertexArrays(array: Int) = GL30.glDeleteVertexArrays(array)
    override fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, pointer: Long) = GL30.glVertexAttribIPointer(index, size, type, stride, pointer)
    override fun glBindVertexArray(array: Int) = GL30.glBindVertexArray(array)

    override fun glGenerateMipmap(target: Int) = GL30.glGenerateMipmap(target)


    // GL31
    override fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int) = GL31.glDrawArraysInstanced(mode, first, count, primcount)

    // GL33
    override fun glVertexAttribDivisor(index: Int, divisor: Int) = GL33.glVertexAttribDivisor(index, divisor)
}