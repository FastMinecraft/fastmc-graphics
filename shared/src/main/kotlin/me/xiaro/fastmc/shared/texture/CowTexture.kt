package me.xiaro.fastmc.shared.texture

import java.awt.image.BufferedImage

object CowTexture {
    fun v112(input: BufferedImage): BufferedImage {
        val scale = input.width / 64
        val output = BufferedImage(64 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB)
        val graphics = output.createGraphics()

        var transformer = TextureTransformer(0, 0, 8, 8, 6, scale)
        transformer.execute(input, graphics, 0, 0)

        transformer = TextureTransformer(22, 0, 1, 3, 1, scale)
        transformer.execute(input, graphics, 0, 0)

        transformer = TextureTransformer(0, 16, 4, 12, 4, scale)
        transformer.execute(input, graphics, 28, 0)

        transformer = TextureTransformer(18, 4, 12, 18, 10, scale)
        transformer.rotateX(-1)
        transformer.execute(input, graphics, 0, 16)

        transformer = TextureTransformer(52, 0, 4, 6, 1, scale)
        transformer.rotateX(-1)
        transformer.execute(input, graphics, 0, 44)

        graphics.dispose()
        return output
    }
}