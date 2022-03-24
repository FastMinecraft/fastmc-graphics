package me.luna.fastmc.terrain

import me.luna.fastmc.shared.opengl.VertexBufferObject

data class ChunkVertexData(val builtOrigin: Long, val vertexCount: Int, val size: Int, val vbo: VertexBufferObject)