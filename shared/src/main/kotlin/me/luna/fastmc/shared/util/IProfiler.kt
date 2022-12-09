package dev.fastmc.common

interface IProfiler {
    fun start(name: String)
    fun swap(name: String)
    fun end()
}