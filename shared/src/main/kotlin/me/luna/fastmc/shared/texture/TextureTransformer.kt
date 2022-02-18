package me.luna.fastmc.shared.texture

import me.luna.fastmc.shared.util.collection.mapArray
import me.luna.fastmc.shared.util.fastCeil
import me.luna.fastmc.shared.util.toRadian
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.sin

class TextureTransformer(
    offsetX: Int,
    offsetY: Int,
    sizeX: Int,
    sizeY: Int,
    sizeZ: Int,
    scale: Int,
) {
    private val inputQuads: Array<Quad> = arrayOf(
        Quad(offsetX, offsetY, sizeZ, 0, sizeZ + sizeX, sizeZ, scale), // Up
        Quad(offsetX, offsetY, sizeZ + sizeX, 0, sizeZ + sizeX + sizeX, sizeZ, scale), // Down
        Quad(offsetX, offsetY, 0, sizeZ, sizeZ, sizeZ + sizeY, scale), // West
        Quad(offsetX, offsetY, sizeZ, sizeZ, sizeZ + sizeX, sizeZ + sizeY, scale), // South
        Quad(offsetX, offsetY, sizeZ + sizeX, sizeZ, sizeZ + sizeX + sizeZ, sizeZ + sizeY, scale), // East
        Quad(
            offsetX,
            offsetY,
            sizeZ + sizeX + sizeZ,
            sizeZ,
            sizeZ + sizeX + sizeZ + sizeX,
            sizeZ + sizeY,
            scale
        ), // North
    )

    val outputQuads: Array<Quad> = inputQuads.copyOf()

    fun rotateX(count: Int) {
        val newCount = Math.floorMod(count, 4)

        for (i in 0 until newCount) {
            outputQuads[2].rotate(-90.0)
            outputQuads[4].rotate(90.0)

            val q0 = outputQuads[3]
            val q1 = outputQuads[5]
            val q3 = outputQuads[1]
            val q5 = outputQuads[0]

            outputQuads[0] = q0
            outputQuads[1] = q1
            outputQuads[3] = q3
            outputQuads[5] = q5

            q3.rotate(180.0)
            q5.rotate(180.0)
        }
    }

    fun rotateY(count: Int) {
        val newCount = Math.floorMod(count, 4)

        for (i in 0 until newCount) {
            outputQuads[0].rotate(90.0)
            outputQuads[1].rotate(-90.0)

            val q2 = outputQuads[3]
            val q3 = outputQuads[4]
            val q4 = outputQuads[5]
            val q5 = outputQuads[2]

            outputQuads[2] = q2
            outputQuads[3] = q3
            outputQuads[4] = q4
            outputQuads[5] = q5
        }
    }

    fun rotateZ(count: Int) {
        val newCount = Math.floorMod(count, 4)

        for (i in 0 until newCount) {
            outputQuads[3].rotate(90.0)
            outputQuads[5].rotate(-90.0)

            val q0 = outputQuads[2]
            val q1 = outputQuads[4]
            val q2 = outputQuads[1]
            val q4 = outputQuads[0]

            outputQuads[0] = q0
            outputQuads[1] = q1
            outputQuads[2] = q2
            outputQuads[4] = q4

            q0.rotate(90.0)
            q1.rotate(90.0)
            q2.rotate(90.0)
            q4.rotate(90.0)
        }
    }

    fun execute(input: BufferedImage): BufferedImage {
        inputQuads.forEach {
            it.transform(input)
        }

        val sizeX = outputQuads[0].image.width
        val sizeY = outputQuads[2].image.height
        val sizeZ = outputQuads[0].image.height

        val width = sizeZ + sizeX + sizeZ + sizeX
        val height = sizeZ + sizeY

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        TextureUtils.combineTextureSides(graphics, outputQuads.mapArray { it.image }, 0, 0)

        graphics.dispose()
        return image
    }

    fun execute(input: BufferedImage, output: Graphics2D, x: Int, y: Int) {
        inputQuads.forEach {
            it.transform(input)
        }

        TextureUtils.combineTextureSides(output, outputQuads.mapArray { it.image }, x, y)
    }

    private fun Graphics2D.drawQuadImage(index: Int, x1: Int, y1: Int, x2: Int, y2: Int) {
        val image = outputQuads[index].image
        drawImage(image, x1, y1, x2, y2, 0, 0, image.width, image.height, null)
    }

    inner class Quad(
        offsetX: Int,
        offsetY: Int,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        scale: Int
    ) {
        val x1 = (x1 + offsetX) * scale
        val y1 = (y1 + offsetY) * scale
        val x2 = (x2 + offsetX) * scale
        val y2 = (y2 + offsetY) * scale

        val width: Int
            get() = x2 - x1
        val height: Int
            get() = y2 - y1

        val imageTransform = AffineTransform()
        private var rotation = 0.0

        lateinit var image: BufferedImage; private set

        fun transform(input: BufferedImage) {
            val tempImage = BufferedImage(width, height, input.type)
            val tempGraphics = tempImage.createGraphics()
            tempGraphics.drawImage(input, 0, 0, width, height, x1, y1, x2, y2, null)
            tempGraphics.dispose()

            val bounds = imageTransform.getBounds()
            val image = BufferedImage(bounds.width.fastCeil(), bounds.height.fastCeil(), input.type)
            val graphics = image.createGraphics()
            graphics.drawImage(tempImage, imageTransform, null)
            graphics.dispose()

            this.image = image
        }

        fun rotate(degree: Double) {
            val radian = degree.toRadian()
            val tempTransform = AffineTransform(imageTransform)
            tempTransform.rotate(radian)
            val tempBounds = tempTransform.getBounds()

            val rotRadian = rotation.toRadian()
            val sin = sin(rotRadian)
            val cos = cos(rotRadian)

            val x = -tempBounds.minX * cos - tempBounds.minY * sin
            val y = -tempBounds.minY * cos + tempBounds.minX * sin

            imageTransform.translate(x, y)
            imageTransform.rotate(radian)

            rotation += degree
        }

        fun scale(x: Double, y: Double) {
            val bounds = imageTransform.getBounds()
            val w = bounds.width / 2.0
            val h = bounds.height / 2.0

            imageTransform.translate(w, h)
            imageTransform.scale(x, y)
            imageTransform.translate(-w, -h)
        }

        private fun AffineTransform.getBounds(): Rectangle2D {
            return createTransformedShape(Rectangle(width, height)).bounds2D
        }
    }
}