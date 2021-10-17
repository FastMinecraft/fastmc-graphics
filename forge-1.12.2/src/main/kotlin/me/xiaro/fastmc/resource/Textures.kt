package me.xiaro.fastmc.resource

import me.xiaro.fastmc.opengl.DefaultTexture
import me.xiaro.fastmc.opengl.ITexture
import me.xiaro.fastmc.utils.TextureUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.item.EnumDyeColor
import net.minecraft.util.ResourceLocation
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import kotlin.math.PI

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
    fun drawHead(graphics: Graphics2D, oldImage: BufferedImage, scale: Int, identity: AffineTransform) {
        graphics.rotate(PI / -2.0, 0.0, 22.0 * scale)
        graphics.drawImage(
            oldImage,
            0 * scale,
            22 * scale,
            6 * scale,
            38 * scale,
            0 * scale,
            6 * scale,
            6 * scale,
            22 * scale,
            null
        )

        graphics.transform = identity
        graphics.rotate(-PI, 48.0 * scale, 16.0 * scale)
        graphics.drawImage(
            oldImage,
            32 * scale,
            10 * scale,
            48 * scale,
            16 * scale,
            6 * scale,
            0 * scale,
            22 * scale,
            6 * scale,
            null
        )

        graphics.transform = identity
        graphics.rotate(PI / 2.0, 48.0 * scale, 22.0 * scale)
        graphics.drawImage(
            oldImage,
            42 * scale,
            22 * scale,
            48 * scale,
            38 * scale,
            22 * scale,
            6 * scale,
            28 * scale,
            22 * scale,
            null
        )

        graphics.transform = identity
        graphics.drawImage(
            oldImage,
            16 * scale,
            0 * scale,
            32 * scale,
            16 * scale,
            6 * scale,
            6 * scale,
            22 * scale,
            22 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            32 * scale,
            0 * scale,
            48 * scale,
            16 * scale,
            28 * scale,
            28 * scale,
            44 * scale,
            44 * scale,
            null
        )
    }

    fun drawFoot(graphics: Graphics2D, oldImage: BufferedImage, scale: Int, identity: AffineTransform) {
        graphics.rotate(PI / -2.0, 0.0, 44.0 * scale)
        graphics.drawImage(
            oldImage,
            0 * scale,
            44 * scale,
            6 * scale,
            60 * scale,
            0 * scale,
            28 * scale,
            6 * scale,
            44 * scale,
            null
        )

        graphics.transform = identity
        graphics.rotate(PI, 16.0 * scale, 38.0 * scale)
        graphics.drawImage(
            oldImage,
            0 * scale,
            32 * scale,
            16 * scale,
            38 * scale,
            22 * scale,
            22 * scale,
            38 * scale,
            28 * scale,
            null
        )

        graphics.transform = identity
        graphics.rotate(PI / 2.0, 48.0 * scale, 44.0 * scale)
        graphics.drawImage(
            oldImage,
            42 * scale,
            44 * scale,
            48 * scale,
            60 * scale,
            22 * scale,
            28 * scale,
            28 * scale,
            44 * scale,
            null
        )

        graphics.transform = identity
        graphics.drawImage(
            oldImage,
            16 * scale,
            22 * scale,
            32 * scale,
            38 * scale,
            6 * scale,
            28 * scale,
            22 * scale,
            44 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            32 * scale,
            22 * scale,
            48 * scale,
            38 * scale,
            28 * scale,
            28 * scale,
            44 * scale,
            44 * scale,
            null
        )
    }

    fun drawLeg0(graphics: Graphics2D, oldImage: BufferedImage, scale: Int) {
        graphics.drawImage(
            oldImage,
            3 * scale,
            44 * scale,
            9 * scale,
            47 * scale,
            53 * scale,
            0 * scale,
            59 * scale,
            3 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            0 * scale,
            47 * scale,
            9 * scale,
            50 * scale,
            53 * scale,
            3 * scale,
            62 * scale,
            6 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            9 * scale,
            47 * scale,
            12 * scale,
            50 * scale,
            50 * scale,
            3 * scale,
            53 * scale,
            6 * scale,
            null
        )
    }

    fun drawLeg1(graphics: Graphics2D, oldImage: BufferedImage, scale: Int) {
        graphics.drawImage(
            oldImage,
            3 * scale,
            50 * scale,
            9 * scale,
            53 * scale,
            53 * scale,
            0 * scale,
            59 * scale,
            3 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            0 * scale,
            53 * scale,
            6 * scale,
            56 * scale,
            56 * scale,
            3 * scale,
            62 * scale,
            6 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            6 * scale,
            53 * scale,
            12 * scale,
            56 * scale,
            50 * scale,
            3 * scale,
            56 * scale,
            6 * scale,
            null
        )
    }

    fun drawLeg2(graphics: Graphics2D, oldImage: BufferedImage, scale: Int) {
        graphics.drawImage(
            oldImage,
            15 * scale,
            44 * scale,
            21 * scale,
            47 * scale,
            53 * scale,
            0 * scale,
            59 * scale,
            3 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            12 * scale,
            47 * scale,
            24 * scale,
            50 * scale,
            50 * scale,
            3 * scale,
            62 * scale,
            6 * scale,
            null
        )
    }

    fun drawLeg3(graphics: Graphics2D, oldImage: BufferedImage, scale: Int) {
        graphics.drawImage(
            oldImage,
            15 * scale,
            50 * scale,
            21 * scale,
            53 * scale,
            53 * scale,
            0 * scale,
            59 * scale,
            3 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            12 * scale,
            53 * scale,
            15 * scale,
            56 * scale,
            59 * scale,
            3 * scale,
            62 * scale,
            6 * scale,
            null
        )

        graphics.drawImage(
            oldImage,
            15 * scale,
            53 * scale,
            24 * scale,
            56 * scale,
            50 * scale,
            3 * scale,
            59 * scale,
            6 * scale,
            null
        )
    }

    fun transformBedTexture(oldImage: BufferedImage): BufferedImage {
        val newImage = BufferedImage(oldImage.width, oldImage.height, oldImage.type)
        val scale = oldImage.width / 64

        val graphics = newImage.createGraphics()
        val identity = AffineTransform()

        drawHead(graphics, oldImage, scale, identity)
        drawFoot(graphics, oldImage, scale, identity)

        drawLeg0(graphics, oldImage, scale)
        drawLeg1(graphics, oldImage, scale)
        drawLeg2(graphics, oldImage, scale)
        drawLeg3(graphics, oldImage, scale)

        graphics.dispose()

        return newImage
    }

    val images = Array(EnumDyeColor.values().size) {
        val enumDyeColor = EnumDyeColor.values()[it]
        val resourceLocation = ResourceLocation("textures/entity/bed/${enumDyeColor.dyeColorName}.png")
        transformBedTexture(resourceLocation.toBufferedImage(mc))
    }

    return DefaultTexture("tileEntity/Bed", TextureUtils.combineColoredTextures(images))
}

fun shulkerTexture(mc: Minecraft): ITexture {
    val enumDyeColors = EnumDyeColor.values()
    val images = Array(enumDyeColors.size) {
        val enumDyeColor = enumDyeColors[it]
        val resourceLocation = ResourceLocation("textures/entity/shulker/shulker_${enumDyeColor.dyeColorName}.png")
        resourceLocation.toBufferedImage(mc)
    }

    return DefaultTexture("tileEntity/ShulkerBox", TextureUtils.combineColoredTextures(images))
}

fun ResourceLocation.toBufferedImage(mc: Minecraft): BufferedImage {
    return TextureUtil.readBufferedImage(mc.resourceManager.getResource(this).inputStream)
}