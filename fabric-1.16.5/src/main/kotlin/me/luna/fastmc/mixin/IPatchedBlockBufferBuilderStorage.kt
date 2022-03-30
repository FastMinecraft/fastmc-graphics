package me.luna.fastmc.mixin

import me.luna.fastmc.terrain.ChunkBuilderContext

interface IPatchedBlockBufferBuilderStorage {
    val context: ChunkBuilderContext
}