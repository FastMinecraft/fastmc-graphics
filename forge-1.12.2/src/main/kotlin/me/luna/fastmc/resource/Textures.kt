package me.luna.fastmc.resource

import me.luna.fastmc.shared.texture.ITexture
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage

class ResourceLocationTexture(
    override val resourceName: String,
    private val resourceLocation: ResourceLocation
) : ITexture {
    override val id: Int
        get() = mc.renderEngine.getTexture(resourceLocation).glTextureId

    override fun bind() {
        mc.renderEngine.bindTexture(resourceLocation)
    }

    override fun destroy() {

    }

    private companion object {
        private val mc = Minecraft.getMinecraft()
    }
}

fun ResourceLocation.readImage(resourceManager: net.minecraft.client.resources.IResourceManager): BufferedImage {
    return TextureUtil.readBufferedImage(resourceManager.getResource(this).inputStream)
}