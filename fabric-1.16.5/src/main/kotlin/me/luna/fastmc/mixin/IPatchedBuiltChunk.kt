package me.luna.fastmc.mixin

import net.minecraft.client.gl.VertexBuffer

interface IPatchedBuiltChunk {
    var index: Int
    val bufferArray: Array<VertexBuffer>
}