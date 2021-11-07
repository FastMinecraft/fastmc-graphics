package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.model.Model
import me.xiaro.fastmc.opengl.*
import me.xiaro.fastmc.resource.IResourceManager
import me.xiaro.fastmc.resource.ResourceEntry
import me.xiaro.fastmc.tileentity.info.ITileEntityInfo
import me.xiaro.fastmc.util.BufferUtils
import org.joml.Matrix4f
import java.nio.ByteBuffer

abstract class TileEntityRenderBuilder<T : ITileEntityInfo<*>>(
    protected val resourceManager: IResourceManager,
    protected val builtPosX: Double,
    protected val builtPosY: Double,
    protected val builtPosZ: Double,
    protected val size: Int,
    vertexSize: Int
) {
    protected val buffer: ByteBuffer = BufferUtils.byte(size * vertexSize)

    fun build(): Renderer {
        buffer.flip()
        return uploadBuffer(buffer)
    }

    protected abstract fun uploadBuffer(buffer: ByteBuffer): Renderer

    abstract fun add(info: T)

    protected fun putPos(info: T) {
        buffer.putFloat((info.posX + 0.5 - builtPosX).toFloat())
        buffer.putFloat((info.posY - builtPosY).toFloat())
        buffer.putFloat((info.posZ + 0.5 - builtPosZ).toFloat())
    }

    protected fun putLightMapUV(lightMapUV: Int) {
        buffer.put((lightMapUV and 0xFF).toByte())
        buffer.put((lightMapUV shr 16 and 0xFF).toByte())
    }

    protected fun renderInfo(shader: Shader, vaoID: Int, vboID: Int, model: Model): RenderInfo {
        return RenderInfo(resourceManager, shader, vaoID, vboID, model.modelSize, size, builtPosX, builtPosY, builtPosZ)
    }

    protected companion object {
        fun model(name: String): ResourceEntry<Model> {
            return ResourceEntry(Model::class.java, "tileEntity/$name") {
                it.model
            }
        }

        fun shader(name: String): ResourceEntry<Shader> {
            return ResourceEntry(Shader::class.java, "tileEntity/$name") {
                it.tileEntityShader
            }
        }

        fun texture(name: String): ResourceEntry<ITexture> {
            return ResourceEntry(ITexture::class.java, "tileEntity/$name") {
                it.texture
            }
        }
    }

    open class Renderer(
        renderInfo: RenderInfo
    ) : IRenderInfo by renderInfo {
        fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            shader.bind()
            preRender()

            val x = builtPosX - renderPosX
            val y = builtPosY - renderPosY
            val z = builtPosZ - renderPosZ

            shader.updateModelViewMatrix(
                modelView.translate(x.toFloat(), y.toFloat(), z.toFloat(), Matrix4f())
            )

            glBindVertexArray(vaoID)
            glDrawArraysInstanced(GL_TRIANGLES, 0, modelSize, size)
            glBindVertexArray(0)

            postRender()
        }

        protected open fun preRender() {

        }

        protected open fun postRender() {

        }

        fun destroy() {
            glDeleteVertexArrays(vaoID)
            glDeleteBuffers(vboID)
            glDeleteBuffers(vboID)
        }
    }

    open class Shader(resourceName: String, vertShaderPath: String, fragShaderPath: String) : DrawShader(resourceName, vertShaderPath, fragShaderPath) {
        val partialTicksUniform = glGetUniformLocation(id, "partialTicks")

        init {
            bind()
            glUniform1i(glGetUniformLocation(id, "lightMapTexture"), 1)
            unbind()
        }
    }

    interface IRenderInfo {
        val resourceManager: IResourceManager
        val shader: Shader
        val vaoID: Int
        val vboID: Int
        val modelSize: Int
        val size: Int
        val builtPosX: Double
        val builtPosY: Double
        val builtPosZ: Double
    }

    class RenderInfo(
        override val resourceManager: IResourceManager,
        override val shader: Shader,
        override val vaoID: Int,
        override val vboID: Int,
        override val modelSize: Int,
        override val size: Int,
        override val builtPosX: Double,
        override val builtPosY: Double,
        override val builtPosZ: Double
    ) : IRenderInfo
}