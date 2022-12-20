@file:Suppress("NOTHING_TO_INLINE")

package dev.fastmc.graphics.shared.opengl

import dev.fastmc.graphics.FastMcMod.glWrapper
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

interface IGLWrapper {
    val lightMapUnit: Int

    // GL11
    fun glClear(mask: Int)
    fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun glClearDepth(depth: Double)

    fun glDeleteTextures(texture: Int)
    fun glBindTexture(texture: Int)
    fun glDrawArrays(mode: Int, first: Int, count: Int)
    fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long)


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
    fun glBindBufferBase(target: Int, index: Int, buffer: Int)
    fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Long, size: Long)

    fun glDeleteFramebuffers(framebuffer: Int)
    fun glBindFramebuffer(target: Int, framebuffer: Int)


    // GL20
    fun glProgramUniform1i(program: Int, location: Int, v0: Int)
    fun glProgramUniform1f(program: Int, location: Int, v0: Float)
    fun glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float)
    fun glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float)
    fun glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float)
    fun glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, matrices: FloatBuffer)


    // GL31
    fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int)
    fun glGetUniformBlockIndex(program: Int, uniformBlockName: CharSequence): Int
    fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int)

    // GL32
    fun glFenceSync(condition: Int, flags: Int): Long
    fun glDeleteSync(sync: Long)
    fun glGetSynciv(sync: Long, pname: Int): Int


    // GL42
    fun glMemoryBarrier(barriers: Int)


    // GL43
    fun glInvalidateBufferSubData(buffer: Int, offset: Long, length: Long)
    fun glInvalidateBufferData(buffer: Int)
    fun glMultiDrawArraysIndirect(
        mode: Int,
        indirect: Long,
        primcount: Int,
        stride: Int
    )

    fun glMultiDrawElementsIndirect(
        mode: Int,
        type: Int,
        indirect: Long,
        primcount: Int,
        stride: Int
    )

    fun glShaderStorageBlockBinding(program: Int, storageBlockIndex: Int, storageBlockBinding: Int)
    fun glGetProgramResourceIndex(program: Int, programInterface: Int, name: CharSequence): Int


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

    fun glCreateFramebuffers(): Int

    fun glCheckNamedFramebufferStatus(
        framebuffer: Int,
        target: Int
    ): Int

    fun glNamedFramebufferRenderbuffer(
        framebuffer: Int,
        attachment: Int,
        renderbuffertarget: Int,
        renderbuffer: Int
    )

    fun glNamedFramebufferTexture(
        framebuffer: Int,
        attachment: Int,
        texture: Int,
        level: Int
    )

    fun glNamedFramebufferDrawBuffers(framebuffer: Int, bufs: IntArray)

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

    fun glClearNamedBufferData(buffer: Int, internalformat: Int, format: Int, type: Int, data: ByteBuffer?)
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

const val GL_DEPTH_BUFFER_BIT = 0x100
const val GL_STENCIL_BUFFER_BIT = 0x400
const val GL_COLOR_BUFFER_BIT = 0x4000

inline fun glClear(mask: Int) = glWrapper.glClear(mask)
inline fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) =
    glWrapper.glClearColor(red, green, blue, alpha)

inline fun glClearDepth(depth: Double) = glWrapper.glClearDepth(depth)

