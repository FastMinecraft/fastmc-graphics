package me.luna.fastmc.mixin

import dev.fastmc.common.IProfiler
import me.luna.fastmc.util.Minecraft

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