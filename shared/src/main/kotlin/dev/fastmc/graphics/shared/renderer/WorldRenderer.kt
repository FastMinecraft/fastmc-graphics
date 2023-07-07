package dev.fastmc.graphics.shared.renderer

import dev.fastmc.graphics.shared.terrain.TerrainRenderer
import dev.luna5ama.glwrapper.api.GL_DYNAMIC_STORAGE_BIT
import dev.luna5ama.glwrapper.api.glInvalidateBufferData
import dev.luna5ama.glwrapper.api.glNamedBufferSubData
import dev.luna5ama.glwrapper.impl.*
import dev.luna5ama.kmogus.MemoryStack
import dev.luna5ama.kmogus.asMutable
import dev.luna5ama.kmogus.copyToMutableArr
import kotlinx.coroutines.CoroutineScope
import org.joml.Matrix4f
import kotlin.coroutines.CoroutineContext

abstract class WorldRenderer : IRenderer {
    lateinit var tileEntityRenderer: TileEntityInstancingRenderer<*>; private set
    lateinit var entityRenderer: EntityInstancingRenderer<*>; private set
    lateinit var terrainRenderer: TerrainRenderer; private set

    final override val camera = Camera()

    fun init(
        tileEntityRenderer: TileEntityInstancingRenderer<*>,
        entityRenderer: EntityInstancingRenderer<*>,
        terrainRenderer: TerrainRenderer
    ) {
        check(!this::tileEntityRenderer.isInitialized)
        check(!this::entityRenderer.isInitialized)
        check(!this::terrainRenderer.isInitialized)

        this.tileEntityRenderer = tileEntityRenderer
        this.entityRenderer = entityRenderer
        this.terrainRenderer = terrainRenderer
    }

    abstract fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope)

    abstract fun preRender(partialTicks: Float)

    abstract fun postRender()

    open fun destroy() {
        tileEntityRenderer.destroy()
        entityRenderer.destroy()
        terrainRenderer.destroy()
    }
}