inline fun glDeleteTextures(texture: Int) = glWrapper.glDeleteTextures(texture)
inline fun glBindTexture(texture: Int) = glWrapper.glBindTexture(texture)
inline fun glDrawArrays(mode: Int, first: Int, count: Int) = glWrapper.glDrawArrays(mode, first, count)
inline fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long) =
    glWrapper.glDrawElements(mode, count, type, indices)


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
const val GL_TEXTURE0 = 0x84C0
const val GL_TEXTURE1 = 0x84C1
const val GL_TEXTURE2 = 0x84C2
const val GL_TEXTURE3 = 0x84C3
const val GL_TEXTURE4 = 0x84C4
const val GL_TEXTURE5 = 0x84C5
const val GL_TEXTURE6 = 0x84C6
const val GL_TEXTURE7 = 0x84C7
const val GL_TEXTURE8 = 0x84C8
const val GL_TEXTURE9 = 0x84C9
const val GL_TEXTURE10 = 0x84CA
const val GL_TEXTURE11 = 0x84CB
const val GL_TEXTURE12 = 0x84CC
const val GL_TEXTURE13 = 0x84CD
const val GL_TEXTURE14 = 0x84CE
const val GL_TEXTURE15 = 0x84CF
const val GL_TEXTURE16 = 0x84D0
const val GL_TEXTURE17 = 0x84D1
const val GL_TEXTURE18 = 0x84D2
const val GL_TEXTURE19 = 0x84D3
const val GL_TEXTURE20 = 0x84D4
const val GL_TEXTURE21 = 0x84D5
const val GL_TEXTURE22 = 0x84D6
const val GL_TEXTURE23 = 0x84D7
const val GL_TEXTURE24 = 0x84D8
const val GL_TEXTURE25 = 0x84D9
const val GL_TEXTURE26 = 0x84DA
const val GL_TEXTURE27 = 0x84DB
const val GL_TEXTURE28 = 0x84DC
const val GL_TEXTURE29 = 0x84DD
const val GL_TEXTURE30 = 0x84DE
const val GL_TEXTURE31 = 0x84DF

const val GL_COMPRESSED_RGBA = 0x84EE


// GL14
const val GL_TEXTURE_LOD_BIAS = 0x8501

const val GL_DEPTH_COMPONENT16 = 0x81A5
const val GL_DEPTH_COMPONENT24 = 0x81A6
const val GL_DEPTH_COMPONENT32 = 0x81A7

inline fun glMultiDrawArrays(mode: Int, first: IntBuffer, count: IntBuffer) =
    glWrapper.glMultiDrawArrays(mode, first, count)


// GL15
const val GL_ARRAY_BUFFER = 0x8892
const val GL_ELEMENT_ARRAY_BUFFER = 0x8893
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
const val GL_R16 = 0x822A
const val GL_RG8 = 0x822B
const val GL_RG16 = 0x822C
const val GL_R16F = 0x822D
const val GL_R32F = 0x822E
const val GL_RG16F = 0x822F
const val GL_RG32F = 0x8230
const val GL_R8I = 0x8231
const val GL_R8UI = 0x8232
const val GL_R16I = 0x8233
const val GL_R16UI = 0x8234
const val GL_R32I = 0x8235
const val GL_R32UI = 0x8236
const val GL_RG8I = 0x8237
const val GL_RG8UI = 0x8238
const val GL_RG16I = 0x8239
const val GL_RG16UI = 0x823A
const val GL_RG32I = 0x823B
const val GL_RG32UI = 0x823C

const val GL_RED_INTEGER = 0x8D94
const val GL_GREEN_INTEGER = 0x8D95
const val GL_BLUE_INTEGER = 0x8D96
const val GL_ALPHA_INTEGER = 0x8D97
const val GL_RGB_INTEGER = 0x8D98
const val GL_RGBA_INTEGER = 0x8D99
const val GL_BGR_INTEGER = 0x8D9A
const val GL_BGRA_INTEGER = 0x8D9B

const val GL_COMPRESSED_RED = 0x8225
const val GL_COMPRESSED_RED_RGTC1 = 0x8DBB

const val GL_FRAMEBUFFER = 0x8D40
const val GL_READ_FRAMEBUFFER = 0x8CA8
const val GL_DRAW_FRAMEBUFFER = 0x8CA9

const val GL_FRAMEBUFFER_COMPLETE = 0x8CD5
const val GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6
const val GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7
const val GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 0x8CDB
const val GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 0x8CDC
const val GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD
const val GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = 0x8D56
const val GL_FRAMEBUFFER_UNDEFINED = 0x8219

