package me.luna.fastmc.shared.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

object BufferUtils {
    @JvmStatic
    fun allocateFloat(capacity: Int): FloatBuffer = allocateByte(capacity * 4).asFloatBuffer()

    @JvmStatic
    fun allocateShort(capacity: Int): ShortBuffer = allocateByte(capacity * 2).asShortBuffer()

    @JvmStatic
    fun allocateByte(capacity: Int): ByteBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
}