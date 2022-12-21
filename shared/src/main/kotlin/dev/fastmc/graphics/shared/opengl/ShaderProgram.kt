package dev.fastmc.graphics.shared.opengl

import dev.fastmc.common.EnumMap
import dev.fastmc.graphics.FastMcMod
import it.unimi.dsi.fastutil.objects.Object2ByteMap
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap

open class ShaderProgram(
    vararg shaders: ShaderSource,
) : IGLObject, IGLBinding {
    final override val id: Int

    private var currentBindingIndex = 0
    private val bufferBindings = EnumMap<BindingTarget, Object2ByteMap<String>>()

    init {
        val programID = glCreateProgram()
        val shaderIDs = IntArray(shaders.size) { i ->
            createShader(shaders[i]).also {
                glAttachShader(programID, it)
            }
        }

        glLinkProgram(programID)
        val linked = glGetProgrami(programID, GL_LINK_STATUS)
        if (linked == 0) {
            FastMcMod.logger.error(glGetProgramInfoLog(programID, 1024))
            glDeleteProgram(programID)
            throw IllegalStateException("Shader program failed to link")
        }
        this.id = programID

        shaderIDs.forEach {
            glDetachShader(programID, it)
            glDeleteShader(it)
        }
    }

    private fun createShader(source: ShaderSource): Int {
        val id = glCreateShader(source.glTypeEnum)

        glShaderSource(id, source.codeSrc)
        glCompileShader(id)

        val compiled = glGetShaderi(id, GL_COMPILE_STATUS)
        if (compiled == 0) {
            System.err.print(glGetShaderInfoLog(id, 1024))
            System.err.print("Shader source:\n")
            source.codeSrc.lines().forEachIndexed { i, it ->
                System.err.print(i + 1)
                System.err.print('\t')
                System.err.print(it)
                System.err.print('\n')
            }
            System.err.flush()
            glDeleteShader(id)
            throw IllegalStateException("Failed to compile shader: $source")
        }

        return id
    }

    fun attachBuffer(target: Int, buffer: BufferObject, blockName: String) {
        attachBuffer(target, buffer, blockName, -1, -1)
    }

    fun attachBuffer(target: Int, buffer: BufferObject, blockName: String, offset: Int, size: Int) {
        val bindingTarget = BindingTarget[target]
        val map = bufferBindings.getOrPut(bindingTarget) {
            Object2ByteOpenHashMap<String>().apply {
                defaultReturnValue(-1)
            }
        }

        var bindingIndex = map.getByte(blockName).toInt()
        if (bindingIndex == -1) {
            bindingIndex = currentBindingIndex++
            bindingTarget.addBinding(id, blockName, bindingIndex)
            map.put(blockName, bindingIndex.toByte())
        }

        if (offset == -1 || size == -1) {
            glBindBufferBase(target, bindingIndex, buffer.id)
        } else {
            glBindBufferRange(target, bindingIndex, buffer.id, offset.toLong(), size.toLong())
        }
    }

    override fun bind() {
        glUseProgram(id)
    }

    override fun unbind() {
        glUseProgram(0)
    }

    override fun destroy() {
        glDeleteProgram(id)
    }

    data class BufferBinding(val target: Int, val buffer: BufferObject, val offset: Int, val size: Int)

    private enum class BindingTarget {
        UNIFORM_BUFFER {
            override fun addBinding(id: Int, blockName: String, bindingIndex: Int) {
                val index = glGetProgramResourceIndex(id, GL_UNIFORM_BLOCK, blockName)
                glUniformBlockBinding(id, index, bindingIndex)
            }
        },
        SHADER_STORAGE_BUFFER {
            override fun addBinding(id: Int, blockName: String, bindingIndex: Int) {
                val index = glGetProgramResourceIndex(id, GL_SHADER_STORAGE_BLOCK, blockName)
                glShaderStorageBlockBinding(id, index, bindingIndex)
            }
        };

        abstract fun addBinding(id: Int, blockName: String, bindingIndex: Int)

        companion object {
            operator fun get(target: Int): BindingTarget {
                return when (target) {
                    GL_UNIFORM_BUFFER -> UNIFORM_BUFFER
                    GL_SHADER_STORAGE_BUFFER -> SHADER_STORAGE_BUFFER
                    else -> throw IllegalArgumentException("Unsupported buffer binding target: $target")
                }
            }
        }
    }
}