const val GL_COLOR_ATTACHMENT0 = 0x8CE0
const val GL_COLOR_ATTACHMENT1 = 0x8CE1
const val GL_COLOR_ATTACHMENT2 = 0x8CE2
const val GL_COLOR_ATTACHMENT3 = 0x8CE3
const val GL_COLOR_ATTACHMENT4 = 0x8CE4
const val GL_COLOR_ATTACHMENT5 = 0x8CE5
const val GL_COLOR_ATTACHMENT6 = 0x8CE6
const val GL_COLOR_ATTACHMENT7 = 0x8CE7
const val GL_COLOR_ATTACHMENT8 = 0x8CE8
const val GL_COLOR_ATTACHMENT9 = 0x8CE9
const val GL_COLOR_ATTACHMENT10 = 0x8CEA
const val GL_COLOR_ATTACHMENT11 = 0x8CEB
const val GL_COLOR_ATTACHMENT12 = 0x8CEC
const val GL_COLOR_ATTACHMENT13 = 0x8CED
const val GL_COLOR_ATTACHMENT14 = 0x8CEE
const val GL_COLOR_ATTACHMENT15 = 0x8CEF
const val GL_COLOR_ATTACHMENT16 = 0x8CF0
const val GL_COLOR_ATTACHMENT17 = 0x8CF1
const val GL_COLOR_ATTACHMENT18 = 0x8CF2
const val GL_COLOR_ATTACHMENT19 = 0x8CF3
const val GL_COLOR_ATTACHMENT20 = 0x8CF4
const val GL_COLOR_ATTACHMENT21 = 0x8CF5
const val GL_COLOR_ATTACHMENT22 = 0x8CF6
const val GL_COLOR_ATTACHMENT23 = 0x8CF7
const val GL_COLOR_ATTACHMENT24 = 0x8CF8
const val GL_COLOR_ATTACHMENT25 = 0x8CF9
const val GL_COLOR_ATTACHMENT26 = 0x8CFA
const val GL_COLOR_ATTACHMENT27 = 0x8CFB
const val GL_COLOR_ATTACHMENT28 = 0x8CFC
const val GL_COLOR_ATTACHMENT29 = 0x8CFD
const val GL_COLOR_ATTACHMENT30 = 0x8CFE
const val GL_COLOR_ATTACHMENT31 = 0x8CFF

const val GL_DEPTH_COMPONENT32F = 0x8CAC
const val GL_DEPTH32F_STENCIL8 = 0x8CAD

const val GL_DEPTH_ATTACHMENT = 0x8D00
const val GL_STENCIL_ATTACHMENT = 0x8D20
const val GL_DEPTH_STENCIL_ATTACHMENT = 0x821A

const val GL_RENDERBUFFER = 0x8D41

const val GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E

inline fun glGenerateMipmap(target: Int) = glWrapper.glGenerateMipmap(target)

inline fun glDeleteVertexArrays(array: Int) = glWrapper.glDeleteVertexArrays(array)
inline fun glBindVertexArray(array: Int) = glWrapper.glBindVertexArray(array)
inline fun glBindBufferBase(target: Int, index: Int, buffer: Int) = glWrapper.glBindBufferBase(target, index, buffer)
inline fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Long, size: Long) =
    glWrapper.glBindBufferRange(target, index, buffer, offset, size)

inline fun glDeleteFramebuffers(framebuffer: Int) = glWrapper.glDeleteFramebuffers(framebuffer)
inline fun glBindFramebuffer(target: Int, framebuffer: Int) = glWrapper.glBindFramebuffer(target, framebuffer)


// GL31
const val GL_UNIFORM_BUFFER = 0x8A11

inline fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int) =
    glWrapper.glDrawArraysInstanced(mode, first, count, primcount)

inline fun glGetUniformBlockIndex(program: Int, uniformBlockName: CharSequence): Int =
    glWrapper.glGetUniformBlockIndex(program, uniformBlockName)

inline fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) =
    glWrapper.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)

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


// GL42
const val GL_ATOMIC_COUNTER_BUFFER = 0x92C0

// GL42
const val GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 0x1
const val GL_ELEMENT_ARRAY_BARRIER_BIT = 0x2
const val GL_UNIFORM_BARRIER_BIT = 0x4
const val GL_TEXTURE_FETCH_BARRIER_BIT = 0x8
const val GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 0x20
const val GL_COMMAND_BARRIER_BIT = 0x40
const val GL_PIXEL_BUFFER_BARRIER_BIT = 0x80
const val GL_TEXTURE_UPDATE_BARRIER_BIT = 0x100
const val GL_BUFFER_UPDATE_BARRIER_BIT = 0x200
const val GL_FRAMEBUFFER_BARRIER_BIT = 0x400
const val GL_TRANSFORM_FEEDBACK_BARRIER_BIT = 0x800
const val GL_ATOMIC_COUNTER_BARRIER_BIT = 0x1000
const val GL_ALL_BARRIER_BITS = -0x1

