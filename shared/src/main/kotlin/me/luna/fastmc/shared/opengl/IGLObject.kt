package me.luna.fastmc.shared.opengl

interface IGLObject {
    val id: Int

    fun bind()

    fun unbind()

    fun destroy()
}