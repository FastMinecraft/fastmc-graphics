@file:Suppress("NOTHING_TO_INLINE")

package me.luna.fastmc.shared.opengl

import me.luna.fastmc.FastMcMod.glWrapper
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

interface IGLWrapper {
    val lightMapUnit: Int

    // GL11
    fun glDeleteTextures(texture: Int)
    fun glBindTexture(texture: Int)
    fun glDrawArrays(mode: Int, first: Int, count: Int)
    fun glDrawElements(mode: Int, indices_count: Int, type: Int, indices_buffer_offset: Long)


    // GL14
    fun glMultiDrawArrays(mode: Int, first: IntBuffer, count: IntBuffer)


    // GL15
    fun glDeleteBuffers(buffer: Int)
    fun glBindBuffer(target: Int, buffer: Int)


    // GL20
    fun glCreateShader(type: Int): Int
    fun glDeleteShader(shader: Int)
    fun glShaderSource(shader: Int, string: CharSequence)
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

    // GL30
    fun glBindVertexArray(array: Int)
    fun glDeleteVertexArrays(array: Int)
    fun glGenerateMipmap(target: Int)


    // GL31
    fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int)

    // GL20
    fun glProgramUniform1i(program: Int, location: Int, v0: Int)
    fun glProgramUniform1f(program: Int, location: Int, v0: Float)
    fun glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float)
    fun glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float)
    fun glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float)
    fun glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, matrices: FloatBuffer)


    // GL32
    fun glFenceSync(condition: Int, flags: Int): Long
    fun glDeleteSync(sync: Long)
    fun glGetSynciv(sync: Long, pname: Int): Int


    // GL43
    fun glInvalidateBufferSubData(buffer: Int, offset: Long, length: Long)
    fun glInvalidateBufferData(buffer: Int)
    fun glMultiDrawArraysIndirect(
        mode: Int,
        indirect: Long,
        primcount: Int,
        stride: Int
    )


    // GL45
    fun glCreateVertexArrays(): Int
    fun glVertexArrayVertexBuffer(vaobj: Int, bindingindex: Int, buffer: Int, offset: Long, stride: Int)
    fun glVertexArrayElementBuffer(vaobj: Int, buffer: Int)
    fun glEnableVertexArrayAttrib(vaobj: Int, index: Int)
    fun glVertexArrayAttribFormat(
        vaobj: Int,
        attribindex: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        relativeoffset: Int
    )

    fun glVertexArrayAttribIFormat(vaobj: Int, attribindex: Int, size: Int, type: Int, relativeoffset: Int)
    fun glVertexArrayBindingDivisor(vaobj: Int, bindingindex: Int, divisor: Int)
    fun glVertexArrayAttribBinding(vaobj: Int, attribindex: Int, bindingindex: Int)

    fun glCreateBuffers(): Int
    fun glNamedBufferStorage(buffer: Int, data: ByteBuffer, flags: Int)
    fun glNamedBufferStorage(buffer: Int, size: Long, flags: Int)
    fun glNamedBufferData(buffer: Int, size: Long, usage: Int)
    fun glNamedBufferData(buffer: Int, data: ByteBuffer, usage: Int)
    fun glNamedBufferSubData(buffer: Int, offset: Long, data: ByteBuffer)
    fun glCopyNamedBufferSubData(readBuffer: Int, writeBuffer: Int, readOffset: Long, writeOffset: Long, size: Long)

    fun glCreateTextures(target: Int): Int
    fun glBindTextureUnit(unit: Int, texture: Int)
    fun glTextureStorage2D(texture: Int, levels: Int, internalformat: Int, width: Int, height: Int)
    fun glTextureSubImage2D(
        texture: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        pixels: ByteBuffer
    )

    fun glTextureParameteri(texture: Int, pname: Int, param: Int)
    fun glTextureParameterf(texture: Int, pname: Int, param: Float)
    fun glMapNamedBuffer(
        buffer: Int,
        access: Int,
        old_buffer: ByteBuffer?
    ): ByteBuffer?

    fun glMapNamedBufferRange(buffer: Int, offset: Long, length: Long, access: Int): ByteBuffer?
    fun glMapNamedBufferRange(
        buffer: Int,
        offset: Long,
        length: Long,
        access: Int,
        old_buffer: ByteBuffer?
    ): ByteBuffer?

    fun glUnmapNamedBuffer(buffer: Int): Boolean

    fun glFlushMappedNamedBufferRange(
        buffer: Int,
        offset: Long,
        length: Long
    )
}

