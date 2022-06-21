package me.luna.fastmc.shared.opengl

interface IGLTargetBinding {
    fun bind(target: Int)
    fun unbind(target: Int)
}