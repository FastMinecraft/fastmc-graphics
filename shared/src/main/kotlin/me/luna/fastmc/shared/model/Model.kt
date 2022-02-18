package me.luna.fastmc.shared.model

import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.resource.Resource

abstract class Model(override val resourceName: String, private val textureSizeX: Int, private val textureSizeZ: Int) :
    Resource {
    private var vboID = 0
    var modelSize = 0; private set

    private fun init0() {
        val builder = ModelBuilder(0, textureSizeX, textureSizeZ)
        builder.buildModel()

        vboID = glGenBuffers()
        modelSize = builder.vertexSize

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, builder.build(), GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    protected abstract fun ModelBuilder.buildModel()

    fun attachVBO() {
        glBindBuffer(GL_ARRAY_BUFFER, vboID)

        vertexAttribute.apply()
    }

    override fun destroy() {
        glDeleteBuffers(vboID)
    }

    companion object {
        fun <T : Model> T.init() :T {
            this.init0()
            return this
        }

        private val vertexAttribute = buildAttribute(20) {
            float(0, 3, GLDataType.GL_FLOAT, false)
            float(1, 2, GLDataType.GL_UNSIGNED_SHORT, true)
            float(2, 3, GLDataType.GL_BYTE, false)
            int(3, 3, GLDataType.GL_UNSIGNED_BYTE)
        }
    }
}