package me.luna.fastmc.mixin

import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import net.minecraft.client.renderer.chunk.RenderChunk
import net.minecraft.util.BlockRenderLayer

interface IPatchedChunkRenderContainer {
    fun renderChunkLayer(list: FastObjectArrayList<RenderChunk>, layer: BlockRenderLayer)
}