package dev.fastmc.graphics.renderer

import dev.fastmc.graphics.shared.renderer.EntityInstancingRenderer
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.shared.util.ITypeID
import dev.fastmc.graphics.util.Minecraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.entity.Entity
import kotlin.coroutines.CoroutineContext

class EntityInstancingRendererImpl(private val mc: Minecraft, worldRenderer: WorldRenderer) :
    EntityInstancingRenderer<Entity>(worldRenderer) {
    init {
//        register<EntityCow, CowInstancingBuilder>()
    }

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        parentScope.launch(FastMcCoreScope.context) {
            renderEntryList.forEach {
                it.clear()
            }

            mc.world?.let { world ->
                world.entities
                    .forEach {
                        renderEntryMap[(it as ITypeID).typeID]?.add(it)
                    }

                coroutineScope {
                    for (entry in renderEntryList) {
                        launch(FastMcCoreScope.context) {
                            entry.markDirty()
                            entry.update(mainThreadContext, this)
                        }
                    }
                }
            } ?: run {
                withContext(mainThreadContext) {
                    renderEntryList.forEach {
                        it.destroyRenderer()
                    }
                }
            }
        }
    }

}