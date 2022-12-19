package dev.fastmc.graphics.shared.terrain

sealed interface FaceData {
    fun addToBatch(
        batch: RenderRegion.LayerBatch,
        vertexRegionOffset: Int,
        indexRegionOffset: Int,
        visibleFaceBit: Int,
        chunkOffsetData: Int
    )

    class Singleton(indexBufferLength: Int) : FaceData {
        private val indexCount = indexBufferLength

        override fun addToBatch(
            batch: RenderRegion.LayerBatch,
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
    }

    class Multiple(private val dataArray: IntArray) : FaceData {
        private val allFaceBits: Int

        init {
            var bits = 0

            for (i in 0 until MAX_COUNT) {
                if (dataArray[i * 3] != -1) {
                    bits = bits or (i + 1)
                }
            }

            allFaceBits = bits
        }

        override fun addToBatch(
            batch: RenderRegion.LayerBatch,
            vertexRegionOffset: Int,
            indexRegionOffset: Int,
            visibleFaceBit: Int,
            chunkOffsetData: Int
        ) {
            if (visibleFaceBit and allFaceBits == 0) return

            for (i in 0 until MAX_COUNT) {
                val index = i * 3
                if (dataArray[index] == -1) continue
                if ((i + 1) and visibleFaceBit == 0) continue

                batch.put(
                    vertexRegionOffset + dataArray[index],
                    indexRegionOffset + dataArray[index + 1],
                    dataArray[index + 2],
                    chunkOffsetData
                )
            }
        }
    }

    companion object {
        const val MAX_COUNT = 0b11_11_11
    }
}