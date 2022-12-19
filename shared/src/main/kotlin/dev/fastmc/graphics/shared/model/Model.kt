package dev.fastmc.graphics.shared.model

import dev.fastmc.graphics.shared.opengl.BufferObject
import dev.fastmc.graphics.shared.opengl.GLDataType
import dev.fastmc.graphics.shared.opengl.VertexArrayObject
import dev.fastmc.graphics.shared.opengl.impl.buildAttribute
import dev.fastmc.graphics.shared.resource.Resource

abstract class Model(override val resourceName: String, private val textureSizeX: Int, private val textureSizeZ: Int) :
    Resource {
    private lateinit var vbo: BufferObject
    var modelSize = 0; private set

    private fun init0() {
        val builder = ModelBuilder(0, textureSizeX, textureSizeZ)
        builder.buildModel()

        vbo = BufferObject.Immutable()
        modelSize = builder.vertexSize

        vbo.allocate(builder.build(), 0)
    }

    protected abstract fun ModelBuilder.buildModel()

    fun attachVbo(vao: VertexArrayObject) {
        vao.attachVbo(vbo, VERTEX_ATTRIBUTE)
    }

    override fun destroy() {
        vbo.destroy()
    }

    companion object {
        fun <T : Model> T.init(): T {
            this.init0()
            return this
        }

        @JvmField
        val VERTEX_ATTRIBUTE = buildAttribute(20) {
            float(0, 3, GLDataType.GL_FLOAT, false)
            float(1, 2, GLDataType.GL_UNSIGNED_SHORT, true)
            float(2, 3, GLDataType.GL_BYTE, false)
            int(3, 1, GLDataType.GL_UNSIGNED_BYTE)
        }
    }
}