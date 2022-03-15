package me.luna.fastmc.resource

import me.luna.fastmc.shared.texture.ITexture
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage

class ResourceLocationTexture(
    private val mc: Minecraft,
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
}

fun ResourceLocation.readImage(mc: Minecraft): BufferedImage {
    return TextureUtil.readBufferedImage(mc.resourceManager.getResource(this).inputStream)
}