package me.luna.fastmc.shared.texture

import me.luna.fastmc.shared.util.MathUtils
import java.awt.Graphics2D
import java.awt.image.BufferedImage

object TextureUtils {
    @JvmStatic
    fun combineTexturesVertically(images: Array<BufferedImage>): BufferedImage {
        check(images.isNotEmpty())

        val firstImage = images[0]
        val height = firstImage.height
        val totalHeight = MathUtils.ceilToPOT(height * images.size)
        val finalImage = BufferedImage(firstImage.width, totalHeight, firstImage.type)
        val graphics = finalImage.createGraphics()

        for (i in images.indices) {
            val src = images[i]
            graphics.drawImage(src, 0, height * i, null)
        }

        graphics.dispose()

        return finalImage
    }

    @JvmStatic
    fun combineColoredTextures(images: Array<BufferedImage>): BufferedImage {
        check(images.size == 16)

        val firstImage = images[0]
        val size = firstImage.width
        val finalImage = BufferedImage(size * 4, size * 4, firstImage.type)
        val graphics = finalImage.createGraphics()

        for (x in 0 until 4) {
            for (y in 0 until 4) {
                val src = images[x + y * 4]
                graphics.drawImage(src, x * size, y * size, null)
            }
        }

        graphics.dispose()

        return finalImage
    }

    @JvmStatic
    fun combineColoredWithUncoloredTextures(images: Array<BufferedImage>): BufferedImage {
        check(images.size == 17)

        val firstImage = images[0]
        val size = firstImage.width
        val finalImage = BufferedImage(size * 4, MathUtils.ceilToPOT(size * 5), firstImage.type)
        val graphics = finalImage.createGraphics()

        for (x in 0 until 4) {
            for (y in 0 until 4) {
                val src = images[x + y * 4]
                graphics.drawImage(src, x * size, y * size, null)
            }
        }

        graphics.drawImage(images[16], 0, 4 * size, null)

        graphics.dispose()

        return finalImage
    }

    fun combineTextureSides(output: Graphics2D, images: Array<BufferedImage>, x: Int, y: Int) {
        check(images.size == 6)

        val sizeX = images[0].width
        val sizeY = images[2].height
        val sizeZ = images[0].height

        // Up
        output.drawQuadImage(
            images[0],
            x + sizeZ,
            y + 0,
            x + sizeZ + sizeX,
            y + sizeZ
        )
        // Down
        output.drawQuadImage(
            images[1],
            x + sizeZ + sizeX,
            y + 0,
            x + sizeZ + sizeX + sizeX,
            y + sizeZ
        )
        // West
        output.drawQuadImage(
            images[2],
            x + 0,
            y + sizeZ,
            x + sizeZ,
            y + sizeZ + sizeY
        )
        // South
        output.drawQuadImage(
            images[3],
            x + sizeZ,
            y + sizeZ,
            x + sizeZ + sizeX,
            y + sizeZ + sizeY
        )
        // East
        output.drawQuadImage(
            images[4],
            x + sizeZ + sizeX,
            y + sizeZ,
            x + sizeZ + sizeX + sizeZ,
            y + sizeZ + sizeY
        )
        // North
        output.drawQuadImage(
            images[5],
            x + sizeZ + sizeX + sizeZ,
            y + sizeZ,
            x + sizeZ + sizeX + sizeZ + sizeX,
            y + sizeZ + sizeY
        )
    }

    private fun Graphics2D.drawQuadImage(image: BufferedImage, x1: Int, y1: Int, x2: Int, y2: Int) {
        drawImage(image, x1, y1, x2, y2, 0, 0, image.width, image.height, null)
    }

    fun BufferedImage.emptyCopy(): BufferedImage {
        return BufferedImage(this.width, this.height, this.type)
    }
}