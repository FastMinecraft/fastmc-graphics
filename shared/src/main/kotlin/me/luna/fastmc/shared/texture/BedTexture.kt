package me.luna.fastmc.shared.texture

import me.luna.fastmc.shared.texture.TextureUtils.emptyCopy
import java.awt.image.BufferedImage

object BedTexture {
    fun vAll(input: BufferedImage): BufferedImage {
        val scale = input.height / 64
        val output = input.emptyCopy()
        val graphics = output.createGraphics()

        var transformer = TextureTransformer(0, 0, 16, 16, 6, scale)
        transformer.rotateX(1)
        transformer.execute(input, graphics, 0, 0)

        transformer = TextureTransformer(0, 22, 16, 16, 6, scale)
        transformer.rotateX(1)
        transformer.execute(input, graphics, 0, 22)

        transformer = TextureTransformer(50, 6, 3, 3, 3, scale)
        transformer.rotateY(1)
        transformer.execute(input, graphics, 0, 44)

        transformer = TextureTransformer(50, 18, 3, 3, 3, scale)
        transformer.rotateY(2)
        transformer.execute(input, graphics, 0, 50)

        transformer = TextureTransformer(50, 0, 3, 3, 3, scale)
        transformer.rotateY(0)
        transformer.execute(input, graphics, 12, 44)

        transformer = TextureTransformer(50, 12, 3, 3, 3, scale)
        transformer.rotateY(3)
        transformer.execute(input, graphics, 12, 50)

        graphics.dispose()
        return output
    }
}