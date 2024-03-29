package dev.fastmc.graphics.shared

class Config {
    val glErrorDebug: Boolean
        get() = false

    val fps: Boolean
        get() = true

    val avgFps: Boolean
        get() = true

    val maxFrameTime: Boolean
        get() = true

    val chunkUpdate: Boolean
        get() = true
}