// GL11
const val GL_POINTS = 0x0
const val GL_LINES = 0x1
const val GL_LINE_LOOP = 0x2
const val GL_LINE_STRIP = 0x3
const val GL_TRIANGLES = 0x4
const val GL_TRIANGLE_STRIP = 0x5
const val GL_TRIANGLE_FAN = 0x6
const val GL_QUADS = 0x7
const val GL_QUAD_STRIP = 0x8
const val GL_POLYGON = 0x9

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

inline fun glDeleteTextures(texture: Int) = glWrapper.glDeleteTextures(texture)
inline fun glBindTexture(texture: Int) = glWrapper.glBindTexture(texture)
inline fun glDrawArrays(mode: Int, first: Int, count: Int) = glWrapper.glDrawArrays(mode, first, count)
inline fun glDrawElements(mode: Int, indices_count: Int, type: Int, indices_buffer_offset: Long) =
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

inline fun glMultiDrawArrays(mode: Int, first: IntBuffer, count: IntBuffer) =
    glWrapper.glMultiDrawArrays(mode, first, count)


// GL15
const val GL_ELEMENT_ARRAY_BUFFER = 0x8893
const val GL_ARRAY_BUFFER = 0x8892
const val GL_STREAM_DRAW = 0x88E0
const val GL_STATIC_DRAW = 0x88E4
const val GL_DYNAMIC_DRAW = 0x88E8

const val GL_READ_ONLY = 0x88B8
const val GL_WRITE_ONLY = 0x88B9
const val GL_READ_WRITE = 0x88BA

inline fun glDeleteBuffers(buffer: Int) = glWrapper.glDeleteBuffers(buffer)
inline fun glBindBuffer(target: Int, buffer: Int) = glWrapper.glBindBuffer(target, buffer)


// GL20
const val GL_FRAGMENT_SHADER = 0x8B30
const val GL_VERTEX_SHADER = 0x8B31
const val GL_COMPILE_STATUS = 0x8B81

inline fun glCreateShader(type: Int) = glWrapper.glCreateShader(type)
inline fun glDeleteShader(shader: Int) = glWrapper.glDeleteShader(shader)
inline fun glShaderSource(shader: Int, string: CharSequence) = glWrapper.glShaderSource(shader, string)
inline fun glCompileShader(shader: Int) = glWrapper.glCompileShader(shader)
inline fun glGetShaderi(shader: Int, pname: Int) = glWrapper.glGetShaderi(shader, pname)
inline fun glGetShaderInfoLog(shader: Int, maxLength: Int) = glWrapper.glGetShaderInfoLog(shader, maxLength)
inline fun glAttachShader(program: Int, shader: Int) = glWrapper.glAttachShader(program, shader)
inline fun glDetachShader(program: Int, shader: Int) = glWrapper.glDetachShader(program, shader)

const val GL_LINK_STATUS = 0x8B82

inline fun glCreateProgram() = glWrapper.glCreateProgram()
inline fun glDeleteProgram(program: Int) = glWrapper.glDeleteProgram(program)

inline fun glLinkProgram(program: Int) = glWrapper.glLinkProgram(program)
inline fun glGetProgrami(shader: Int, pname: Int) = glWrapper.glGetProgrami(shader, pname)
inline fun glGetProgramInfoLog(program: Int, maxLength: Int) = glWrapper.glGetProgramInfoLog(program, maxLength)

