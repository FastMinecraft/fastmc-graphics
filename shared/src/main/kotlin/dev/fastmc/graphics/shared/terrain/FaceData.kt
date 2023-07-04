package dev.fastmc.graphics.shared.terrain

import it.unimi.dsi.fastutil.ints.IntArrayList

sealed interface FaceData {
    fun addToBatch(
        batch: RenderRegion.LayerBatch,
        vertexRegionOffset: Int,
        indexRegionOffset: Int,
        visibleFaceBit: Int,
        chunkOffsetData: Int
    )

    class Singleton(private val indexLength: Int) : FaceData {
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
                indexLength,
                chunkOffsetData
            )
        }
    }

    class Multiple(private val dataArray: IntArray) : FaceData {
        private val visibleIndices: IntArray
        private val allFaceBits: Int

        init {
            var bits = 0
            val visibleIndexList = IntArrayList()

            for (i in 0 until MAX_COUNT) {
                if (dataArray[i * 3] != -1) {
                    visibleIndexList.add(i)
                    bits = bits or (i + 1)
                }
            }

            visibleIndexList.trim()
            visibleIndices = visibleIndexList.elements()
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

            for (i in visibleIndices.indices) {
                val dataIndex = visibleIndices[i]
                val index = dataIndex * 3
                if ((dataIndex + 1) and visibleFaceBit == 0) continue

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