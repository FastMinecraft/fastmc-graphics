package me.luna.fastmc.shared.renderbuilder

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.opengl.*
import me.luna.fastmc.shared.renderer.IRenderer
import me.luna.fastmc.shared.resource.IResourceManager
import me.luna.fastmc.shared.resource.ResourceEntry
import me.luna.fastmc.shared.texture.ITexture
import me.luna.fastmc.shared.util.BufferUtils
import me.luna.fastmc.shared.util.ParallelUtils
import org.joml.Matrix4f
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class AbstractRenderBuilder<T : IInfo<*>>(private val vertexSize: Int) {
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

    private val size: Int
        get() {
            check(size0 != -1)
            return size0
        }

    private val buffer: ByteBuffer
        get() {
            check(buffer0 != null)
            return buffer0!!
        }

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

        vertexAttribute = buildAttribute(vertexSize) { setupAttribute() }
    }

    suspend fun addAll(entities: List<T>) {
        coroutineScope {
            ParallelUtils.splitListIndex(
                total = entities.size,
                blockForEach = { start, end ->
                    launch {
                        val regionBuffer = buffer.duplicate().order(ByteOrder.nativeOrder())
                        regionBuffer.position(start * vertexSize)

                        for (i in start until end) {
                            add(regionBuffer, entities[i])
                        }
                    }
                },
                blockForRemaining = { start, end ->
                    buffer.position(start * vertexSize)

                    for (i in start until end) {
                        add(buffer, entities[i])
                    }
                }
            )
        }
    }

    fun build(): Renderer {
        buffer.position(0).limit(buffer.capacity())
        return uploadBuffer(buffer)
    }

    abstract fun VertexAttribute.Builder.setupAttribute()

    abstract fun add(buffer: ByteBuffer, info: T)

    private lateinit var vertexAttribute: VertexAttribute

    protected abstract val model: ResourceEntry<Model>
    protected abstract val shader: ResourceEntry<Shader>
    protected abstract val texture: ResourceEntry<ITexture>

    protected open fun uploadBuffer(buffer: ByteBuffer): Renderer {
        val shader = shader.get(resourceManager)
        val model = model.get(resourceManager)

        val vaoID = glGenVertexArrays()
        val vboID = glGenBuffers()

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW)

        glBindVertexArray(vaoID)

        vertexAttribute.apply()

        model.attachVBO()

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        return SingleTextureRenderer(renderInfo(shader, vaoID, vboID, model), texture)
    }

    protected fun renderInfo(shader: Shader, vaoID: Int, vboID: Int, model: Model): RenderInfo {
        return RenderInfo(resourceManager, shader, vaoID, vboID, model.modelSize, size, builtPosX, builtPosY, builtPosZ)
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

    class SingleTextureRenderer(
        renderInfo: RenderInfo,
        private val texture: ResourceEntry<ITexture>
    ) : Renderer(renderInfo) {
        override fun preRender() {
            texture.get(resourceManager).bind()
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