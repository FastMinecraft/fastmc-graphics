package dev.fastmc.graphics.shared.opengl

interface IGLTargetBinding {
    fun bind(target: Int)
    fun unbind(target: Int)
}