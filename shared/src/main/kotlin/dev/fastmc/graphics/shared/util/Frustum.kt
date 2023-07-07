package dev.fastmc.graphics.shared.util

import org.joml.Matrix4f
import org.joml.Vector4f

class Frustum(invCombined: Matrix4f) {
    val nearTopLeft = Vector4f(-1.0f, 1.0f, -1.0f, 1.0f)
    val nearTopRight = Vector4f(1.0f, 1.0f, -1.0f, 1.0f)
    val nearBottomLeft = Vector4f(-1.0f, -1.0f, -1.0f, 1.0f)
    val nearBottomRight = Vector4f(1.0f, -1.0f, -1.0f, 1.0f)
    val farTopLeft = Vector4f(-1.0f, 1.0f, 1.0f, 1.0f)
    val farTopRight = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
    val farBottomLeft = Vector4f(-1.0f, -1.0f, 1.0f, 1.0f)
    val farBottomRight = Vector4f(1.0f, -1.0f, 1.0f, 1.0f)

    val planes = arrayOf(
        nearTopLeft,
        nearTopRight,
        nearBottomLeft,
        nearBottomRight,
        farTopLeft,
        farTopRight,
        farBottomLeft,
        farBottomRight
    )

    init {
        for (plane in planes) {
            plane.mulProject(invCombined)
        }
    }
}