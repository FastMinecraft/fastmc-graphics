package me.xiaro.fastmc.renderer

import me.xiaro.fastmc.entity.CowInfo
import me.xiaro.fastmc.shared.renderbuilder.entity.CowRenderBuilder
import me.xiaro.fastmc.shared.renderer.AbstractEntityRenderer
import me.xiaro.fastmc.shared.renderer.AbstractWorldRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import org.lwjgl.opengl.GL11.*

class EntityRenderer(private val mc: Minecraft, worldRenderer: AbstractWorldRenderer) :
    AbstractEntityRenderer<Entity>(worldRenderer) {
    init {
        register(CowInfo::class.java, CowRenderBuilder::class.java)
    }

    override fun onPostTick() {
        renderEntryList.forEach {
            it.clear()
        }

        mc.world?.let { world ->
            world.loadedEntityList
                .groupBy {
                    it::class.java
                }.forEach { (clazz, entities) ->
                    renderEntryMap[clazz]?.addAll(entities)
                }

            updateRenderers()
        } ?: run {
            renderEntryList.forEach {
                it.destroyRenderer()
            }
        }
    }

    override fun render() {
        mc.profiler.startSection("render")

        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        super.render()

        mc.profiler.endSection()
    }
}