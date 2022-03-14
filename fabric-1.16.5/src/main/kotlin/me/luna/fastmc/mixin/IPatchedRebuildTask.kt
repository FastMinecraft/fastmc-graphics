package me.luna.fastmc.mixin

import net.minecraft.client.render.chunk.ChunkBuilder

interface IPatchedRebuildTask {
    val builtChunk: ChunkBuilder.BuiltChunk
}