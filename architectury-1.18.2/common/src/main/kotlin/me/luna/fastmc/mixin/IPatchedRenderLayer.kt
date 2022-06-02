package me.luna.fastmc.mixin

import net.minecraft.client.render.RenderLayer

interface IPatchedRenderLayer {
    val index: Int

    companion object {
        inline val RenderLayer.index get() = (this as IPatchedRenderLayer).index
    }
}