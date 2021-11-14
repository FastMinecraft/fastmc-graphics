package me.xiaro.fastmc.shared.renderer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.xiaro.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.xiaro.fastmc.shared.renderbuilder.IInfo
import org.joml.Matrix4f

abstract class AbstractRenderer<TE : Any>(protected val worldRenderer: AbstractWorldRenderer) :
    IRenderer by worldRenderer {
    protected val renderEntryMap = HashMap<Class<out TE>, AbstractRenderEntry<TE, *>>()
    protected val renderEntryList = ArrayList<AbstractRenderEntry<TE, *>>()

    protected inline fun <reified E : TE, T : IInfo<E>> register(
        infoClass: Class<T>,
        builderClass: Class<out AbstractRenderBuilder<in T>>
    ) {
        val entityClass = E::class.java
        if (!renderEntryMap.containsKey(entityClass)) {
            register(RenderEntry(infoClass, builderClass))
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified E : TE> register(renderEntry: AbstractRenderEntry<out TE, out IInfo<E>>) {
        renderEntryMap[E::class.java] = renderEntry as AbstractRenderEntry<TE, *>
        renderEntryList.add(renderEntry)
    }

    fun clear() {
        renderEntryList.forEach {
            it.clear()
        }
    }

    fun hasRenderer(entity: TE): Boolean {
        return renderEntryMap.containsKey(entity.javaClass)
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

    protected abstract inner class AbstractRenderEntry<E : TE, T : IInfo<E>> {
        abstract fun clear()

        abstract fun add(entity: E)

        abstract fun addAll(list: Collection<E>)

        abstract fun remove(entity: E): Boolean

        abstract fun update(scope: CoroutineScope, actor: SendChannel<() -> Unit>)

        abstract fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double)

        abstract fun destroyRenderer()
    }

    protected inner class RenderEntry<E : TE, T : IInfo<E>>(
        private val infoClass: Class<T>,
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
                scope.launch {
                    val builder = builderClass.newInstance()
                    val entityInfo = infoClass.newInstance()

                    builder.init(this@AbstractRenderer, entities.size)

                    entities.forEach {
                        entityInfo.entity = it
                        builder.add(entityInfo)
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