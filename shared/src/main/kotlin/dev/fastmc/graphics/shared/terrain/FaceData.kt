package dev.fastmc.graphics.shared.terrain

import java.nio.IntBuffer

sealed interface FaceData {
    val dataSize: Int

    fun addToBatch(
        batch: RenderRegion.DefaultLayerBatch,
        vertexRegionOffset: Int,
        indexRegionOffset: Int,
        visibleFaceBit: Int,
        chunkOffsetData: Int
    )

    fun addToBuffer(
        buffer: IntBuffer,
        vertexRegionOffset: Int,
        indexRegionOffset: Int
    )

    class Singleton(indexBufferLength: Int) : FaceData {
        override val dataSize: Int
            get() = 1
        private val indexCount = indexBufferLength

        override fun addToBatch(
            batch: RenderRegion.DefaultLayerBatch,
            vertexRegionOffset: Int,
            indexRegionOffset: Int,
            visibleFaceBit: Int,
            chunkOffsetData: Int
        ) {
            batch.put(
                vertexRegionOffset / 16,
                indexRegionOffset / 4,
                indexCount,
                chunkOffsetData
            )
        }

        override fun addToBuffer(buffer: IntBuffer, vertexRegionOffset: Int, indexRegionOffset: Int) {
            buffer.put(0b11_11_11)
            buffer.put(vertexRegionOffset / 16)
            buffer.put(indexRegionOffset / 4)
            buffer.put(indexCount)
        }
    }

    class Multiple(private val dataArray: IntArray) : FaceData {
        override val dataSize: Int
            get() = dataArray.size / 4
        private val allFaceBits: Int

        init {
            var bits = 0

            for (i in dataArray.indices step 4) {
                bits = bits or (dataArray[i] + 1)
            }

            allFaceBits = bits
        }

        override fun addToBatch(
            batch: RenderRegion.DefaultLayerBatch,
            vertexRegionOffset: Int,
            indexRegionOffset: Int,
            visibleFaceBit: Int,
            chunkOffsetData: Int
        ) {
            if (visibleFaceBit and allFaceBits == 0) return

            val vertexBaseOffset = vertexRegionOffset / 16
            val indexBaseOffset = indexRegionOffset / 4

            for (i in dataArray.indices step 4) {
                if ((dataArray[i]) and visibleFaceBit == 0) continue

                batch.put(
                    vertexBaseOffset + dataArray[i + 1],
                    indexBaseOffset + dataArray[i + 2],
                    dataArray[i + 3],
                    chunkOffsetData
                )
            }
        }

        override fun addToBuffer(buffer: IntBuffer, vertexRegionOffset: Int, indexRegionOffset: Int) {
            val vertexBaseOffset = vertexRegionOffset / 16
            val indexBaseOffset = indexRegionOffset / 4

            for (i in dataArray.indices step 4) {
                buffer.put(dataArray[i])
                buffer.put(vertexBaseOffset + dataArray[i + 1])
                buffer.put(indexBaseOffset + dataArray[i + 2])
                buffer.put(dataArray[i + 3])
            }
        }
    }

    companion object {
        const val MAX_COUNT = 0b11_11_11
    }
}