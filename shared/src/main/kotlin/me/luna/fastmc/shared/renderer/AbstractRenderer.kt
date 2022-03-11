package me.luna.fastmc.shared.renderer

import kotlinx.coroutines.*
import me.luna.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.luna.fastmc.shared.renderbuilder.IInfo
import me.luna.fastmc.shared.util.ClassIDRegistry
import me.luna.fastmc.shared.util.ITypeID
import me.luna.fastmc.shared.util.collection.FastIntMap
import org.joml.Matrix4f
import kotlin.coroutines.CoroutineContext

abstract class AbstractRenderer<ET : Any>(
    protected val worldRenderer: AbstractWorldRenderer,
    val registry: ClassIDRegistry<Any>
) :
    IRenderer by worldRenderer {
    protected val renderEntryMap = FastIntMap<AbstractRenderEntry<ET, *>>()
    protected val renderEntryList = ArrayList<AbstractRenderEntry<ET, *>>()

    protected inline fun <reified E : ET, reified B : AbstractRenderBuilder<out IInfo<*>>> register() {
        val entityClass = E::class.java
        @Suppress("UNCHECKED_CAST")
        if (!renderEntryMap.containsKey((registry as ClassIDRegistry<E>).get(entityClass))) {
            register<E>(RenderEntry(B::class.java))
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified E : ET> register(renderEntry: AbstractRenderEntry<out ET, out IInfo<E>>) {
        renderEntryMap[(registry as ClassIDRegistry<E>).get(E::class.java)] = renderEntry as AbstractRenderEntry<ET, *>
        renderEntryList.add(renderEntry)
    }

    protected val lock = Any()
    protected var adding = ArrayList<ET>()
    protected var removing = ArrayList<ET>()

    fun updateEntities(adding: List<ET>, removing: List<ET>) {
        synchronized(lock) {
            this.adding.addAll(adding)
            this.removing.addAll(removing)
        }
    }

    fun clear() {
        renderEntryList.forEach {
            it.clear()
        }
    }

    fun hasRenderer(entity: ET): Boolean {
        return renderEntryMap.containsKey((entity as ITypeID).typeID)
    }

    abstract fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope)

    @OptIn(ObsoleteCoroutinesApi::class)
    protected suspend fun updateRenderers(mainThreadContext: CoroutineContext, force: Boolean) {
        coroutineScope {
            for (entry in renderEntryList) {
                if (force) entry.markDirty()
                launch(Dispatchers.Default) {
                    entry.update(mainThreadContext, this)
                }
            }
        }
    }

    open fun render() {
        renderEntryList.forEach {
            it.render(modelViewMatrix, renderPosX, renderPosY, renderPosZ)
        }
    }

    protected abstract inner class AbstractRenderEntry<E : ET, T : IInfo<E>> {
        abstract fun clear()

        abstract fun add(entity: E)

        abstract fun addAll(collection: Collection<E>)

        abstract fun remove(entity: E): Boolean

        abstract fun removeAll(collection: Collection<E>)

        abstract suspend fun update(mainThreadContext: CoroutineContext, parentScope: CoroutineScope)

        abstract fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double)

        abstract fun destroyRenderer()

        abstract fun markDirty()
    }

    protected inner class RenderEntry<E : ET, T : IInfo<E>>(
        private val builderClass: Class<out AbstractRenderBuilder<in T>>,
    ) : AbstractRenderEntry<E, T>() {
        private var renderer: AbstractRenderBuilder.Renderer? = null
        private val entities = ArrayList<E>()
        private var dirty = false

        override fun clear() {
            if (entities.isNotEmpty()) {
                entities.clear()
                dirty = true
            }
        }

        override fun add(entity: E) {
            entities.add(entity)
            dirty = true
        }

        override fun addAll(collection: Collection<E>) {
            dirty = entities.addAll(collection) || dirty
        }

        override fun remove(entity: E): Boolean {
            val removed = entities.remove(entity)
            dirty = entities.remove(entity) || dirty
            return removed
        }

        override fun removeAll(collection: Collection<E>) {
            @Suppress("ConvertArgumentToSet")
            dirty = entities.removeAll(collection) || dirty
        }

        @Suppress("UNCHECKED_CAST")
        override suspend fun update(
            mainThreadContext: CoroutineContext,
            parentScope: CoroutineScope
        ) {
            if (!dirty) return
            if (entities.isEmpty()) {
                withContext(mainThreadContext) {
                    destroyRenderer()
                }
            } else {
                dirty = false
                val builder = builderClass.newInstance()

                builder.init(this@AbstractRenderer, entities.size)
                builder.addAll(entities as List<T>)

                withContext(mainThreadContext) {
                    renderer?.destroy()
                    renderer = builder.build()
                }
            }
        }

        override fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            renderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
        }

        override fun destroyRenderer() {
            renderer?.destroy()
            renderer = null
            dirty = true
        }

        override fun markDirty() {
            dirty = true
        }
    }
}