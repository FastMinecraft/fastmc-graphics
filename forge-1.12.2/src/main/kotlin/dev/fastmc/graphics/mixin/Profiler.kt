package dev.fastmc.graphics.mixin

import dev.fastmc.graphics.shared.util.IProfiler
import net.minecraft.client.Minecraft

class Profiler(private val mc: Minecraft) : IProfiler {
    override fun start(name: String) {
        mc.profiler.startSection(name)
    }

    override fun swap(name: String) {
        mc.profiler.endStartSection(name)
    }

    override fun end() {
        mc.profiler.endSection()
    }
}