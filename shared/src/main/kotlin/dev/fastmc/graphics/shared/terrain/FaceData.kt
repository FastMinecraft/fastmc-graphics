package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.CachedBuffer

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
        buffer: CachedBuffer,
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
                vertexRegionOffset,
                indexRegionOffset,
                indexCount,
                chunkOffsetData
            )
        }

        override fun addToBuffer(buffer: CachedBuffer, vertexRegionOffset: Int, indexRegionOffset: Int) {
            val intBuffer = buffer.ensureRemainingInt(4)
            intBuffer.put(0b11_11_11)
            intBuffer.put(vertexRegionOffset)
            intBuffer.put(indexRegionOffset)
            intBuffer.put(indexCount)
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

            for (i in dataArray.indices step 4) {
                if ((dataArray[i]) and visibleFaceBit == 0) continue

                batch.put(
                    vertexRegionOffset + dataArray[i + 1],
                    indexRegionOffset + dataArray[i + 2],
                    dataArray[i + 3],
                    chunkOffsetData
                )
            }
        }

        override fun addToBuffer(buffer: CachedBuffer, vertexRegionOffset: Int, indexRegionOffset: Int) {
            buffer.ensureRemainingInt(dataArray.size).put(dataArray)
        }
    }

    companion object {
        const val MAX_COUNT = 0b11_11_11
    }
}