package me.luna.fastmc.shared.opengl

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.opengl.impl.UniformBufferObject
import me.luna.fastmc.shared.resource.Resource
import me.luna.fastmc.shared.util.collection.FastObjectArrayList

open class ShaderProgram(
    final override val resourceName: String,
    vertex: ShaderSource.Vertex,
    fragment: ShaderSource.Fragment
) :
    Resource,
    IGLObject {
    final override val id: Int

    private val uniformBuffers = FastObjectArrayList<UniformBufferObject>()

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
            FastMcMod.logger.error(glGetShaderInfoLog(id, 1024))
            glDeleteShader(id)
            throw IllegalStateException("Failed to compile shader: $source")
        }

        return id
    }

    fun attachUBO(ubo: UniformBufferObject) {
        val index = glGetUniformBlockIndex(id, ubo.blockName)
        glUniformBlockBinding(id, index, uniformBuffers.size)
        uniformBuffers.add(ubo)
    }

    final override fun bind() {
        glUseProgram(id)
        for (i in uniformBuffers.indices) {
            glBindBufferBase(GL_UNIFORM_BUFFER, i, uniformBuffers[i].id)
        }
    }

    final override fun unbind() {
        glUseProgram(0)
    }

    fun unbindForce() {
        glUseProgramForce(0)
    }

    override fun destroy() {
        glDeleteProgram(id)
    }
}