package me.luna.fastmc.shared.opengl

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.resource.Resource

open class Shader(final override val resourceName: String, vertShaderPath: String, fragShaderPath: String) : Resource {
    val id: Int

    init {
        val vertexShaderID = createShader(vertShaderPath, GL_VERTEX_SHADER)
        val fragShaderID = createShader(fragShaderPath, GL_FRAGMENT_SHADER)
        val id = glCreateProgram()

        glAttachShader(id, vertexShaderID)
        glAttachShader(id, fragShaderID)

        glLinkProgram(id)
        val linked = glGetProgrami(id, GL_LINK_STATUS)
        if (linked == 0) {
            FastMcMod.logger.error(glGetProgramInfoLog(id, 1024))
            glDeleteProgram(id)
            throw IllegalStateException("Shader failed to link")
        }
        this.id = id

        glDetachShader(id, vertexShaderID)
        glDetachShader(id, fragShaderID)
        glDeleteShader(vertexShaderID)
        glDeleteShader(fragShaderID)
    }

    private fun createShader(path: String, shaderType: Int): Int {
        val stringBuilder = StringBuilder()
        javaClass.getResourceAsStream(path)!!.bufferedReader().forEachLine {
            if (it.startsWith("#import")) {
                val importContent = javaClass.getResourceAsStream(it.substring(8))!!.readBytes().decodeToString()
                stringBuilder.appendLine(importContent)
            } else {
                stringBuilder.appendLine(it)
            }
        }
        val id = glCreateShader(shaderType)

        glShaderSource(id, stringBuilder)
        glCompileShader(id)

        val compiled = glGetShaderi(id, GL_COMPILE_STATUS)
        if (compiled == 0) {
            FastMcMod.logger.error(glGetShaderInfoLog(id, 1024))
            glDeleteShader(id)
            throw IllegalStateException("Failed to compile shader: $path")
        }

        return id
    }

    fun bind() {
        glUseProgram(id)
    }

    fun unbind() {
        glUseProgram(0)
    }

    fun unbindForce() {
        glUseProgramForce(0)
    }

    override fun destroy() {
        glDeleteProgram(id)
    }
}