package me.xiaro.fastmc.resource

import me.xiaro.fastmc.ResourceLocation
import me.xiaro.fastmc.shared.adapter.BedTexture
import me.xiaro.fastmc.shared.adapter.ChestTexture
import me.xiaro.fastmc.shared.opengl.DefaultTexture
import me.xiaro.fastmc.shared.opengl.ITexture
import me.xiaro.fastmc.shared.util.TextureUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.util.DyeColor
import org.joml.Matrix4f
import java.awt.image.BufferedImage
import java.io.File
import java.text.NumberFormat
import javax.imageio.ImageIO

class ResourceLocationTexture(
    private val mc: MinecraftClient,
    override val resourceName: String,
    private val ResourceLocation: ResourceLocation
) : ITexture {
    override fun bind() {
        mc.textureManager.bindTexture(ResourceLocation)
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

fun main() {
    val a = Matrix4f().identity()
    val b = net.minecraft.util.math.Matrix4f().apply { loadIdentity() }

    println(a.toString(NumberFormat.getNumberInstance()))
    println(b)

    a.scale(-1.0f, 0.5f, 1.0f)
    b.multiply(net.minecraft.util.math.Matrix4f.scale(-1.0f, 0.5f, 1.0f))

    println(a.toString(NumberFormat.getNumberInstance()))
    println(b.toString())
}

fun bedTexture(mc: MinecraftClient): ITexture {
    val images = Array(DyeColor.values().size) {
        val enumDyeColor = DyeColor.values()[it]
        val resourceLocation = ResourceLocation("textures/entity/bed/${enumDyeColor.getName()}.png")
        BedTexture.vAll(resourceLocation.toBufferedImage(mc))
    }

    val image = TextureUtils.combineColoredTextures(images)
    ImageIO.write(image, "png", File("D:/out.png"))

    return DefaultTexture("tileEntity/Bed", image)
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
    return ImageIO.read(mc.resourceManager.getResource(this).inputStream)
}