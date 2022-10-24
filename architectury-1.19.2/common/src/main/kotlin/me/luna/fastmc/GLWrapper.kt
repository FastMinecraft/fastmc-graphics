package me.luna.fastmc

import com.mojang.blaze3d.platform.GlStateManager
import me.luna.fastmc.mixin.accessor.AccessorGlStateManager
import me.luna.fastmc.shared.opengl.IGLWrapper
import me.luna.fastmc.shared.util.allocateInt
import org.lwjgl.opengl.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

class GLWrapper : IGLWrapper {
    override val lightMapUnit: Int
        get() = 2


    // GL11
    override fun glClear(mask: Int) = GL11C.glClear(mask)
    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) =
        GL11C.glClearColor(red, green, blue, alpha)

    override fun glClearDepth(depth: Double) = GL11C.glClearDepth(depth)

    override fun glDeleteTextures(texture: Int) = GL11C.glDeleteTextures(texture)
    override fun glBindTexture(texture: Int) = GlStateManager._bindTexture(texture)
    override fun glDrawArrays(mode: Int, first: Int, count: Int) = GL11C.glDrawArrays(mode, first, count)
    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long) =
        GL11C.glDrawElements(mode, count, type, indices)


    // GL14
    override fun glMultiDrawArrays(mode: Int, first: IntBuffer, count: IntBuffer) =
        GL14C.glMultiDrawArrays(mode, first, count)


    // GL15
    override fun glDeleteBuffers(buffer: Int) = GL15C.glDeleteBuffers(buffer)
    override fun glBindBuffer(target: Int, buffer: Int) = GL15C.glBindBuffer(target, buffer)


    // GL20
    override fun glCreateShader(type: Int): Int = GL20C.glCreateShader(type)
    override fun glDeleteShader(shader: Int) = GL20C.glDeleteShader(shader)
    override fun glShaderSource(shader: Int, string: CharSequence) = GL20C.glShaderSource(shader, string)
    override fun glCompileShader(shader: Int) = GL20C.glCompileShader(shader)
    override fun glGetShaderi(shader: Int, pname: Int): Int = GL20C.glGetShaderi(shader, pname)
    override fun glGetShaderInfoLog(shader: Int, maxLength: Int): String = GL20C.glGetShaderInfoLog(shader, maxLength)
    override fun glAttachShader(program: Int, shader: Int) = GL20C.glAttachShader(program, shader)
    override fun glDetachShader(program: Int, shader: Int) = GL20C.glDetachShader(program, shader)
    override fun glCreateProgram(): Int = GL20C.glCreateProgram()
    override fun glDeleteProgram(program: Int) = GL20C.glDeleteProgram(program)
    override fun glLinkProgram(program: Int) = GL20C.glLinkProgram(program)
    override fun glGetProgrami(program: Int, pname: Int): Int = GL20C.glGetProgrami(program, pname)
    override fun glGetProgramInfoLog(program: Int, maxLength: Int): String =
        GL20C.glGetProgramInfoLog(program, maxLength)

    override fun glUseProgram(program: Int) = GL20C.glUseProgram(program)
    override fun glGetUniformLocation(program: Int, name: CharSequence): Int = GL20C.glGetUniformLocation(program, name)


    // GL30
    override fun glDeleteVertexArrays(array: Int) = GL30C.glDeleteVertexArrays(array)
    override fun glBindVertexArray(array: Int) = GL30C.glBindVertexArray(array)
    override fun glGenerateMipmap(target: Int) = GL30C.glGenerateMipmap(target)
    override fun glBindBufferBase(target: Int, index: Int, buffer: Int) = GL30C.glBindBufferBase(target, index, buffer)
    override fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Long, size: Long) =
        GL30C.glBindBufferRange(target, index, buffer, offset, size)

    override fun glDeleteFramebuffers(framebuffer: Int) = GL30C.glDeleteFramebuffers(framebuffer)
    override fun glBindFramebuffer(target: Int, framebuffer: Int) = GL30C.glBindFramebuffer(target, framebuffer)


    // GL31
    override fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int) =
        GL31C.glDrawArraysInstanced(mode, first, count, primcount)

    override fun glGetUniformBlockIndex(program: Int, uniformBlockName: CharSequence): Int =
        GL31C.glGetUniformBlockIndex(program, uniformBlockName)

    override fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) =
        GL31C.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)


    // GL32
    private val lengthBuffer = allocateInt(1).apply {
        put(1)
        flip()
    }
    private val valueBuffer = allocateInt(1)

    override fun glFenceSync(condition: Int, flags: Int): Long = GL32C.glFenceSync(condition, flags)
    override fun glDeleteSync(sync: Long) = GL32C.glDeleteSync(sync)

    override fun glGetSynciv(sync: Long, pname: Int): Int {
        GL32C.glGetSynciv(sync, pname, lengthBuffer, valueBuffer)
        return valueBuffer.get(0)
    }


    // GL41
    override fun glProgramUniform1i(program: Int, location: Int, v0: Int) =
        GL41C.glProgramUniform1i(program, location, v0)

    override fun glProgramUniform1f(program: Int, location: Int, v0: Float) =
        GL41C.glProgramUniform1f(program, location, v0)

    override fun glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float) =
        GL41C.glProgramUniform2f(program, location, v0, v1)

    override fun glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float) =
        GL41C.glProgramUniform3f(program, location, v0, v1, v2)

    override fun glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float) =
        GL41C.glProgramUniform4f(program, location, v0, v1, v2, v3)

    override fun glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, matrices: FloatBuffer) =
        GL41C.glProgramUniformMatrix4fv(program, location, transpose, matrices)


    // GL42
    override fun glMemoryBarrier(barriers: Int) = GL42C.glMemoryBarrier(barriers)


    // GL43
    override fun glInvalidateBufferSubData(buffer: Int, offset: Long, length: Long) =
        GL43C.glInvalidateBufferSubData(buffer, offset, length)

    override fun glInvalidateBufferData(buffer: Int) = GL43C.glInvalidateBufferData(buffer)
    override fun glMultiDrawArraysIndirect(
        mode: Int,
        indirect: Long,
        primcount: Int,
        stride: Int
    ) = GL43C.glMultiDrawArraysIndirect(mode, indirect, primcount, stride)

    override fun glMultiDrawElementsIndirect(
        mode: Int,
        type: Int,
        indirect: Long,
        primcount: Int,
        stride: Int
    ) = GL43C.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride)

    override fun glShaderStorageBlockBinding(program: Int, storageBlockIndex: Int, storageBlockBinding: Int) =
        GL43C.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding)

    override fun glGetProgramResourceIndex(program: Int, programInterface: Int, name: CharSequence): Int =
        GL43C.glGetProgramResourceIndex(program, programInterface, name)


    // GL45
    override fun glCreateVertexArrays(): Int = GL45C.glCreateVertexArrays()
    override fun glVertexArrayVertexBuffer(vaobj: Int, bindingindex: Int, buffer: Int, offset: Long, stride: Int) =
        GL45C.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride)

    override fun glVertexArrayElementBuffer(vaobj: Int, buffer: Int) = GL45C.glVertexArrayElementBuffer(vaobj, buffer)
    override fun glEnableVertexArrayAttrib(vaobj: Int, index: Int) = GL45C.glEnableVertexArrayAttrib(vaobj, index)
    override fun glVertexArrayAttribFormat(
        vaobj: Int,
        attribindex: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        relativeoffset: Int
    ) = GL45C.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset)

    override fun glVertexArrayAttribIFormat(vaobj: Int, attribindex: Int, size: Int, type: Int, relativeoffset: Int) =
        GL45C.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset)

    override fun glVertexArrayBindingDivisor(vaobj: Int, bindingindex: Int, divisor: Int) =
        GL45C.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor)

    override fun glVertexArrayAttribBinding(vaobj: Int, attribindex: Int, bindingindex: Int) =
        GL45C.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex)

    override fun glCreateBuffers(): Int = GL45C.glCreateBuffers()

    override fun glNamedBufferStorage(buffer: Int, data: ByteBuffer, flags: Int) =
        GL45C.glNamedBufferStorage(buffer, data, flags)

    override fun glNamedBufferStorage(buffer: Int, size: Long, flags: Int) =
        GL45C.glNamedBufferStorage(buffer, size, flags)

    override fun glNamedBufferData(buffer: Int, size: Long, usage: Int) =
        GL45C.glNamedBufferData(buffer, size, usage)

    override fun glNamedBufferData(buffer: Int, data: ByteBuffer, usage: Int) =
        GL45C.glNamedBufferData(buffer, data, usage)

    override fun glNamedBufferSubData(buffer: Int, offset: Long, data: ByteBuffer) =
        GL45C.glNamedBufferSubData(buffer, offset, data)

    override fun glCopyNamedBufferSubData(
        readBuffer: Int,
        writeBuffer: Int,
        readOffset: Long,
        writeOffset: Long,
        size: Long
    ) = GL45C.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size)

    override fun glCreateTextures(target: Int): Int = GL45C.glCreateTextures(target)

    override fun glBindTextureUnit(unit: Int, texture: Int) {
        val textures = AccessorGlStateManager.getTextures()
        if (unit < textures.size) {
            textures[unit].boundTexture = texture
        }
        GL45C.glBindTextureUnit(unit, texture)
    }

    override fun glTextureStorage2D(texture: Int, levels: Int, internalformat: Int, width: Int, height: Int) =
        GL45C.glTextureStorage2D(texture, levels, internalformat, width, height)

    override fun glTextureSubImage2D(
        texture: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        pixels: ByteBuffer
    ) = GL45C.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, pixels)

    override fun glTextureParameteri(texture: Int, pname: Int, param: Int) =
        GL45C.glTextureParameteri(texture, pname, param)

    override fun glTextureParameterf(texture: Int, pname: Int, param: Float) =
        GL45C.glTextureParameterf(texture, pname, param)

    override fun glCreateFramebuffers(): Int = GL45C.glCreateFramebuffers()

    override fun glCheckNamedFramebufferStatus(
        framebuffer: Int,
        target: Int
    ): Int = GL45C.glCheckNamedFramebufferStatus(framebuffer, target)

    override fun glNamedFramebufferRenderbuffer(
        framebuffer: Int,
        attachment: Int,
        renderbuffertarget: Int,
        renderbuffer: Int
    ) = GL45C.glNamedFramebufferRenderbuffer(framebuffer, attachment, renderbuffer, renderbuffer)

    override fun glNamedFramebufferTexture(
        framebuffer: Int,
        attachment: Int,
        texture: Int,
        level: Int
    ) = GL45C.glNamedFramebufferTexture(framebuffer, attachment, texture, level)

    override fun glNamedFramebufferDrawBuffers(framebuffer: Int, bufs: IntArray) =
        GL45C.glNamedFramebufferDrawBuffers(framebuffer, bufs)

    override fun glMapNamedBuffer(
        buffer: Int,
        access: Int,
        old_buffer: ByteBuffer?
    ): ByteBuffer? = GL45C.glMapNamedBuffer(buffer, access, old_buffer)

    override fun glMapNamedBufferRange(buffer: Int, offset: Long, length: Long, access: Int): ByteBuffer? =
        GL45C.glMapNamedBufferRange(buffer, offset, length, access)

    override fun glMapNamedBufferRange(
        buffer: Int,
        offset: Long,
        length: Long,
        access: Int,
        old_buffer: ByteBuffer?
    ): ByteBuffer? = GL45C.glMapNamedBufferRange(buffer, offset, length, access, old_buffer)

    override fun glUnmapNamedBuffer(buffer: Int): Boolean = GL45C.glUnmapNamedBuffer(buffer)

    override fun glFlushMappedNamedBufferRange(buffer: Int, offset: Long, length: Long) =
        GL45C.glFlushMappedNamedBufferRange(buffer, offset, length)

    override fun glClearNamedBufferData(buffer: Int, internalformat: Int, format: Int, type: Int, data: ByteBuffer?) {
        GL45C.glClearNamedBufferData(buffer, internalformat, format, type, data)
    }
}