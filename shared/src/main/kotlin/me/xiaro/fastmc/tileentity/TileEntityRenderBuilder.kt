package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.FastMcMod
import me.xiaro.fastmc.IRenderer
import me.xiaro.fastmc.model.Model
import me.xiaro.fastmc.opengl.*
import me.xiaro.fastmc.resource.IResourceManager
import me.xiaro.fastmc.resource.ResourceEntry
import me.xiaro.fastmc.tileentity.info.ITileEntityInfo
import me.xiaro.fastmc.util.BufferUtils
import org.joml.Matrix4f
import java.nio.ByteBuffer

abstract class TileEntityRenderBuilder<T : ITileEntityInfo<*>>(private val vertexSize: Int) {
    fun init(renderer: IRenderer, size: Int) {
        check(size0 == -1)
        check(resourceManager0 == null)
        check(builtPosX0.isNaN())
        check(builtPosY0.isNaN())
        check(builtPosZ0.isNaN())

        size0 = size
        resourceManager0 = renderer.resourceManager
        builtPosX0 = renderer.renderPosX
        builtPosY0 = renderer.renderPosY
        builtPosZ0 = renderer.renderPosZ
        buffer0 = BufferUtils.byte(size * vertexSize)
    }

    private var resourceManager0: IResourceManager? = null
    private var builtPosX0 = Double.NaN
    private var builtPosY0 = Double.NaN
    private var builtPosZ0 = Double.NaN
    private var size0 = -1
    private var buffer0: ByteBuffer? = null

    protected val resourceManager: IResourceManager
        get() {
            check(resourceManager0 != null)
            return resourceManager0!!
        }

    protected val builtPosX: Double
        get() {
            check(!builtPosX0.isNaN())
            return builtPosX0
        }

    protected val builtPosY: Double
        get() {
            check(!builtPosY0.isNaN())
            return builtPosY0
        }

    protected val builtPosZ: Double
        get() {
            check(!builtPosZ0.isNaN())
            return builtPosZ0
        }

    protected val size: Int
        get() {
            check(size0 != -1)
            return size0
        }

    protected val buffer: ByteBuffer
        get() {
            check(buffer0 != null)
            return buffer0!!
        }

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

    protected fun putHDirection(hDirection: Int) {
        buffer.put(hDirection.toByte())
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

            shader.updateOffset(builtPosX - renderPosX, builtPosY - renderPosY, builtPosZ - renderPosZ)
            shader.updateModelViewMatrix(modelView)

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

    open class Shader(resourceName: String, vertShaderPath: String, fragShaderPath: String) :
        DrawShader(resourceName, vertShaderPath, fragShaderPath) {
        val partialTicksUniform = glGetUniformLocation(id, "partialTicks")

        init {
            bind()
            glUniform1i(glGetUniformLocation(id, "lightMapTexture"), FastMcMod.glWrapper.lightMapUnit)
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