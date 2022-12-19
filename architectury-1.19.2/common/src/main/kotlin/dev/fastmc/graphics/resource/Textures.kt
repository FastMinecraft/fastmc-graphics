package dev.fastmc.graphics.resource

import dev.fastmc.graphics.shared.texture.*
import dev.fastmc.graphics.util.ResourceLocation
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

fun smallChestTexture(resourceManager: net.minecraft.resource.ResourceManager): ITexture {
    val images = arrayOf(
        transformSmallChestTexture(resourceManager, "normal"),
        transformSmallChestTexture(resourceManager, "trapped"),
        transformSmallChestTexture(resourceManager, "christmas")
    )

    return DefaultTexture("tileEntity/SmallChest", TextureUtils.combineTexturesVertically(images))
}

fun transformSmallChestTexture(resourceManager: net.minecraft.resource.ResourceManager, name: String): BufferedImage {
    val input = ResourceLocation("textures/entity/chest/$name.png").toBufferedImage(resourceManager)
    return ChestTexture.v115Small(input)
}

fun largeChestTexture(resourceManager: net.minecraft.resource.ResourceManager): ITexture {
    fun transformTexture(resourceManager: net.minecraft.resource.ResourceManager, name: String): BufferedImage {
        val left = ResourceLocation("textures/entity/chest/${name}_left.png").toBufferedImage(resourceManager)
        val right = ResourceLocation("textures/entity/chest/${name}_right.png").toBufferedImage(resourceManager)
        return ChestTexture.v115Large(left, right)
    }

    val images = arrayOf(
        transformTexture(resourceManager, "normal"),
        transformTexture(resourceManager, "trapped"),
        transformTexture(resourceManager, "christmas")
    )

    return DefaultTexture("tileEntity/LargeChest", TextureUtils.combineTexturesVertically(images))
}

fun bedTexture(resourceManager: net.minecraft.resource.ResourceManager): ITexture {
    val images = Array(DyeColor.values().size) {
        val enumDyeColor = DyeColor.values()[it]
        val resourceLocation = ResourceLocation("textures/entity/bed/${enumDyeColor.getName()}.png")
        BedTexture.vAll(resourceLocation.toBufferedImage(resourceManager))
    }

    return DefaultTexture("tileEntity/Bed", TextureUtils.combineColoredTextures(images))
}

fun shulkerTexture(resourceManager: net.minecraft.resource.ResourceManager): ITexture {
    val enumDyeColors = DyeColor.values()
    val images = Array(enumDyeColors.size + 1) {
        if (it < 16) {
            val enumDyeColor = enumDyeColors[it]
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_${enumDyeColor.getName()}.png")
            resourceLocation.toBufferedImage(resourceManager)
        } else {
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker.png")
            resourceLocation.toBufferedImage(resourceManager)
        }
    }

    return DefaultTexture("tileEntity/ShulkerBox", TextureUtils.combineColoredWithUncoloredTextures(images))
}

fun ResourceLocation.toBufferedImage(resourceManager: net.minecraft.resource.ResourceManager): BufferedImage {
    return resourceManager.getResource(this).get().inputStream.use {
        ImageIO.read(it)
    }
}