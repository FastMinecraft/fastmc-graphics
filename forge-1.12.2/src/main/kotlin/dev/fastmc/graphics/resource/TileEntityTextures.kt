package dev.fastmc.graphics.resource

import dev.fastmc.graphics.shared.texture.BedTexture
import dev.fastmc.graphics.shared.texture.DefaultTexture
import dev.fastmc.graphics.shared.texture.ITexture
import dev.fastmc.graphics.shared.texture.TextureUtils
import net.minecraft.item.EnumDyeColor
import net.minecraft.util.ResourceLocation

fun smallChestTexture(resourceManager: net.minecraft.client.resources.IResourceManager): ITexture {
    return DefaultTexture(
        "tileEntity/SmallChest", TextureUtils.combineTexturesVertically(
            arrayOf(
                ResourceLocation("textures/entity/chest/normal.png").readImage(resourceManager),
                ResourceLocation("textures/entity/chest/trapped.png").readImage(resourceManager),
                ResourceLocation("textures/entity/chest/christmas.png").readImage(resourceManager)
            )
        )
    )
}

fun largeChestTexture(resourceManager: net.minecraft.client.resources.IResourceManager): ITexture {
    return DefaultTexture(
        "tileEntity/LargeChest", TextureUtils.combineTexturesVertically(
            arrayOf(
                ResourceLocation("textures/entity/chest/normal_double.png").readImage(resourceManager),
                ResourceLocation("textures/entity/chest/trapped_double.png").readImage(resourceManager),
                ResourceLocation("textures/entity/chest/christmas_double.png").readImage(resourceManager)
            )
        )
    )
}

fun bedTexture(resourceManager: net.minecraft.client.resources.IResourceManager): ITexture {
    val images = Array(EnumDyeColor.values().size) {
        val enumDyeColor = EnumDyeColor.values()[it]
        val resourceLocation = ResourceLocation("textures/entity/bed/${enumDyeColor.dyeColorName}.png")
        BedTexture.vAll(resourceLocation.readImage(resourceManager))
    }

    return DefaultTexture("tileEntity/Bed", TextureUtils.combineColoredTextures(images))
}

fun shulkerTexture(resourceManager: net.minecraft.client.resources.IResourceManager): ITexture {
    val enumDyeColors = EnumDyeColor.values()
    val images = Array(enumDyeColors.size + 1) {
        if (it < 16) {
            val enumDyeColor = enumDyeColors[it]
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_${enumDyeColor.getName()}.png")
            resourceLocation.readImage(resourceManager)
        } else {
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_purple.png")
            resourceLocation.readImage(resourceManager)
        }
    }

    return DefaultTexture("tileEntity/ShulkerBox", TextureUtils.combineColoredWithUncoloredTextures(images))
}