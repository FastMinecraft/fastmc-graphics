package dev.fastmc.graphics.shared.texture

import dev.fastmc.graphics.shared.texture.TextureUtils.emptyCopy
import java.awt.image.BufferedImage

object ChestTexture {
    fun v115Small(input: BufferedImage): BufferedImage {
        val scale = input.height / 64
        val output = input.emptyCopy()
        val graphics = output.createGraphics()

        fun transform(transformer: TextureTransformer) {
            transformer.rotateX(2)
            transformer.outputQuads[0].scale(-1.0, 1.0)
            transformer.outputQuads[1].scale(-1.0, 1.0)
        }

        var transformer = TextureTransformer(0, 19, 14, 10, 14, scale)
        transform(transformer)
        transformer.execute(input, graphics, 0, 19)

        transformer = TextureTransformer(0, 0, 2, 4, 1, scale)
        transform(transformer)
        transformer.execute(input, graphics, 0, 0)

        transformer = TextureTransformer(0, 0, 14, 5, 14, scale)
        transform(transformer)
        transformer.execute(input, graphics, 0, 0)

        return output
    }

    fun v115Large(left: BufferedImage, right: BufferedImage): BufferedImage {
        val scale = left.height / 64

        fun Int.scaled(): Int {
            return this * scale
        }

        fun transformSide(input: BufferedImage): BufferedImage {
            val output = input.emptyCopy()
            val graphics = output.createGraphics()

            fun TextureTransformer.transform() {
                rotateX(2)
                outputQuads[0].scale(-1.0, 1.0)
                outputQuads[1].scale(-1.0, 1.0)
            }

            var transformer = TextureTransformer(0, 19, 15, 10, 14, scale)
            transformer.transform()
            transformer.execute(input, graphics, 0, 19)

            transformer = TextureTransformer(0, 0, 1, 4, 1, scale)
            transformer.transform()
            transformer.execute(input, graphics, 0, 0)

            transformer = TextureTransformer(0, 0, 15, 5, 14, scale)
            transformer.transform()
            transformer.execute(input, graphics, 0, 0)

            return output
        }

        val left1 = transformSide(left)
        val right1 = transformSide(right)

        val output = BufferedImage(left.width * 2, left1.height, left1.type)
        val graphics = output.createGraphics()

        fun transformPart(offsetX: Int, offsetY: Int, sizeX: Int, sizeY: Int, sizeZ: Int) {
            val sizeX2 = sizeX * 2

            fun drawImage(src: BufferedImage, dx: Int, dy: Int, sx: Int, sy: Int, width: Int, height: Int) {
                graphics.drawImage(
                    src,
                    (offsetX + dx).scaled(),
                    (offsetY + dy).scaled(),
                    (offsetX + dx + width).scaled(),
                    (offsetY + dy + height).scaled(),
                    (offsetX + sx).scaled(),
                    (offsetY + sy).scaled(),
                    (offsetX + sx + width).scaled(),
                    (offsetY + sy + height).scaled(),
                    null
                )
            }

            fun combineSides(dx: Int, dy: Int, sx: Int, sy: Int, width: Int, height: Int, reversed: Boolean) {
                drawImage(
                    if (reversed) right1 else left1,
                    dx,
                    dy,
                    sx,
                    sy,
                    width,
                    height
                )

                drawImage(
                    if (reversed) left1 else right1,
                    dx + width,
                    dy,
                    sx,
                    sy,
                    width,
                    height,
                )
            }

            combineSides(sizeZ, 0, sizeZ, 0, sizeX, sizeZ, true)
            combineSides(sizeZ + sizeX2, 0, sizeZ + sizeX, 0, sizeX, sizeZ, true)
            drawImage(right1, 0, sizeZ, 0, sizeZ, sizeZ, sizeY)
            combineSides(sizeZ, sizeZ, sizeZ, sizeZ, sizeX, sizeY, true)
            drawImage(left1, sizeZ + sizeX2, sizeZ, sizeZ + sizeX, sizeZ, sizeZ, sizeY)
            combineSides(sizeZ + sizeX2 + sizeZ, sizeZ, sizeZ + sizeX + sizeZ, sizeZ, sizeX, sizeY, false)
        }

        val lol = true

        if (lol) {
            transformPart(0, 19, 15, 10, 14)
            transformPart(0, 0, 1, 4, 1)
            transformPart(0, 0, 15, 5, 14)
        } else {
            graphics.drawImage(left1, 0, 0, null)
            graphics.drawImage(right1, left.width, 0, null)
        }

        graphics.dispose()
        return output
    }
}