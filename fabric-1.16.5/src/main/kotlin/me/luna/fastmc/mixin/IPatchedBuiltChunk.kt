package me.luna.fastmc.mixin

import me.luna.fastmc.terrain.ChunkVertexData
import me.luna.fastmc.terrain.RenderRegion
import net.minecraft.client.render.chunk.ChunkBuilder

interface IPatchedBuiltChunk {
    val chunkBuilder: ChunkBuilder

    var index: Int
    val chunkVertexDataArray: Array<ChunkVertexData?>
    var region: RenderRegion

}