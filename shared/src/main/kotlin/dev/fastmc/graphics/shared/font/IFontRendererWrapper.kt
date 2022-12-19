package dev.fastmc.graphics.shared.font

import org.joml.Matrix4f

interface IFontRendererWrapper {
    val wrapped: FontRenderer

    fun drawString(
        projection: Matrix4f,
        modelView: Matrix4f,
        string: String,
        posX: Float,
        posY: Float,
        color: Int,
        scale: Float,
        drawShadow: Boolean
    )

    fun destroy() {
        wrapped.destroy()
    }
}