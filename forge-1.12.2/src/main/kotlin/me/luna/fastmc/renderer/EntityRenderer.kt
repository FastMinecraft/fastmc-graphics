package me.luna.fastmc.renderer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.luna.fastmc.shared.renderbuilder.entity.CowRenderBuilder
import me.luna.fastmc.shared.renderer.AbstractEntityRenderer
import me.luna.fastmc.shared.renderer.WorldRenderer
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.ITypeID
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.EntityCow
import org.lwjgl.opengl.GL11.*
import kotlin.coroutines.CoroutineContext

class EntityRenderer(private val mc: Minecraft, worldRenderer: WorldRenderer) :
    AbstractEntityRenderer<Entity>(worldRenderer) {
    init {
        register<EntityCow, CowRenderBuilder>()
    }

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        parentScope.launch(FastMcCoreScope.context) {
            renderEntryList.forEach {
                it.clear()
            }

            mc.world?.let { world ->
                world.loadedEntityList.forEach {
                    renderEntryMap[(it as ITypeID).typeID]?.add(it)
                }

                coroutineScope {
                    for (entry in renderEntryList) {
                        launch {
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
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        super.render()
    }
}