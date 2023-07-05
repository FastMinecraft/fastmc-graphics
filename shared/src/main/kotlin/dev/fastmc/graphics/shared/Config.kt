package dev.fastmc.graphics.shared

import java.util.Properties

class Config(properties: Properties) {
    var glErrorDebug = properties.getProperty("glErrorDebug", "false").toBoolean()
    var fps = properties.getProperty("fps", "true").toBoolean()
    var avgFps = properties.getProperty("avgFps", "true").toBoolean()
    var maxFrameTime = properties.getProperty("maxFrameTime", "true").toBoolean()
    var chunkUpdate = properties.getProperty("chunkUpdate", "true").toBoolean()

    fun toProperties(): Properties {
        val properties = Properties()
        properties.setProperty("glErrorDebug", glErrorDebug.toString())
        properties.setProperty("fps", fps.toString())
        properties.setProperty("avgFps", avgFps.toString())
        properties.setProperty("maxFrameTime", maxFrameTime.toString())
        properties.setProperty("chunkUpdate", chunkUpdate.toString())
        return properties
    }
}