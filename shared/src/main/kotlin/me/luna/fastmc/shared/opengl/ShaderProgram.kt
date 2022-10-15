package me.luna.fastmc.shared.opengl

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.util.collection.FastObjectArrayList

open class ShaderProgram(
    vertex: ShaderSource.Vertex,
    fragment: ShaderSource.Fragment
) : IGLObject, IGLBinding {
    final override val id: Int

    private val bufferBindings = FastObjectArrayList<BufferBinding>()

    init {
        val vertexShaderID = createShader(vertex, GL_VERTEX_SHADER)
        val fragShaderID = createShader(fragment, GL_FRAGMENT_SHADER)
        val id = glCreateProgram()

        glAttachShader(id, vertexShaderID)
        glAttachShader(id, fragShaderID)

        glLinkProgram(id)
        val linked = glGetProgrami(id, GL_LINK_STATUS)
        if (linked == 0) {
            FastMcMod.logger.error(glGetProgramInfoLog(id, 1024))
            glDeleteProgram(id)
            throw IllegalStateException("Shader program failed to link")
        }
        this.id = id

        glDetachShader(id, vertexShaderID)
        glDetachShader(id, fragShaderID)
        glDeleteShader(vertexShaderID)
        glDeleteShader(fragShaderID)
    }

    private fun createShader(source: ShaderSource, shaderType: Int): Int {
        val id = glCreateShader(shaderType)

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
            glDeleteShader(id)
            throw IllegalStateException("Failed to compile shader: $source")
        }

        return id
    }

    fun attachBufferBinding(target: Int, buffer: BufferObject, blockName: String) {
        attachBufferBinding(target, buffer, blockName, -1, -1)
    }


    fun attachBufferBinding(target: Int, buffer: BufferObject, blockName: String, offset: Int, size: Int) {
        when (target) {
            GL_UNIFORM_BUFFER -> {
                val index = glGetUniformBlockIndex(id, blockName)
                glUniformBlockBinding(id, index, bufferBindings.size)
                bufferBindings.add(BufferBinding(target, buffer, offset, size))
            }
            GL_SHADER_STORAGE_BUFFER -> {
                val index = glGetProgramResourceIndex(id, GL_SHADER_STORAGE_BLOCK, blockName)
                glShaderStorageBlockBinding(id, index, bufferBindings.size)
                bufferBindings.add(BufferBinding(target, buffer, offset, size))
            }
            else -> throw IllegalArgumentException("Unsupported buffer binding target: $target")
        }
    }

    override fun bind() {
        glUseProgram(id)
        for (i in bufferBindings.indices) {
            val binding = bufferBindings[i]
            if (binding.offset == -1 || binding.size == -1) {
                glBindBufferBase(binding.target, i, binding.buffer.id)
            } else {
                glBindBufferRange(binding.target, i, binding.buffer.id, binding.offset.toLong(), binding.size.toLong())
            }
        }
    }

    override fun unbind() {
        glUseProgram(0)
    }

    override fun destroy() {
        glDeleteProgram(id)
    }

    data class BufferBinding(val target: Int, val buffer: BufferObject, val offset: Int, val size: Int)
}