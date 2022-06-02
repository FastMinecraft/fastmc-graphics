package me.luna.fastmc.shared.opengl.impl

import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.util.allocateByte
import java.nio.ByteBuffer

class UniformBufferObject(val blockName: String, size: Int): IGLObject {
    private val serverBuffer = BufferObject.Immutable(BufferObject.Target.GL_UNIFORM_BUFFER).apply {
        allocate(size, GL_DYNAMIC_STORAGE_BIT)
    }
    val clientBuffer = allocateByte(size)

    override val id: Int
        get() = serverBuffer.id

    inline fun update(crossinline block: (ByteBuffer) -> Unit) {
        clientBuffer.clear()
        block.invoke(clientBuffer)
        upload()
    }

    fun upload() {
        clientBuffer.flip()
        serverBuffer.invalidate()
        glNamedBufferSubData(serverBuffer.id, 0, clientBuffer)
    }

    override fun bind() {
        serverBuffer.bind()
    }

    override fun unbind() {
        serverBuffer.unbind()
    }

    override fun destroy() {
        serverBuffer.destroy()
    }
}