package dev.fastmc.graphics.shared.instancing

import dev.fastmc.common.ParallelUtils
import dev.fastmc.graphics.FastMcMod
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.opengl.*
import dev.luna5ama.glwrapper.api.*
import dev.fastmc.graphics.shared.renderer.IRenderer
import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.fastmc.graphics.shared.resource.Resource
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.luna5ama.glwrapper.impl.*
import dev.luna5ama.glwrapper.impl.ShaderSource.Companion.invoke
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.Ptr
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

abstract class AbstractInstancingBuilder<T : IInfo<*>>(private val vertexSize: Int) {
    private var resourceManager0: IResourceManager? = null
    private var builtPosX0 = Double.NaN
    private var builtPosY0 = Double.NaN
    private var builtPosZ0 = Double.NaN
    private var size0 = -1

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

    private val buffer = Arr.malloc(0L)

    fun init(renderer: IRenderer, size: Int) {
        check(size0 == -1)
        check(resourceManager0 == null)
        check(builtPosX0.isNaN())
        check(builtPosY0.isNaN())
        check(builtPosZ0.isNaN())

        size0 = size
        resourceManager0 = renderer.resourceManager
        builtPosX0 = renderer.camera.posX
        builtPosY0 = renderer.camera.posY
        builtPosZ0 = renderer.camera.posZ
        buffer.realloc((size * vertexSize).toLong(), true)

        vertexAttribute = buildAttribute(vertexSize, 1) { setupAttribute() }
    }

    suspend fun addAll(entities: List<T>) {
        coroutineScope {
            ParallelUtils.splitListIndex(
                total = entities.size,
                blockForEach = { start, end ->
                    launch(FastMcCoreScope.context) {
                        for (i in start until end) {
                            add(buffer.ptr + (i * vertexSize).toLong(), entities[i])
                        }
                    }
                }
            )
        }
    }

    fun build(): Renderer {
        return uploadBuffer(buffer)
    }

    abstract fun VertexAttribute.Builder.setupAttribute()

    abstract fun add(ptr: Ptr, info: T)

    private lateinit var vertexAttribute: VertexAttribute

    protected abstract val model: ResourceEntry<Model>
    protected abstract val shader: ResourceEntry<InstancingShaderProgram>
    protected abstract val texture: ResourceEntry<ITexture>

    protected open fun uploadBuffer(buffer: Arr): Renderer {
        val shader = shader.get(resourceManager)
        val model = model.get(resourceManager)

        val vao = VertexArrayObject()
        val vbo = BufferObject.Immutable()

        vbo.allocate(buffer.len, buffer.ptr, 0)

        model.attachVbo(vao)
        vao.attachVbo(vbo, vertexAttribute)

        return SingleTextureRenderer(renderInfo(shader, vao, listOf(vbo), model), texture)
    }

    protected fun renderInfo(
        shader: InstancingShaderProgram,
        vao: VertexArrayObject,
        vboList: List<BufferObject>,
        model: Model
    ): RenderInfo {
        return RenderInfo(resourceManager, shader, vao, vboList, model.modelSize, size, builtPosX, builtPosY, builtPosZ)
    }

    open class Renderer(
        renderInfo: RenderInfo
    ) : IRenderInfo by renderInfo {
        fun render(renderer: IRenderer) {
            shader.bind()
            preRender()

            shader.setOffset(
                (builtPosX - renderer.camera.posX).toFloat(),
                (builtPosY - renderer.camera.posY).toFloat(),
                (builtPosZ - renderer.camera.posZ).toFloat(),
            )

            glBindVertexArray(vao.id)
            glDrawArraysInstanced(GL_TRIANGLES, 0, modelSize, size)

            postRender()
        }

        protected open fun preRender() {

        }

        protected open fun postRender() {

        }

        fun destroy() {
            vao.destroyVao()
            vboList.forEach {
                it.destroy()
            }
        }
    }

    class SingleTextureRenderer(
        renderInfo: RenderInfo,
        private val texture: ResourceEntry<ITexture>
    ) : Renderer(renderInfo) {
        override fun preRender() {
            texture.get(resourceManager).bind(0)
        }
    }

    open class InstancingShaderProgram(
        override val resourceName: String,
        vertex: ShaderSource.Vert,
        fragment: ShaderSource.Frag
    ) : ShaderProgram(vertex, fragment { define("LIGHT_MAP_UNIT", FastMcMod.lightMapUnit) }), Resource {
        private val offsetUniform = glGetUniformLocation(id, "offset")

        override fun bind() {
            super.bind()
            bindBuffer(GL_UNIFORM_BUFFER, FastMcMod.worldRenderer.camera.ubo, "Camera")
        }

        fun setOffset(x: Float, y: Float, z: Float) {
            glProgramUniform3f(id, offsetUniform, x, y, z)
        }
    }

    interface IRenderInfo {
        val resourceManager: IResourceManager
        val shader: InstancingShaderProgram
        val vao: VertexArrayObject
        val vboList: List<BufferObject>
        val modelSize: Int
        val size: Int
        val builtPosX: Double
        val builtPosY: Double
        val builtPosZ: Double
    }

    class RenderInfo(
        override val resourceManager: IResourceManager,
        override val shader: InstancingShaderProgram,
        override val vao: VertexArrayObject,
        override val vboList: List<BufferObject>,
        override val modelSize: Int,
        override val size: Int,
        override val builtPosX: Double,
        override val builtPosY: Double,
        override val builtPosZ: Double
    ) : IRenderInfo
}