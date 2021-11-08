package me.xiaro.fastmc.shared.renderer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.xiaro.fastmc.shared.tileentity.TileEntityRenderBuilder
import me.xiaro.fastmc.shared.tileentity.info.ITileEntityInfo
import org.joml.Matrix4f

abstract class AbstractTileEntityRenderer<TE : Any>(protected val worldRenderer: AbstractWorldRenderer) :
    IRenderer by worldRenderer {
    protected val renderEntryMap = HashMap<Class<out TE>, AbstractRenderEntry<TE, *>>()
    protected val renderEntryList = ArrayList<AbstractRenderEntry<TE, *>>()

    protected inline fun <reified E : TE, T : ITileEntityInfo<E>> register(
        tileEntityInfoClass: Class<T>,
        builderClass: Class<out TileEntityRenderBuilder<in T>>
    ) {
        val tileEntityClass = E::class.java
        if (!renderEntryMap.containsKey(tileEntityClass)) {
            addRenderEntry(RenderEntry(tileEntityInfoClass, builderClass))
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified E : TE> addRenderEntry(renderEntry: AbstractRenderEntry<out TE, out ITileEntityInfo<E>>) {
        renderEntryMap[E::class.java] = renderEntry as AbstractRenderEntry<TE, *>
        renderEntryList.add(renderEntry)
    }

    fun clear() {
        renderEntryList.forEach {
            it.clear()
        }
    }

    fun hasRenderer(tileEntity: TE): Boolean {
//        return false
        return renderEntryMap.containsKey(tileEntity.javaClass)
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

    protected abstract inner class AbstractRenderEntry<E : TE, T : ITileEntityInfo<E>> {
        abstract fun clear()

        abstract fun add(tileEntity: E)

        abstract fun addAll(list: Collection<E>)

        abstract fun remove(tileEntity: E): Boolean

        abstract fun update(scope: CoroutineScope, actor: SendChannel<() -> Unit>)

        abstract fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double)

        abstract fun destroyRenderer()
    }

    protected inner class RenderEntry<E : TE, T : ITileEntityInfo<E>>(
        private val tileEntityInfoClass: Class<T>,
        private val builderClass: Class<out TileEntityRenderBuilder<in T>>,
    ) : AbstractRenderEntry<E, T>() {
        private var renderer: TileEntityRenderBuilder.Renderer? = null
        private val tileEntities = ArrayList<E>()
        private var dirty = false

        override fun clear() {
            if (tileEntities.isNotEmpty()) {
                tileEntities.clear()
                dirty = true
            }
        }

        override fun add(tileEntity: E) {
            tileEntities.add(tileEntity)
            dirty = true
        }

        override fun addAll(list: Collection<E>) {
            tileEntities.addAll(list)
            dirty = true
        }

        override fun remove(tileEntity: E): Boolean {
            val removed = tileEntities.remove(tileEntity)
            dirty = tileEntities.remove(tileEntity) || dirty
            return removed
        }

        override fun update(
            scope: CoroutineScope,
            actor: SendChannel<() -> Unit>
        ) {
            if (!dirty) return

            if (tileEntities.isEmpty()) {
                destroyRenderer()
            } else {
                dirty = false
                scope.launch {
                    val builder = builderClass.newInstance()
                    val entityInfo = tileEntityInfoClass.newInstance()

                    builder.init(this@AbstractTileEntityRenderer, tileEntities.size)

                    tileEntities.forEach {
                        entityInfo.tileEntity = it
                        builder.add(entityInfo)
                    }

                    actor.send {
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