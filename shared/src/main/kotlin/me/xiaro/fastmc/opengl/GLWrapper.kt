package me.xiaro.fastmc.opengl

import me.xiaro.fastmc.FastMcMod.glWrapper
import java.nio.ByteBuffer
import java.nio.FloatBuffer

interface IGLWrapper {
    val rowMajor: Boolean

    // GL11
    fun glGenTextures(): Int
    fun glDeleteTextures(texture: Int)
    fun glTexParameteri(target: Int, pname: Int, param: Int)
    fun glTexParameterf(target: Int, pname: Int, param: Float)
    fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteBuffer?)
    fun glBindTexture(texture: Int)

    fun glDrawElements(mode: Int, indices_count: Int, type: Int, indices_buffer_offset: Long)

    // GL15
    fun glGenBuffers(): Int
    fun glDeleteBuffers(buffer: Int)
    fun glBindBuffer(target: Int, buffer: Int)
    fun glBufferData(target: Int, data: ByteBuffer, usage: Int)

    // GL20
    fun glEnableVertexAttribArray(index: Int)
    fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long)

    fun glCreateShader(type: Int): Int
    fun glDeleteShader(shader: Int)
    fun glShaderSource(shader: Int, string: String)
    fun glCompileShader(shader: Int)
    fun glGetShaderi(shader: Int, pname: Int): Int
    fun glGetShaderInfoLog(shader: Int, maxLength: Int): String
    fun glAttachShader(program: Int, shader: Int)
    fun glDetachShader(program: Int, shader: Int)

    fun glCreateProgram(): Int
    fun glDeleteProgram(program: Int)
    fun glLinkProgram(program: Int)
    fun glGetProgrami(program: Int, pname: Int): Int
    fun glGetProgramInfoLog(program: Int, maxLength: Int): String
    fun glUseProgram(program: Int)

    fun glGetUniformLocation(program: Int, name: CharSequence): Int
    fun glUniform1i(location: Int, v0: Int)
    fun glUniform1f(location: Int, v0: Float)
    fun glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float)
    fun glUniformMatrix4fv(location: Int, transpose: Boolean, matrices: FloatBuffer)

    // GL30
    fun glGenerateMipmap(target: Int)

    fun glGenVertexArrays(): Int
    fun glDeleteVertexArrays(array: Int)
    fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, pointer: Long)
    fun glBindVertexArray(array: Int)


    // GL31
    fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int)

    // GL33
    fun glVertexAttribDivisor(index: Int, divisor: Int)
}

// GL11
const val GL_TRIANGLES = 0x4

const val GL_TEXTURE_2D = 0xDE1

const val GL_BYTE = 0x1400
const val GL_UNSIGNED_BYTE = 0x1401
const val GL_SHORT = 0x1402
const val GL_UNSIGNED_SHORT = 0x1403
const val GL_INT = 0x1404
const val GL_UNSIGNED_INT = 0x1405
const val GL_FLOAT = 0x1406

const val GL_RGBA = 0x1908

const val GL_NEAREST = 0x2600
const val GL_LINEAR = 0x2601
const val GL_NEAREST_MIPMAP_NEAREST = 0x2700
const val GL_LINEAR_MIPMAP_NEAREST = 0x2701
const val GL_NEAREST_MIPMAP_LINEAR = 0x2702
const val GL_LINEAR_MIPMAP_LINEAR = 0x2703
const val GL_TEXTURE_MAG_FILTER = 0x2800
const val GL_TEXTURE_MIN_FILTER = 0x2801
const val GL_TEXTURE_WRAP_S = 0x2802
const val GL_TEXTURE_WRAP_T = 0x2803
const val GL_CLAMP = 0x2900
const val GL_REPEAT = 0x2901

const val GL_RGBA8 = 0x8058

fun glGenTextures(): Int = glWrapper.glGenTextures()
fun glDeleteTextures(texture: Int) = glWrapper.glDeleteTextures(texture)
fun glTexParameteri(target: Int, pname: Int, param: Int) = glWrapper.glTexParameteri(target, pname, param)
fun glTexParameterf(target: Int, pname: Int, param: Float) = glWrapper.glTexParameterf(target, pname, param)
fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteBuffer?) =
    glWrapper.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)
fun glBindTexture(texture: Int) = glWrapper.glBindTexture(texture)

fun glDrawElements(mode: Int, indices_count: Int, type: Int, indices_buffer_offset: Long) =
    glWrapper.glDrawElements(mode, indices_count, type, indices_buffer_offset)


// GL12
const val GL_RED = 0x1903
const val GL_BGRA = 0x80E1

