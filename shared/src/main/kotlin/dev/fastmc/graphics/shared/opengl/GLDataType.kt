package dev.fastmc.graphics.shared.opengl

enum class GLDataType(val glEnum: Int, val size: Int) {
    GL_BYTE(dev.fastmc.graphics.shared.opengl.GL_BYTE, 1),
    GL_UNSIGNED_BYTE(dev.fastmc.graphics.shared.opengl.GL_UNSIGNED_BYTE, 1),
    GL_SHORT(dev.fastmc.graphics.shared.opengl.GL_SHORT, 2),
    GL_UNSIGNED_SHORT(dev.fastmc.graphics.shared.opengl.GL_UNSIGNED_SHORT, 2),
    GL_INT(dev.fastmc.graphics.shared.opengl.GL_INT, 4),
    GL_UNSIGNED_INT(dev.fastmc.graphics.shared.opengl.GL_UNSIGNED_INT, 4),
    GL_FLOAT(dev.fastmc.graphics.shared.opengl.GL_FLOAT, 4),
}