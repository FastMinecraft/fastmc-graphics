package me.luna.fastmc.resource

import me.luna.fastmc.shared.texture.BedTexture
import me.luna.fastmc.shared.texture.DefaultTexture
import me.luna.fastmc.shared.texture.ITexture
import me.luna.fastmc.shared.texture.TextureUtils
import net.minecraft.client.Minecraft
import net.minecraft.item.EnumDyeColor
import net.minecraft.util.ResourceLocation

fun smallChestTexture(mc: Minecraft): ITexture {
    val images = arrayOf(
        ResourceLocation("textures/entity/chest/normal.png").readImage(mc),
        ResourceLocation("textures/entity/chest/trapped.png").readImage(mc),
        ResourceLocation("textures/entity/chest/christmas.png").readImage(mc)
    )

    return DefaultTexture("tileEntity/SmallChest", TextureUtils.combineTexturesVertically(images))
}

fun largeChestTexture(mc: Minecraft): ITexture {
    val images = arrayOf(
        ResourceLocation("textures/entity/chest/normal_double.png").readImage(mc),
        ResourceLocation("textures/entity/chest/trapped_double.png").readImage(mc),
        ResourceLocation("textures/entity/chest/christmas_double.png").readImage(mc)
    )

    return DefaultTexture("tileEntity/LargeChest", TextureUtils.combineTexturesVertically(images))
}

fun bedTexture(mc: Minecraft): ITexture {
    val images = Array(EnumDyeColor.values().size) {
        val enumDyeColor = EnumDyeColor.values()[it]
        val resourceLocation = ResourceLocation("textures/entity/bed/${enumDyeColor.dyeColorName}.png")
        BedTexture.vAll(resourceLocation.readImage(mc))
    }

    return DefaultTexture("tileEntity/Bed", TextureUtils.combineColoredTextures(images))
}

fun shulkerTexture(mc: Minecraft): ITexture {
    val enumDyeColors = EnumDyeColor.values()
    val images = Array(enumDyeColors.size + 1) {
        if (it < 16) {
            val enumDyeColor = enumDyeColors[it]
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_${enumDyeColor.getName()}.png")
            resourceLocation.readImage(mc)
        } else {
            val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_purple.png")
            resourceLocation.readImage(mc)
        }
    }

    return DefaultTexture("tileEntity/ShulkerBox", TextureUtils.combineColoredWithUncoloredTextures(images))
}
