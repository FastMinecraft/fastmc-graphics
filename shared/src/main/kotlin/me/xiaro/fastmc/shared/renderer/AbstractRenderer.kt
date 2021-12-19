package me.xiaro.fastmc.shared.renderer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.xiaro.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.xiaro.fastmc.shared.renderbuilder.IInfo
import me.xiaro.fastmc.shared.util.ClassIDRegistry
import me.xiaro.fastmc.shared.util.ITypeID
import me.xiaro.fastmc.shared.util.collection.FastIntMap
import org.joml.Matrix4f

abstract class AbstractRenderer<ET : Any>(protected val worldRenderer: AbstractWorldRenderer, val registry: ClassIDRegistry<Any>) :
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

    fun clear() {
        renderEntryList.forEach {
            it.clear()
        }
    }

    fun hasRenderer(entity: ET): Boolean {
        return renderEntryMap.containsKey((entity as ITypeID).typeID)
    }

    abstract fun onPostTick()

    @OptIn(ObsoleteCoroutinesApi::class)
    protected fun updateRenderers() {
        runBlocking {
            val actor = actor<() -> Unit> {
                for (block in channel) {
                    block.invoke()
                }
            }

            coroutineScope {
                for (entry in renderEntryList) {
                    entry.update(this, actor)
                }
            }

            actor.close()
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

        abstract fun addAll(list: Collection<E>)

        abstract fun remove(entity: E): Boolean

        abstract fun update(scope: CoroutineScope, actor: SendChannel<() -> Unit>)

        abstract fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double)

        abstract fun destroyRenderer()
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

        override fun addAll(list: Collection<E>) {
            entities.addAll(list)
            dirty = true
        }

        override fun remove(entity: E): Boolean {
            val removed = entities.remove(entity)
            dirty = entities.remove(entity) || dirty
            return removed
        }

        override fun update(
            scope: CoroutineScope,
            actor: SendChannel<() -> Unit>
        ) {
            if (!dirty) return

            if (entities.isEmpty()) {
                destroyRenderer()
            } else {
                dirty = false
                scope.launch(Dispatchers.Default) {
                    val builder = builderClass.newInstance()

                    builder.init(this@AbstractRenderer, entities.size)

                    entities.forEach {
                        @Suppress("UNCHECKED_CAST")
                        builder.add(it as T)
                    }

                    actor.send {
                        renderer?.destroy()
                        renderer = builder.build()
                    }
                }
            }
        }

        override fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            renderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
        }

        override fun destroyRenderer() {
            renderer?.destroy()
            renderer = null
            dirty = false
        }
    }
}