package me.luna.fastmc.mixin

interface IPatchedChunkBuilder {
    fun upload(running: BooleanArray): Int
}