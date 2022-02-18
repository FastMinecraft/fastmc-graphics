package me.luna.fastmc.shared.renderbuilder

import me.luna.fastmc.shared.opengl.VertexAttribute
import java.nio.ByteBuffer

interface IBuilder<T : IInfo<*>> {
    val vertexSize: Int
    val size: Int
    val buffer: ByteBuffer

    fun VertexAttribute.Builder.setupAttribute()

    fun add(info: T)
}