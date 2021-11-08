package me.xiaro.fastmc.resource

import me.xiaro.fastmc.shared.adapter.BedTexture
import me.xiaro.fastmc.shared.opengl.DefaultTexture
import me.xiaro.fastmc.shared.opengl.ITexture
import me.xiaro.fastmc.shared.util.TextureUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.item.EnumDyeColor
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ResourceLocationTexture(
    private val mc: Minecraft,
    override val resourceName: String,
    private val resourceLocation: ResourceLocation
) : ITexture {
    override fun bind() {
        mc.renderEngine.bindTexture(resourceLocation)
    }

    override fun destroy() {

    }
}

fun smallChestTexture(mc: Minecraft): ITexture {
    val images = arrayOf(
        ResourceLocation("textures/entity/chest/normal.png").toBufferedImage(mc),
        ResourceLocation("textures/entity/chest/trapped.png").toBufferedImage(mc),
        ResourceLocation("textures/entity/chest/christmas.png").toBufferedImage(mc)
    )

    return DefaultTexture("tileEntity/SmallChest", TextureUtils.combineTexturesVertically(images))
}

fun largeChestTexture(mc: Minecraft): ITexture {
    val images = arrayOf(
        ResourceLocation("textures/entity/chest/normal_double.png").toBufferedImage(mc),
        ResourceLocation("textures/entity/chest/trapped_double.png").toBufferedImage(mc),
        ResourceLocation("textures/entity/chest/christmas_double.png").toBufferedImage(mc)
    )

    return DefaultTexture("tileEntity/LargeChest", TextureUtils.combineTexturesVertically(images))
}

fun bedTexture(mc: Minecraft): ITexture {
    val images = Array(EnumDyeColor.values().size) {
        val enumDyeColor = EnumDyeColor.values()[it]
        val resourceLocation = ResourceLocation("textures/entity/bed/${enumDyeColor.dyeColorName}.png")
        BedTexture.vAll(resourceLocation.toBufferedImage(mc))
    }

    val image = TextureUtils.combineColoredTextures(images)
    ImageIO.write(image, "png", File("D:/out.png"))

    return DefaultTexture("tileEntity/Bed", TextureUtils.combineColoredTextures(images))
}

fun shulkerTexture(mc: Minecraft): ITexture {
    val enumDyeColors = EnumDyeColor.values()
    val images = Array(enumDyeColors.size + 1) {
        if (it < 16) {
            val enumDyeColor = enumDyeColors[it]
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_${enumDyeColor.getName()}.png")
            resourceLocation.toBufferedImage(mc)
        } else {
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_purple.png")
            resourceLocation.toBufferedImage(mc)
        }
    }

    return DefaultTexture("tileEntity/ShulkerBox", TextureUtils.combineColoredWithUncoloredTextures(images))
}

fun ResourceLocation.toBufferedImage(mc: Minecraft): BufferedImage {
    return TextureUtil.readBufferedImage(mc.resourceManager.getResource(this).inputStream)
}