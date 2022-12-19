package dev.fastmc.graphics.renderer

import com.mojang.blaze3d.systems.RenderSystem
import dev.fastmc.graphics.shared.renderer.EntityRenderer
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.shared.util.ITypeID
import dev.fastmc.graphics.util.Minecraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.entity.Entity
import org.lwjgl.opengl.GL11.*
import kotlin.coroutines.CoroutineContext

class EntityRendererImpl(private val mc: Minecraft, worldRenderer: WorldRenderer) :
    EntityRenderer<Entity>(worldRenderer) {
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

    override fun render() {
        RenderSystem.disableCull()
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        super.render()
    }
}