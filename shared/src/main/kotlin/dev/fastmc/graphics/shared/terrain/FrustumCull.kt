package dev.fastmc.graphics.shared.terrain

import dev.fastmc.graphics.shared.renderer.IRenderer
import org.joml.FrustumIntersection

abstract class FrustumCull(private val renderer: IRenderer) {
    @Volatile
    private var lastMatrixHash = 0L
    private var isInFrustum0 = false

    fun isInFrustum(): Boolean {
        val frustum = renderer.camera.frustumIntersection
        val matrixHash = renderer.camera.matrixPosHash

        if (matrixHash != lastMatrixHash) {
            isInFrustum0 = isInFrustum(frustum)
            lastMatrixHash = matrixHash
        }

        return isInFrustum0
    }


    fun isInFrustum(frustum: FrustumIntersection, matrixHash: Long): Boolean {
        return if (matrixHash == lastMatrixHash) {
            isInFrustum0
        } else {
            isInFrustum(frustum)
        }
    }

    fun reset() {
        lastMatrixHash = 0L
    }

    abstract fun isInFrustum(frustum: FrustumIntersection): Boolean
}