inline fun glGetUniformLocation(program: Int, name: CharSequence) = glWrapper.glGetUniformLocation(program, name)

var bindProgram = 0

inline fun glUseProgramForce(program: Int) {
    glWrapper.glUseProgram(program)
    bindProgram = program
}

inline fun glUseProgram(program: Int) {
    if (program != bindProgram) {
        glWrapper.glUseProgram(program)
        bindProgram = program
    }
}


// GL30
const val GL_MAP_READ_BIT = 0x1
const val GL_MAP_WRITE_BIT = 0x2
const val GL_MAP_INVALIDATE_RANGE_BIT = 0x4
const val GL_MAP_INVALIDATE_BUFFER_BIT = 0x8
const val GL_MAP_FLUSH_EXPLICIT_BIT = 0x10
const val GL_MAP_UNSYNCHRONIZED_BIT = 0x20

const val GL_R8 = 0x8229
const val GL_COMPRESSED_RED = 0x8225
const val GL_COMPRESSED_RED_RGTC1 = 0x8DBB

inline fun glGenerateMipmap(target: Int) = glWrapper.glGenerateMipmap(target)
inline fun glDeleteVertexArrays(array: Int) = glWrapper.glDeleteVertexArrays(array)
inline fun glBindVertexArray(array: Int) = glWrapper.glBindVertexArray(array)


// GL31
inline fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int) =
    glWrapper.glDrawArraysInstanced(mode, first, count, primcount)


// GL32
const val GL_SYNC_STATUS = 0x9114
const val GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117
const val GL_UNSIGNALED = 0x9118
const val GL_SIGNALED = 0x9119

inline fun glFenceSync(condition: Int, flags: Int): Long = glWrapper.glFenceSync(condition, flags)
inline fun glDeleteSync(sync: Long) = glWrapper.glDeleteSync(sync)
inline fun glGetSynciv(sync: Long, pname: Int): Int = glWrapper.glGetSynciv(sync, pname)


// GL40
const val GL_DRAW_INDIRECT_BUFFER = 0x8F3F


// GL41
inline fun glProgramUniform1i(program: Int, location: Int, v0: Int) =
    glWrapper.glProgramUniform1i(program, location, v0)

inline fun glProgramUniform1f(program: Int, location: Int, v0: Float) =
    glWrapper.glProgramUniform1f(program, location, v0)

inline fun glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float) =
    glWrapper.glProgramUniform2f(program, location, v0, v1)

inline fun glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float) =
    glWrapper.glProgramUniform3f(program, location, v0, v1, v2)

inline fun glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float) =
    glWrapper.glProgramUniform4f(program, location, v0, v1, v2, v3)

inline fun glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, matrices: FloatBuffer) =
    glWrapper.glProgramUniformMatrix4fv(program, location, transpose, matrices)


// GL43
inline fun glInvalidateBufferSubData(buffer: Int, offset: Long, length: Long) =
    glWrapper.glInvalidateBufferSubData(buffer, offset, length)

inline fun glInvalidateBufferData(buffer: Int) = glWrapper.glInvalidateBufferData(buffer)

inline fun glMultiDrawArraysIndirect(
    mode: Int,
    indirect: Long,
    primcount: Int,
    stride: Int
) = glWrapper.glMultiDrawArraysIndirect(mode, indirect, primcount, stride)


// GL44
const val GL_MAP_PERSISTENT_BIT = 0x40
const val GL_MAP_COHERENT_BIT = 0x80
const val GL_DYNAMIC_STORAGE_BIT = 0x100
const val GL_CLIENT_STORAGE_BIT = 0x200


// GL45
inline fun glCreateVertexArrays(): Int = glWrapper.glCreateVertexArrays()
inline fun glVertexArrayElementBuffer(vaobj: Int, buffer: Int) = glWrapper.glVertexArrayElementBuffer(vaobj, buffer)
inline fun glVertexArrayVertexBuffer(vaobj: Int, bindingindex: Int, buffer: Int, offset: Long, stride: Int) =
    glWrapper.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride)

