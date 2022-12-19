package dev.fastmc.graphics.mixin

import dev.fastmc.graphics.shared.util.IProfiler
import dev.fastmc.graphics.util.Minecraft

class Profiler(private val mc: Minecraft) : IProfiler {
    override fun start(name: String) {
        mc.profiler.push(name)
    }

    override fun swap(name: String) {
        mc.profiler.swap(name)
    }

    override fun end() {
        mc.profiler.pop()
    }
}