const val GL_CLAMP_TO_EDGE = 0x812F
const val GL_TEXTURE_MIN_LOD = 0x813A
const val GL_TEXTURE_MAX_LOD = 0x813B
const val GL_TEXTURE_BASE_LEVEL = 0x813C
const val GL_TEXTURE_MAX_LEVEL = 0x813D

const val GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367


// GL13
const val GL_COMPRESSED_RGBA = 0x84EE

// GL14
const val GL_TEXTURE_LOD_BIAS = 0x8501


// GL15
const val GL_ELEMENT_ARRAY_BUFFER = 0x8893
const val GL_ARRAY_BUFFER = 0x8892
const val GL_STREAM_DRAW = 0x88E0
const val GL_STATIC_DRAW = 0x88E4
const val GL_DYNAMIC_DRAW = 0x88E8

fun glGenBuffers() = glWrapper.glGenBuffers()
fun glDeleteBuffers(buffer: Int) = glWrapper.glDeleteBuffers(buffer)
fun glBindBuffer(target: Int, buffer: Int) = glWrapper.glBindBuffer(target, buffer)
fun glBufferData(target: Int, data: ByteBuffer, usage: Int) = glWrapper.glBufferData(target, data, usage)


// GL20
fun glEnableVertexAttribArray(index: Int) = glWrapper.glEnableVertexAttribArray(index)
fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long) =
    glWrapper.glVertexAttribPointer(index, size, type, normalized, stride, pointer)

const val GL_FRAGMENT_SHADER = 0x8B30
const val GL_VERTEX_SHADER = 0x8B31
const val GL_COMPILE_STATUS = 0x8B81

fun glCreateShader(type: Int) = glWrapper.glCreateShader(type)
fun glDeleteShader(shader: Int) = glWrapper.glDeleteShader(shader)
fun glShaderSource(shader: Int, string: String) = glWrapper.glShaderSource(shader, string)
fun glCompileShader(shader: Int) = glWrapper.glCompileShader(shader)
fun glGetShaderi(shader: Int, pname: Int) = glWrapper.glGetShaderi(shader, pname)
fun glGetShaderInfoLog(shader: Int, maxLength: Int)  = glWrapper.glGetShaderInfoLog(shader, maxLength)
fun glAttachShader(program: Int, shader: Int) = glWrapper.glAttachShader(program, shader)
fun glDetachShader(program: Int, shader: Int) = glWrapper.glDetachShader(program, shader)

const val GL_LINK_STATUS = 0x8B82

fun glCreateProgram() = glWrapper.glCreateProgram()
fun glDeleteProgram(program: Int) = glWrapper.glDeleteProgram(program)

private var bindProgram = 0

fun glLinkProgram(program: Int) = glWrapper.glLinkProgram(program)
fun glGetProgrami(shader: Int, pname: Int) = glWrapper.glGetProgrami(shader, pname)
fun glGetProgramInfoLog(program: Int, maxLength: Int) = glWrapper.glGetProgramInfoLog(program, maxLength)

fun glGetUniformLocation(program: Int, name: CharSequence) = glWrapper.glGetUniformLocation(program, name)
fun glUniform1i(location: Int, v0: Int) = glWrapper.glUniform1i(location, v0)
fun glUniform1f(location: Int, v0: Float) = glWrapper.glUniform1f(location, v0)
fun glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float) = glWrapper.glUniform4f(location, v0, v1, v2, v3)
fun glUniformMatrix4fv(location: Int, transpose: Boolean, matrices: FloatBuffer) = glWrapper.glUniformMatrix4fv(location, transpose, matrices)

fun glUseProgramForce(program: Int) {
    glWrapper.glUseProgram(program)
    bindProgram = program
}

fun glUseProgram(program: Int) {
    if (program != bindProgram) {
        glWrapper.glUseProgram(program)
        bindProgram = program
    }
}


// GL30
const val GL_R8 = 0x8229
const val GL_COMPRESSED_RED = 0x8225

fun glGenerateMipmap(target: Int) = glWrapper.glGenerateMipmap(target)

fun glGenVertexArrays() = glWrapper.glGenVertexArrays()
fun glDeleteVertexArrays(array: Int) = glWrapper.glDeleteVertexArrays(array)
fun glVertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, pointer: Long) =
    glWrapper.glVertexAttribIPointer(index, size, type, stride, pointer)
fun glBindVertexArray(array: Int) = glWrapper.glBindVertexArray(array)


// GL31
fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int) = glWrapper.glDrawArraysInstanced(mode, first, count, primcount)


// GL33
fun glVertexAttribDivisor(index: Int, divisor: Int) = glWrapper.glVertexAttribDivisor(index, divisor)