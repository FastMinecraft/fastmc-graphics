package me.luna.fastmc.shared.model

import me.luna.fastmc.shared.util.BufferUtils
import java.nio.ByteBuffer

open class ModelBuilder(val id: Int, val textureSizeX: Int, val textureSizeY: Int) {
    open var idCounter = 0

    private val childModels = ArrayList<ChildModelBuilder>()

    var vertexSize = 0; protected set

    inline fun childModel(block: ChildModelBuilder.() -> Unit) {
        childModel(ChildModelBuilder(this).apply(block))
    }

    fun childModel(childModelBuilder: ChildModelBuilder) {
        childModels.add(childModelBuilder)
        vertexSize += childModelBuilder.vertexSize
    }

    fun build(): ByteBuffer {
        val buffer = BufferUtils.byte(vertexSize * 20)
        build(buffer)
        buffer.flip()
        return buffer
    }

    open fun build(vboBuffer: ByteBuffer) {
        childModels.forEach {
            it.build(vboBuffer)
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
class ChildModelBuilder(parent: ModelBuilder) :
    ModelBuilder(parent.idCounter++, parent.textureSizeX, parent.textureSizeY) {
    override var idCounter: Int by parent::idCounter

    private val boxList = ArrayList<Box>()

    fun addBox(
        textureOffsetX: Float,
        textureOffsetY: Float,
        offsetX: Float,
        offsetY: Float,
        offsetZ: Float,
        sizeX: Float,
        sizeY: Float,
        sizeZ: Float
    ) {
        boxList.add(Box(textureOffsetX, textureOffsetY, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ))
        vertexSize += 36
    }

    fun textureOffset(textureOffsetX: Float, textureOffsetY: Float, block: TextureOffsetGroup.() -> Unit) {
        TextureOffsetGroup(textureOffsetX, textureOffsetY).apply(block)
    }

    override fun build(vboBuffer: ByteBuffer) {
        super.build(vboBuffer)
        boxList.forEach {
            it.putDown(vboBuffer)
            it.putUp(vboBuffer)
            it.putWest(vboBuffer)
            it.putSouth(vboBuffer)
            it.putEast(vboBuffer)
            it.putNorth(vboBuffer)
        }
    }

    inner class TextureOffsetGroup(private val textureOffsetX: Float, private val textureOffsetY: Float) {
        fun addBox(
            offsetX: Float,
            offsetY: Float,
            offsetZ: Float,
            sizeX: Float,
            sizeY: Float,
            sizeZ: Float
        ) {
            this@ChildModelBuilder.addBox(
                textureOffsetX,
                textureOffsetY,
                offsetX,
                offsetY,
                offsetZ,
                sizeX,
                sizeY,
                sizeZ
            )
        }
    }

    private inner class Box(
        private val textureOffsetX: Float,
        private val textureOffsetY: Float,
        offsetX: Float,
        offsetY: Float,
        offsetZ: Float,
        val sizeX: Float,
        val sizeY: Float,
        val sizeZ: Float
    ) {
        val minX = offsetX
        val minY = offsetY
        val minZ = offsetZ
        val maxX = offsetX + sizeX
        val maxY = offsetY + sizeY
        val maxZ = offsetZ + sizeZ

        fun putDown(vboBuffer: ByteBuffer) {
            vboBuffer.putPos(minX, minY, minZ)
            vboBuffer.putUV(sizeZ + sizeX, 0.0f)
            vboBuffer.putNormal(0, -1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeX, sizeZ)
            vboBuffer.putNormal(0, -1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, minY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX, sizeZ)
            vboBuffer.putNormal(0, -1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, minY, minZ)
            vboBuffer.putUV(sizeZ + sizeX, 0.0f)
            vboBuffer.putNormal(0, -1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeX, 0.0f)
            vboBuffer.putNormal(0, -1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeX, sizeZ)
            vboBuffer.putNormal(0, -1, 0)
            vboBuffer.putID()
        }

        fun putUp(vboBuffer: ByteBuffer) {
            vboBuffer.putPos(minX, maxY, maxZ)
            vboBuffer.putUV(sizeZ, sizeZ)
            vboBuffer.putNormal(0, 1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, maxY, minZ)
            vboBuffer.putUV(sizeZ + sizeX, 0.0f)
            vboBuffer.putNormal(0, 1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, maxY, minZ)
            vboBuffer.putUV(sizeZ, 0.0f)
            vboBuffer.putNormal(0, 1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, maxY, maxZ)
            vboBuffer.putUV(sizeZ, sizeZ)
            vboBuffer.putNormal(0, 1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, maxY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX, sizeZ)
            vboBuffer.putNormal(0, 1, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, maxY, minZ)
            vboBuffer.putUV(sizeZ + sizeX, 0.0f)
            vboBuffer.putNormal(0, 1, 0)
            vboBuffer.putID()
        }

        fun putWest(vboBuffer: ByteBuffer) {
            vboBuffer.putPos(minX, maxY, maxZ)
            vboBuffer.putUV(sizeZ, sizeZ)
            vboBuffer.putNormal(-1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, minY, minZ)
            vboBuffer.putUV(0.0f, sizeZ + sizeY)
            vboBuffer.putNormal(-1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, minY, maxZ)
            vboBuffer.putUV(sizeZ, sizeZ + sizeY)
            vboBuffer.putNormal(-1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, maxY, maxZ)
            vboBuffer.putUV(sizeZ, sizeZ)
            vboBuffer.putNormal(-1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, maxY, minZ)
            vboBuffer.putUV(0.0f, sizeZ)
            vboBuffer.putNormal(-1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(minX, minY, minZ)
            vboBuffer.putUV(0.0f, sizeZ + sizeY)
            vboBuffer.putNormal(-1, 0, 0)
            vboBuffer.putID()
        }

        fun putSouth(vboBuffer: ByteBuffer) {
            vboBuffer.putPos(maxX, maxY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX, sizeZ)
            vboBuffer.putNormal(0, 0, 1)
            vboBuffer.putID()

            vboBuffer.putPos(minX, minY, maxZ)
            vboBuffer.putUV(sizeZ, sizeZ + sizeY)
            vboBuffer.putNormal(0, 0, 1)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX, sizeZ + sizeY)
            vboBuffer.putNormal(0, 0, 1)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, maxY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX, sizeZ)
            vboBuffer.putNormal(0, 0, 1)
            vboBuffer.putID()

            vboBuffer.putPos(minX, maxY, maxZ)
            vboBuffer.putUV(sizeZ, sizeZ)
            vboBuffer.putNormal(0, 0, 1)
            vboBuffer.putID()

            vboBuffer.putPos(minX, minY, maxZ)
            vboBuffer.putUV(sizeZ, sizeZ + sizeY)
            vboBuffer.putNormal(0, 0, 1)
            vboBuffer.putID()
        }

        fun putEast(vboBuffer: ByteBuffer) {
            vboBuffer.putPos(maxX, maxY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ, sizeZ)
            vboBuffer.putNormal(1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX, sizeZ + sizeY)
            vboBuffer.putNormal(1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ, sizeZ + sizeY)
            vboBuffer.putNormal(1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, maxY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ, sizeZ)
            vboBuffer.putNormal(1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, maxY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX, sizeZ)
            vboBuffer.putNormal(1, 0, 0)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, maxZ)
            vboBuffer.putUV(sizeZ + sizeX, sizeZ + sizeY)
            vboBuffer.putNormal(1, 0, 0)
            vboBuffer.putID()
        }

        fun putNorth(vboBuffer: ByteBuffer) {
            vboBuffer.putPos(minX, maxY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ + sizeX, sizeZ)
            vboBuffer.putNormal(0, 0, -1)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ, sizeZ + sizeY)
            vboBuffer.putNormal(0, 0, -1)
            vboBuffer.putID()

            vboBuffer.putPos(minX, minY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ + sizeX, sizeZ + sizeY)
            vboBuffer.putNormal(0, 0, -1)
            vboBuffer.putID()

            vboBuffer.putPos(minX, maxY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ + sizeX, sizeZ)
            vboBuffer.putNormal(0, 0, -1)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, maxY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ, sizeZ)
            vboBuffer.putNormal(0, 0, -1)
            vboBuffer.putID()

            vboBuffer.putPos(maxX, minY, minZ)
            vboBuffer.putUV(sizeZ + sizeX + sizeZ, sizeZ + sizeY)
            vboBuffer.putNormal(0, 0, -1)
            vboBuffer.putID()
        }

        private inline fun ByteBuffer.putPos(x: Float, y: Float, z: Float) {
            this.putFloat(x / 16.0f)
            this.putFloat(y / 16.0f)
            this.putFloat(z / 16.0f)
        }

        private inline fun ByteBuffer.putUV(u: Float, v: Float) {
            this.putShort(((u + textureOffsetX) / textureSizeX * 65535.0f).toInt().toShort())
            this.putShort(((v + textureOffsetY) / textureSizeY * 65535.0f).toInt().toShort())
        }

        private inline fun ByteBuffer.putNormal(x: Byte, y: Byte, z: Byte) {
            this.put(x)
            this.put(y)
            this.put(z)
        }

        private inline fun ByteBuffer.putID() {
            this.put(id.toByte())
        }
    }
}