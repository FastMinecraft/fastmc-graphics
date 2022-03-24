package me.luna.fastmc.mixin

import net.minecraft.client.render.chunk.ChunkBuilder
import java.util.concurrent.atomic.AtomicBoolean

interface IPatchedTask {
    val cancelled0: AtomicBoolean
    val chunkBuilder: ChunkBuilder
    val builtChunk: ChunkBuilder.BuiltChunk
}