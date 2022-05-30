package me.luna.fastmc.shared.opengl

enum class GLDataType(val glEnum: Int, val size: Int) {
    GL_BYTE(me.luna.fastmc.shared.opengl.GL_BYTE, 1),
    GL_UNSIGNED_BYTE(me.luna.fastmc.shared.opengl.GL_UNSIGNED_BYTE, 1),
    GL_SHORT(me.luna.fastmc.shared.opengl.GL_SHORT, 2),
    GL_UNSIGNED_SHORT(me.luna.fastmc.shared.opengl.GL_UNSIGNED_SHORT, 2),
    GL_INT(me.luna.fastmc.shared.opengl.GL_INT, 4),
    GL_UNSIGNED_INT(me.luna.fastmc.shared.opengl.GL_UNSIGNED_INT, 4),
    GL_FLOAT(me.luna.fastmc.shared.opengl.GL_FLOAT, 4),
}