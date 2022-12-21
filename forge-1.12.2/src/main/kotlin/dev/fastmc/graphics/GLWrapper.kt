package dev.fastmc.graphics

import dev.fastmc.common.CachedBuffer
import dev.fastmc.common.allocateInt
import dev.fastmc.common.wrapDirectByteBuffer
import dev.fastmc.graphics.mixin.accessor.AccessorGlStateManager
import dev.fastmc.graphics.shared.opengl.IGLWrapper
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.PointerWrapperAbstract
import org.lwjgl.opengl.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class GLWrapper : IGLWrapper {
    override val lightMapUnit: Int
        get() = 1


    // GL11
    override fun glClear(mask: Int) = GL11.glClear(mask)
    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) =
        GlStateManager.clearColor(red, green, blue, alpha)

    override fun glClearDepth(depth: Double) = GlStateManager.clearDepth(depth)

    override fun glDeleteTextures(texture: Int) = GL11.glDeleteTextures(texture)
    override fun glBindTexture(texture: Int) = GlStateManager.bindTexture(texture)
    override fun glDrawArrays(mode: Int, first: Int, count: Int) = GL11.glDrawArrays(mode, first, count)

    // LWJGL checks for GL_ELEMENT_ARRAY_BUFFER binding when invoking glDrawElements() with indices buffer offset.
    // However, it breaks when rendering with a bound VAO that has an element buffer attached (which is 100% valid).
    private val glDrawElementsFunctionPointer =
        GLContext::class.java.getDeclaredMethod("getFunctionAddress", String::class.java).run {
            isAccessible = true
            val pointer = invoke(null, "glDrawElements")
            isAccessible = false
            pointer as Long
        }

    private val nglDrawElementsBOMethod = GL11::class.java.getDeclaredMethod(
        "nglDrawElementsBO",
        Int::class.java,
        Int::class.java,
        Int::class.java,
        Long::class.java,
        Long::class.java
    ).apply {
        isAccessible = true
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long) {
        nglDrawElementsBOMethod.invoke(null, mode, count, type, indices, glDrawElementsFunctionPointer)
    }

    // GL14
    override fun glMultiDrawArrays(mode: Int, first: IntBuffer, count: IntBuffer) =
        GL14.glMultiDrawArrays(mode, first, count)


    // GL15
    override fun glDeleteBuffers(buffer: Int) = GL15.glDeleteBuffers(buffer)
    override fun glBindBuffer(target: Int, buffer: Int) = GL15.glBindBuffer(target, buffer)


    // GL20
    override fun glCreateShader(type: Int): Int = GL20.glCreateShader(type)
    override fun glDeleteShader(shader: Int) = GL20.glDeleteShader(shader)
    override fun glShaderSource(shader: Int, string: CharSequence) = GL20.glShaderSource(shader, string)
    override fun glCompileShader(shader: Int) = GL20.glCompileShader(shader)
    override fun glGetShaderi(shader: Int, pname: Int): Int = GL20.glGetShaderi(shader, pname)
    override fun glGetShaderInfoLog(shader: Int, maxLength: Int): String = GL20.glGetShaderInfoLog(shader, maxLength)
    override fun glAttachShader(program: Int, shader: Int) = GL20.glAttachShader(program, shader)
    override fun glDetachShader(program: Int, shader: Int) = GL20.glDetachShader(program, shader)
    override fun glCreateProgram(): Int = GL20.glCreateProgram()
    override fun glDeleteProgram(program: Int) = GL20.glDeleteProgram(program)
    override fun glLinkProgram(program: Int) = GL20.glLinkProgram(program)
    override fun glGetProgrami(program: Int, pname: Int): Int = GL20.glGetProgrami(program, pname)
    override fun glGetProgramInfoLog(program: Int, maxLength: Int): String =
        GL20.glGetProgramInfoLog(program, maxLength)

    override fun glUseProgram(program: Int) = GL20.glUseProgram(program)
    override fun glGetUniformLocation(program: Int, name: CharSequence): Int = GL20.glGetUniformLocation(program, name)


    // GL30
    override fun glDeleteVertexArrays(array: Int) = GL30.glDeleteVertexArrays(array)
    override fun glBindVertexArray(array: Int) = GL30.glBindVertexArray(array)
    override fun glGenerateMipmap(target: Int) = GL30.glGenerateMipmap(target)
    override fun glBindBufferBase(target: Int, index: Int, buffer: Int) = GL30.glBindBufferBase(target, index, buffer)
    override fun glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Long, size: Long) =
        GL30.glBindBufferRange(target, index, buffer, offset, size)


    override fun glDeleteFramebuffers(framebuffer: Int) = GL30.glDeleteFramebuffers(framebuffer)
    override fun glBindFramebuffer(target: Int, framebuffer: Int) = GL30.glBindFramebuffer(target, framebuffer)

    // GL31
    override fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int) =
        GL31.glDrawArraysInstanced(mode, first, count, primcount)

    override fun glGetUniformBlockIndex(program: Int, uniformBlockName: CharSequence): Int =
        GL31.glGetUniformBlockIndex(program, uniformBlockName)

    override fun glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int) =
        GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)


    // GL32
    private val glSyncInstance: GLSync
    private val pointerField = PointerWrapperAbstract::class.java.getDeclaredField("pointer").apply {
        isAccessible = true
    }
    private val lengthBuffer = allocateInt(1).apply {
        put(1)
        flip()
    }
    private val valueBuffer = allocateInt(1)

    init {
        val constructor = GLSync::class.java.declaredConstructors.first()
        constructor.isAccessible = true
        glSyncInstance = constructor.newInstance(0) as GLSync
        constructor.isAccessible = false
    }

    override fun glFenceSync(condition: Int, flags: Int): Long = GL32.glFenceSync(condition, flags).pointer
    override fun glDeleteSync(sync: Long) {
        pointerField.set(glSyncInstance, sync)
        GL32.glDeleteSync(glSyncInstance)
    }

    override fun glGetSynciv(sync: Long, pname: Int): Int {
        pointerField.set(glSyncInstance, sync)
        GL32.glGetSync(glSyncInstance, pname, lengthBuffer, valueBuffer)
        return valueBuffer.get(0)
    }


    // GL41
    override fun glProgramUniform1i(program: Int, location: Int, v0: Int) =
        GL41.glProgramUniform1i(program, location, v0)

    override fun glProgramUniform1f(program: Int, location: Int, v0: Float) =
        GL41.glProgramUniform1f(program, location, v0)

    override fun glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float) =
        GL41.glProgramUniform2f(program, location, v0, v1)

    override fun glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float) =
        GL41.glProgramUniform3f(program, location, v0, v1, v2)

    override fun glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float) =
        GL41.glProgramUniform4f(program, location, v0, v1, v2, v3)

    override fun glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, matrices: FloatBuffer) =
        GL41.glProgramUniformMatrix4(program, location, transpose, matrices)


    // GL42
    override fun glMemoryBarrier(barriers: Int) = GL42.glMemoryBarrier(barriers)


    // GL43
    override fun glInvalidateBufferSubData(buffer: Int, offset: Long, length: Long) =
        GL43.glInvalidateBufferSubData(buffer, offset, length)

    override fun glInvalidateBufferData(buffer: Int) = GL43.glInvalidateBufferData(buffer)
    override fun glMultiDrawArraysIndirect(
        mode: Int,
        indirect: Long,
        primcount: Int,
        stride: Int
    ) = GL43.glMultiDrawArraysIndirect(mode, indirect, primcount, stride)

    override fun glMultiDrawElementsIndirect(
        mode: Int,
        type: Int,
        indirect: Long,
        primcount: Int,
        stride: Int
    ) = GL43.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride)

    override fun glShaderStorageBlockBinding(program: Int, storageBlockIndex: Int, storageBlockBinding: Int) =
        GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding)

    override fun glGetProgramResourceIndex(program: Int, programInterface: Int, name: CharSequence): Int =
        GL43.glGetProgramResourceIndex(program, programInterface, name)


    // GL45
    override fun glCreateVertexArrays(): Int = GL45.glCreateVertexArrays()
    override fun glVertexArrayVertexBuffer(vaobj: Int, bindingindex: Int, buffer: Int, offset: Long, stride: Int) =
        GL45.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride)

    override fun glVertexArrayElementBuffer(vaobj: Int, buffer: Int) = GL45.glVertexArrayElementBuffer(vaobj, buffer)
    override fun glEnableVertexArrayAttrib(vaobj: Int, index: Int) = GL45.glEnableVertexArrayAttrib(vaobj, index)
    override fun glVertexArrayAttribFormat(
        vaobj: Int,
        attribindex: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        relativeoffset: Int
    ) = GL45.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset)

    override fun glVertexArrayAttribIFormat(vaobj: Int, attribindex: Int, size: Int, type: Int, relativeoffset: Int) =
        GL45.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset)

    override fun glVertexArrayBindingDivisor(vaobj: Int, bindingindex: Int, divisor: Int) =
        GL45.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor)

    override fun glVertexArrayAttribBinding(vaobj: Int, attribindex: Int, bindingindex: Int) =
        GL45.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex)

    override fun glCreateBuffers(): Int = GL45.glCreateBuffers()

    override fun glNamedBufferStorage(buffer: Int, data: ByteBuffer, flags: Int) =
        GL45.glNamedBufferStorage(buffer, data, flags)

    override fun glNamedBufferStorage(buffer: Int, size: Long, flags: Int) =
        GL45.glNamedBufferStorage(buffer, size, flags)

    override fun glNamedBufferData(buffer: Int, size: Long, usage: Int) =
        GL45.glNamedBufferData(buffer, size, usage)

    override fun glNamedBufferData(buffer: Int, data: ByteBuffer, usage: Int) =
        GL45.glNamedBufferData(buffer, data, usage)

    override fun glNamedBufferSubData(buffer: Int, offset: Long, data: ByteBuffer) =
        GL45.glNamedBufferSubData(buffer, offset, data)

    override fun glCopyNamedBufferSubData(
        readBuffer: Int,
        writeBuffer: Int,
        readOffset: Long,
        writeOffset: Long,
        size: Long
    ) = GL45.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size)

    override fun glCreateTextures(target: Int): Int = GL45.glCreateTextures(target)

    override fun glBindTextureUnit(unit: Int, texture: Int) {
        val textures = AccessorGlStateManager.getTextureState()
        if (unit < textures.size) {
            textures[unit].textureName = texture
        }
        GL45.glBindTextureUnit(unit, texture)
    }

    override fun glTextureStorage2D(texture: Int, levels: Int, internalformat: Int, width: Int, height: Int) =
        GL45.glTextureStorage2D(texture, levels, internalformat, width, height)

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
    ) = GL45.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, pixels)

    override fun glTextureParameteri(texture: Int, pname: Int, param: Int) =
        GL45.glTextureParameteri(texture, pname, param)

    override fun glTextureParameterf(texture: Int, pname: Int, param: Float) =
        GL45.glTextureParameterf(texture, pname, param)

    override fun glCreateFramebuffers(): Int = GL45.glCreateFramebuffers()

    override fun glCheckNamedFramebufferStatus(
        framebuffer: Int,
        target: Int
    ): Int = GL45.glCheckNamedFramebufferStatus(framebuffer, target)

    override fun glNamedFramebufferRenderbuffer(
        framebuffer: Int,
        attachment: Int,
        renderbuffertarget: Int,
        renderbuffer: Int
    ) = GL45.glNamedFramebufferRenderbuffer(framebuffer, attachment, renderbuffer, renderbuffer)

    private val glNamedFramebufferDrawBuffersBuffer = CachedBuffer(4)

    override fun glNamedFramebufferDrawBuffers(framebuffer: Int, bufs: IntArray) {
        val buffer = glNamedFramebufferDrawBuffersBuffer.getWithCapacityInt(bufs.size, bufs.size)
        buffer.put(bufs)
        buffer.flip()
        GL45.glNamedFramebufferDrawBuffers(framebuffer, buffer)
    }

    override fun glNamedFramebufferTexture(
        framebuffer: Int,
        attachment: Int,
        texture: Int,
        level: Int
    ) = GL45.glNamedFramebufferTexture(framebuffer, attachment, texture, level)

    override fun glMapNamedBuffer(
        buffer: Int,
        access: Int,
        old_buffer: ByteBuffer?
    ): ByteBuffer? = GL45.glMapNamedBuffer(buffer, access, old_buffer)

    override fun glMapNamedBufferRange(buffer: Int, offset: Long, length: Long, access: Int): ByteBuffer? =
        GL45.glMapNamedBufferRange(buffer, offset, length, access, null)

    override fun glMapNamedBufferRange(
        buffer: Int,
        offset: Long,
        length: Long,
        access: Int,
        old_buffer: ByteBuffer?
    ): ByteBuffer? = GL45.glMapNamedBufferRange(buffer, offset, length, access, old_buffer)

    override fun glUnmapNamedBuffer(buffer: Int): Boolean = GL45.glUnmapNamedBuffer(buffer)

    override fun glFlushMappedNamedBufferRange(buffer: Int, offset: Long, length: Long) =
        GL45.glFlushMappedNamedBufferRange(buffer, offset, length)

    private val dummyByteBuffer = wrapDirectByteBuffer(0L, 1)

    override fun glClearNamedBufferData(buffer: Int, internalformat: Int, format: Int, type: Int, data: ByteBuffer?) =
        GL45.glClearNamedBufferData(buffer, internalformat, format, type, data ?: dummyByteBuffer)

    override fun glClearNamedBufferSubData(
        buffer: Int,
        internalformat: Int,
        offset: Long,
        size: Long,
        format: Int,
        type: Int,
        data: ByteBuffer?
    ) = GL45.glClearNamedBufferSubData(buffer, internalformat, offset, size, format, type, data)
}