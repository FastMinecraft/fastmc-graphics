package dev.fastmc.graphics.shared.model

import dev.luna5ama.kmogus.*

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

    fun build(memoryStack: MemoryStack): Arr {
        val buffer = memoryStack.calloc(vertexSize * 20L).asMutable()
        build(buffer)
        buffer.flip()
        return buffer
    }

    open fun build(buffer: MutableArr) {
        childModels.forEach {
            it.build(buffer)
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

    override fun build(buffer: MutableArr) {
        super.build(buffer)
        boxList.forEach {
            it.putDown(buffer)
            it.putUp(buffer)
            it.putWest(buffer)
            it.putSouth(buffer)
            it.putEast(buffer)
            it.putNorth(buffer)
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

        fun putDown(vboBuffer: MutableArr) {
            vboBuffer.usePtr {
                putPos(minX, minY, minZ)
                    .putUV(sizeZ + sizeX, 0.0f)
                    .putNormal(0, -1, 0)
                    .putID()

                    .putPos(maxX, minY, maxZ)
                    .putUV(sizeZ + sizeX + sizeX, sizeZ)
                    .putNormal(0, -1, 0)
                    .putID()

                    .putPos(minX, minY, maxZ)
                    .putUV(sizeZ + sizeX, sizeZ)
                    .putNormal(0, -1, 0)
                    .putID()

                    .putPos(minX, minY, minZ)
                    .putUV(sizeZ + sizeX, 0.0f)
                    .putNormal(0, -1, 0)
                    .putID()

                    .putPos(maxX, minY, minZ)
                    .putUV(sizeZ + sizeX + sizeX, 0.0f)
                    .putNormal(0, -1, 0)
                    .putID()

                    .putPos(maxX, minY, maxZ)
                    .putUV(sizeZ + sizeX + sizeX, sizeZ)
                    .putNormal(0, -1, 0)
                    .putID()
            }
        }

        fun putUp(vboBuffer: MutableArr) {
            vboBuffer.usePtr {
                putPos(minX, maxY, maxZ)
                    .putUV(sizeZ, sizeZ)
                    .putNormal(0, 1, 0)
                    .putID()

                    .putPos(maxX, maxY, minZ)
                    .putUV(sizeZ + sizeX, 0.0f)
                    .putNormal(0, 1, 0)
                    .putID()

                    .putPos(minX, maxY, minZ)
                    .putUV(sizeZ, 0.0f)
                    .putNormal(0, 1, 0)
                    .putID()

                    .putPos(minX, maxY, maxZ)
                    .putUV(sizeZ, sizeZ)
                    .putNormal(0, 1, 0)
                    .putID()

                    .putPos(maxX, maxY, maxZ)
                    .putUV(sizeZ + sizeX, sizeZ)
                    .putNormal(0, 1, 0)
                    .putID()

                    .putPos(maxX, maxY, minZ)
                    .putUV(sizeZ + sizeX, 0.0f)
                    .putNormal(0, 1, 0)
                    .putID()
            }
        }

        fun putWest(vboBuffer: MutableArr) {
            vboBuffer.usePtr {
                putPos(minX, maxY, maxZ)
                    .putUV(sizeZ, sizeZ)
                    .putNormal(-1, 0, 0)
                    .putID()

                    .putPos(minX, minY, minZ)
                    .putUV(0.0f, sizeZ + sizeY)
                    .putNormal(-1, 0, 0)
                    .putID()

                    .putPos(minX, minY, maxZ)
                    .putUV(sizeZ, sizeZ + sizeY)
                    .putNormal(-1, 0, 0)
                    .putID()

                    .putPos(minX, maxY, maxZ)
                    .putUV(sizeZ, sizeZ)
                    .putNormal(-1, 0, 0)
                    .putID()

                    .putPos(minX, maxY, minZ)
                    .putUV(0.0f, sizeZ)
                    .putNormal(-1, 0, 0)
                    .putID()

                    .putPos(minX, minY, minZ)
                    .putUV(0.0f, sizeZ + sizeY)
                    .putNormal(-1, 0, 0)
                    .putID()
            }
        }

        fun putSouth(vboBuffer: MutableArr) {
            vboBuffer.usePtr {
                putPos(maxX, maxY, maxZ)
                    .putUV(sizeZ + sizeX, sizeZ)
                    .putNormal(0, 0, 1)
                    .putID()

                    .putPos(minX, minY, maxZ)
                    .putUV(sizeZ, sizeZ + sizeY)
                    .putNormal(0, 0, 1)
                    .putID()

                    .putPos(maxX, minY, maxZ)
                    .putUV(sizeZ + sizeX, sizeZ + sizeY)
                    .putNormal(0, 0, 1)
                    .putID()

                    .putPos(maxX, maxY, maxZ)
                    .putUV(sizeZ + sizeX, sizeZ)
                    .putNormal(0, 0, 1)
                    .putID()

                    .putPos(minX, maxY, maxZ)
                    .putUV(sizeZ, sizeZ)
                    .putNormal(0, 0, 1)
                    .putID()

                    .putPos(minX, minY, maxZ)
                    .putUV(sizeZ, sizeZ + sizeY)
                    .putNormal(0, 0, 1)
                    .putID()
            }
        }

        fun putEast(vboBuffer: MutableArr) {
            vboBuffer.usePtr {
                putPos(maxX, maxY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ, sizeZ)
                    .putNormal(1, 0, 0)
                    .putID()

                    .putPos(maxX, minY, maxZ)
                    .putUV(sizeZ + sizeX, sizeZ + sizeY)
                    .putNormal(1, 0, 0)
                    .putID()

                    .putPos(maxX, minY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ, sizeZ + sizeY)
                    .putNormal(1, 0, 0)
                    .putID()

                    .putPos(maxX, maxY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ, sizeZ)
                    .putNormal(1, 0, 0)
                    .putID()

                    .putPos(maxX, maxY, maxZ)
                    .putUV(sizeZ + sizeX, sizeZ)
                    .putNormal(1, 0, 0)
                    .putID()

                    .putPos(maxX, minY, maxZ)
                    .putUV(sizeZ + sizeX, sizeZ + sizeY)
                    .putNormal(1, 0, 0)
                    .putID()
            }
        }

        fun putNorth(vboBuffer: MutableArr) {
            vboBuffer.usePtr {
                putPos(minX, maxY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ + sizeX, sizeZ)
                    .putNormal(0, 0, -1)
                    .putID()

                    .putPos(maxX, minY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ, sizeZ + sizeY)
                    .putNormal(0, 0, -1)
                    .putID()

                    .putPos(minX, minY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ + sizeX, sizeZ + sizeY)
                    .putNormal(0, 0, -1)
                    .putID()

                    .putPos(minX, maxY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ + sizeX, sizeZ)
                    .putNormal(0, 0, -1)
                    .putID()

                    .putPos(maxX, maxY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ, sizeZ)
                    .putNormal(0, 0, -1)
                    .putID()

                    .putPos(maxX, minY, minZ)
                    .putUV(sizeZ + sizeX + sizeZ, sizeZ + sizeY)
                    .putNormal(0, 0, -1)
                    .putID()
            }
        }

        private inline fun Ptr.putPos(x: Float, y: Float, z: Float): Ptr {
            return setFloatInc(x / 16.0f)
                .setFloatInc(y / 16.0f)
                .setFloatInc(z / 16.0f)
        }

        private inline fun Ptr.putUV(u: Float, v: Float): Ptr {
            return setShortInc(((u + textureOffsetX) / textureSizeX * 65535.0f).toInt().toShort())
                .setShortInc(((v + textureOffsetY) / textureSizeY * 65535.0f).toInt().toShort())
        }

        private inline fun Ptr.putNormal(x: Byte, y: Byte, z: Byte): Ptr {
            return setByteInc(x)
                .setByteInc(y)
                .setByteInc(z)
        }

        private inline fun Ptr.putID(): Ptr {
            return setByteInc(id.toByte())
        }
    }
}