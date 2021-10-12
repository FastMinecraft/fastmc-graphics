package me.xiaro.fastmc.utils

import java.awt.image.BufferedImage

object TextureUtils {
    @JvmStatic
    fun combineTexturesVertically(images: Array<BufferedImage>): BufferedImage {
        assert(images.isNotEmpty())

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
        assert(images.size == 16)

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
}