inline fun glEnableVertexArrayAttrib(vaobj: Int, index: Int) = glWrapper.glEnableVertexArrayAttrib(vaobj, index)
inline fun glVertexArrayAttribFormat(
    vaobj: Int,
    attribindex: Int,
    size: Int,
    type: Int,
    normalized: Boolean,
    relativeoffset: Int
) = glWrapper.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset)

inline fun glVertexArrayAttribIFormat(vaobj: Int, attribindex: Int, size: Int, type: Int, relativeoffset: Int) =
    glWrapper.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset)

inline fun glVertexArrayBindingDivisor(vaobj: Int, bindingindex: Int, divisor: Int) =
    glWrapper.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor)

inline fun glVertexArrayAttribBinding(vaobj: Int, attribindex: Int, bindingindex: Int) =
    glWrapper.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex)

inline fun glCreateBuffers(): Int = glWrapper.glCreateBuffers()

inline fun glNamedBufferStorage(buffer: Int, data: ByteBuffer, flags: Int) =
    glWrapper.glNamedBufferStorage(buffer, data, flags)

inline fun glNamedBufferStorage(buffer: Int, size: Long, flags: Int) =
    glWrapper.glNamedBufferStorage(buffer, size, flags)

inline fun glNamedBufferData(buffer: Int, size: Long, usage: Int) =
    glWrapper.glNamedBufferData(buffer, size, usage)

inline fun glNamedBufferData(buffer: Int, data: ByteBuffer, usage: Int) =
    glWrapper.glNamedBufferData(buffer, data, usage)

inline fun glNamedBufferSubData(buffer: Int, offset: Long, data: ByteBuffer) =
    glWrapper.glNamedBufferSubData(buffer, offset, data)

inline fun glCopyNamedBufferSubData(
    readBuffer: Int,
    writeBuffer: Int,
    readOffset: Long,
    writeOffset: Long,
    size: Long
) = glWrapper.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size)

inline fun glCreateTextures(target: Int): Int = glWrapper.glCreateTextures(target)

inline fun glBindTextureUnit(unit: Int, texture: Int) = glWrapper.glBindTextureUnit(unit, texture)

inline fun glTextureStorage2D(texture: Int, levels: Int, internalformat: Int, width: Int, height: Int) =
    glWrapper.glTextureStorage2D(texture, levels, internalformat, width, height)

inline fun glTextureSubImage2D(
    texture: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    width: Int,
    height: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) = glWrapper.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, pixels)

inline fun glTextureParameteri(texture: Int, pname: Int, param: Int) =
    glWrapper.glTextureParameteri(texture, pname, param)

inline fun glTextureParameterf(texture: Int, pname: Int, param: Float) =
    glWrapper.glTextureParameterf(texture, pname, param)

inline fun glMapNamedBuffer(
    buffer: Int,
    access: Int,
    old_buffer: ByteBuffer?
): ByteBuffer? = glWrapper.glMapNamedBuffer(buffer, access, old_buffer)

inline fun glMapNamedBufferRange(buffer: Int, offset: Long, length: Long, access: Int): ByteBuffer? =
    glWrapper.glMapNamedBufferRange(buffer, offset, length, access)

inline fun glMapNamedBufferRange(
    buffer: Int,
    offset: Long,
    length: Long,
    access: Int,
    old_buffer: ByteBuffer?
): ByteBuffer? = glWrapper.glMapNamedBufferRange(buffer, offset, length, access, old_buffer)

inline fun glUnmapNamedBuffer(buffer: Int): Boolean = glWrapper.glUnmapNamedBuffer(buffer)

inline fun glFlushMappedNamedBufferRange(
    buffer: Int,
    offset: Long,
    length: Long
) = glWrapper.glFlushMappedNamedBufferRange(buffer, offset, length)