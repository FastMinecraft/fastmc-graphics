@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("BufferUtils")

package me.luna.fastmc.shared.util

import it.unimi.dsi.fastutil.ints.IntArrayList
import java.nio.*

inline fun allocateInt(capacity: Int): IntBuffer = allocateByte(capacity * 4).asIntBuffer()

inline fun allocateFloat(capacity: Int): FloatBuffer = allocateByte(capacity * 4).asFloatBuffer()

inline fun allocateShort(capacity: Int): ShortBuffer = allocateByte(capacity * 2).asShortBuffer()

inline fun allocateByte(capacity: Int): ByteBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())

inline fun IntArrayList.toIntBuffer(): IntBuffer {
    val buffer = allocateInt(this.size)
    buffer.put(this.elements(), 0, this.size)
    buffer.flip()
    return buffer
}

inline fun Buffer.skip(count: Int) {
    this.position(position() + count)
}