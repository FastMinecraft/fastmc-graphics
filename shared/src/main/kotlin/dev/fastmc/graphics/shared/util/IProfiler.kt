package dev.fastmc.graphics.shared.util

interface IProfiler {
    fun start(name: String)
    fun swap(name: String)
    fun end()
}