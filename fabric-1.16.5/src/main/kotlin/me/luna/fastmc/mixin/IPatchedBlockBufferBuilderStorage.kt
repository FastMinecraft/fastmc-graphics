package me.luna.fastmc.mixin

import me.luna.fastmc.shared.util.CachedByteBuffer

interface IPatchedBlockBufferBuilderStorage {
    val cachedByteBuffer: CachedByteBuffer
}