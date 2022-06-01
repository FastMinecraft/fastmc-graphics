package me.luna.fastmc.resource

import me.luna.fastmc.shared.texture.*
import me.luna.fastmc.util.ResourceLocation
import net.minecraft.client.MinecraftClient
import net.minecraft.util.DyeColor
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ResourceLocationTexture(
    private val mc: MinecraftClient,
    override val resourceName: String,
    private val resourceLocation: ResourceLocation
) : ITexture {
    override val id: Int
        get() = mc.textureManager.getTexture(resourceLocation)!!.glId

    override fun bind() {
        mc.textureManager.bindTexture(resourceLocation)
    }

    override fun destroy() {

    }
}

fun smallChestTexture(mc: MinecraftClient): ITexture {
    val images = arrayOf(
        transformSmallChestTexture(mc, "normal"),
        transformSmallChestTexture(mc, "trapped"),
        transformSmallChestTexture(mc, "christmas")
    )

    return DefaultTexture("tileEntity/SmallChest", TextureUtils.combineTexturesVertically(images))
}

fun transformSmallChestTexture(mc: MinecraftClient, name: String): BufferedImage {
    val input = ResourceLocation("textures/entity/chest/$name.png").toBufferedImage(mc)
    return ChestTexture.v115Small(input)
}

fun largeChestTexture(mc: MinecraftClient): ITexture {
    fun transformTexture(mc: MinecraftClient, name: String): BufferedImage {
        val left = ResourceLocation("textures/entity/chest/${name}_left.png").toBufferedImage(mc)
        val right = ResourceLocation("textures/entity/chest/${name}_right.png").toBufferedImage(mc)
        return ChestTexture.v115Large(left, right)
    }

    val images = arrayOf(
        transformTexture(mc, "normal"),
        transformTexture(mc, "trapped"),
        transformTexture(mc, "christmas")
    )

    return DefaultTexture("tileEntity/LargeChest", TextureUtils.combineTexturesVertically(images))
}

fun bedTexture(mc: MinecraftClient): ITexture {
    val images = Array(DyeColor.values().size) {
        val enumDyeColor = DyeColor.values()[it]
        val resourceLocation = ResourceLocation("textures/entity/bed/${enumDyeColor.getName()}.png")
        BedTexture.vAll(resourceLocation.toBufferedImage(mc))
    }

    return DefaultTexture("tileEntity/Bed", TextureUtils.combineColoredTextures(images))
}

fun shulkerTexture(mc: MinecraftClient): ITexture {
    val enumDyeColors = DyeColor.values()
    val images = Array(enumDyeColors.size + 1) {
        if (it < 16) {
            val enumDyeColor = enumDyeColors[it]
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_${enumDyeColor.getName()}.png")
            resourceLocation.toBufferedImage(mc)
        } else {
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker.png")
            resourceLocation.toBufferedImage(mc)
        }
    }

    return DefaultTexture("tileEntity/ShulkerBox", TextureUtils.combineColoredWithUncoloredTextures(images))
}

fun ResourceLocation.toBufferedImage(mc: MinecraftClient): BufferedImage {
    return mc.resourceManager.getResource(this).inputStream.use {
        ImageIO.read(it)
    }
}