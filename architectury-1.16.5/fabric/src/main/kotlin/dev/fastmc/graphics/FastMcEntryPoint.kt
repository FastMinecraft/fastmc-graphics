package dev.fastmc.graphics

import net.fabricmc.api.ModInitializer

class FastMcEntryPoint : ModInitializer {
    override fun onInitialize() {
        println("$javaClass: onInitialize!!!")
    }
}