inline fun glMemoryBarrier(barriers: Int) = glWrapper.glMemoryBarrier(barriers)


// GL43
const val GL_COMPUTE_SHADER = 0x91B9

const val GL_SHADER_STORAGE_BUFFER = 0x90D2

const val GL_UNIFORM = 0x92E1
const val GL_UNIFORM_BLOCK = 0x92E2
const val GL_PROGRAM_INPUT = 0x92E3
const val GL_PROGRAM_OUTPUT = 0x92E4
const val GL_BUFFER_VARIABLE = 0x92E5
const val GL_SHADER_STORAGE_BLOCK = 0x92E6
const val GL_VERTEX_SUBROUTINE = 0x92E8
const val GL_TESS_CONTROL_SUBROUTINE = 0x92E9
const val GL_TESS_EVALUATION_SUBROUTINE = 0x92EA
const val GL_GEOMETRY_SUBROUTINE = 0x92EB
const val GL_FRAGMENT_SUBROUTINE = 0x92EC
const val GL_COMPUTE_SUBROUTINE = 0x92ED
const val GL_VERTEX_SUBROUTINE_UNIFORM = 0x92EE
const val GL_TESS_CONTROL_SUBROUTINE_UNIFORM = 0x92EF
const val GL_TESS_EVALUATION_SUBROUTINE_UNIFORM = 0x92F0
const val GL_GEOMETRY_SUBROUTINE_UNIFORM = 0x92F1
const val GL_FRAGMENT_SUBROUTINE_UNIFORM = 0x92F2
const val GL_COMPUTE_SUBROUTINE_UNIFORM = 0x92F3
const val GL_TRANSFORM_FEEDBACK_VARYING = 0x92F4

const val GL_SHADER_STORAGE_BARRIER_BIT = 0x2000

inline fun glInvalidateBufferSubData(buffer: Int, offset: Long, length: Long) =
    glWrapper.glInvalidateBufferSubData(buffer, offset, length)

inline fun glInvalidateBufferData(buffer: Int) = glWrapper.glInvalidateBufferData(buffer)

inline fun glMultiDrawArraysIndirect(
    mode: Int,
    indirect: Long,
    primcount: Int,
    stride: Int
) = glWrapper.glMultiDrawArraysIndirect(mode, indirect, primcount, stride)

inline fun glMultiDrawElementsIndirect(
    mode: Int,
    type: Int,
    indirect: Long,
    primcount: Int,
    stride: Int
) = glWrapper.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride)

inline fun glShaderStorageBlockBinding(program: Int, storageBlockIndex: Int, storageBlockBinding: Int) =
    glWrapper.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding)

inline fun glGetProgramResourceIndex(program: Int, programInterface: Int, name: CharSequence): Int =
    glWrapper.glGetProgramResourceIndex(program, programInterface, name)


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

inline fun glCreateFramebuffers(): Int = glWrapper.glCreateFramebuffers()

inline fun glCheckNamedFramebufferStatus(
    framebuffer: Int,
    target: Int
): Int = glWrapper.glCheckNamedFramebufferStatus(framebuffer, target)

inline fun glNamedFramebufferRenderbuffer(
    framebuffer: Int,
    attachment: Int,
    renderbuffertarget: Int,
    renderbuffer: Int
) = glWrapper.glNamedFramebufferRenderbuffer(framebuffer, attachment, renderbuffertarget, renderbuffer)

inline fun glNamedFramebufferTexture(
    framebuffer: Int,
    attachment: Int,
    texture: Int,
    level: Int
) = glWrapper.glNamedFramebufferTexture(framebuffer, attachment, texture, level)

inline fun glNamedFramebufferDrawBuffers(framebuffer: Int, bufs: IntArray) =
    glWrapper.glNamedFramebufferDrawBuffers(framebuffer, bufs)

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

inline fun glClearNamedBufferData(buffer: Int, internalformat: Int, format: Int, type: Int, data: ByteBuffer?) =
    glWrapper.glClearNamedBufferData(buffer, internalformat, format, type, data)