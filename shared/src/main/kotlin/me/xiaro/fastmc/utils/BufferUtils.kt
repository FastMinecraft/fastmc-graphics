package me.xiaro.fastmc.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

object BufferUtils {
    @JvmStatic
    fun float(capacity: Int): FloatBuffer = byte(capacity * 4).asFloatBuffer()

    @JvmStatic
    fun short(capacity: Int): ShortBuffer = byte(capacity * 2).asShortBuffer()

    @JvmStatic
    fun byte(capacity: Int): ByteBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
}