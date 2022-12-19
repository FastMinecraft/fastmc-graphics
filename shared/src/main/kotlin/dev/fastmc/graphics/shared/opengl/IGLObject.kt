package dev.fastmc.graphics.shared.opengl

interface IGLObject {
    val id: Int

    fun destroy()
}