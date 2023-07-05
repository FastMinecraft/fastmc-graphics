package dev.fastmc.graphics.resource

import dev.fastmc.graphics.shared.texture.ITexture
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage

class ResourceLocationTexture(
    override val resourceName: String,
    private val resourceLocation: ResourceLocation
) : ITexture {
    override val id: Int
        get() = mc.renderEngine?.getTexture(resourceLocation)?.glTextureId ?: 0

    override fun destroy() {

    }

    private companion object {
        private val mc = Minecraft.getMinecraft()
    }
}

fun ResourceLocation.readImage(resourceManager: net.minecraft.client.resources.IResourceManager): BufferedImage {
    return TextureUtil.readBufferedImage(resourceManager.getResource(this).inputStream)
}