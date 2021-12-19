package me.xiaro.fastmc.renderer

import me.xiaro.fastmc.shared.renderbuilder.entity.CowRenderBuilder
import me.xiaro.fastmc.shared.renderer.AbstractEntityRenderer
import me.xiaro.fastmc.shared.renderer.AbstractWorldRenderer
import me.xiaro.fastmc.shared.util.ITypeID
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.EntityCow
import org.lwjgl.opengl.GL11.*

class EntityRenderer(private val mc: Minecraft, worldRenderer: AbstractWorldRenderer) :
    AbstractEntityRenderer<Entity>(worldRenderer) {
    init {
        register<EntityCow, CowRenderBuilder>()
    }

    override fun onPostTick() {
        renderEntryList.forEach {
            it.clear()
        }

        mc.world?.let { world ->
            world.loadedEntityList.forEach {
                renderEntryMap[(it as ITypeID).typeID]?.add(it)
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