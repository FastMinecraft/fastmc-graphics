package me.luna.fastmc.renderer

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.*
import me.luna.fastmc.EntityCow
import me.luna.fastmc.Minecraft
import me.luna.fastmc.endSection
import me.luna.fastmc.shared.renderbuilder.entity.CowRenderBuilder
import me.luna.fastmc.shared.renderer.AbstractEntityRenderer
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer
import me.luna.fastmc.shared.util.ITypeID
import me.luna.fastmc.startSection
import net.minecraft.entity.Entity
import org.lwjgl.opengl.GL11.*
import kotlin.coroutines.CoroutineContext

class EntityRenderer(private val mc: Minecraft, worldRenderer: AbstractWorldRenderer) :
    AbstractEntityRenderer<Entity>(worldRenderer) {
    init {
        register<EntityCow, CowRenderBuilder>()
    }

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        parentScope.launch(Dispatchers.Default) {
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
                        launch(Dispatchers.Default) {
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
        mc.profiler.startSection("render")

        RenderSystem.disableCull()
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        super.render()

        mc.profiler.endSection()
    }
}