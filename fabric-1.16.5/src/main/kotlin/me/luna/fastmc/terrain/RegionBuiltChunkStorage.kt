package me.luna.fastmc.terrain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.luna.fastmc.mixin.IPatchedBuiltChunk
import me.luna.fastmc.mixin.accessor.AccessorWorldRenderer
import me.luna.fastmc.shared.util.*
import me.luna.fastmc.shared.util.collection.ExtendedBitSet
import me.luna.fastmc.util.isDoneOrNull
import net.minecraft.client.render.BuiltChunkStorage
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import java.util.*
import java.util.concurrent.Future
import kotlin.math.max
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
class RegionBuiltChunkStorage(
    chunkBuilder: ChunkBuilder,
    world: World,
    viewDistance: Int,
    worldRenderer: WorldRenderer
) : BuiltChunkStorage(chunkBuilder, world, viewDistance, worldRenderer) {
    val regionSize = (super.sizeX + 31) shr 4
    val regionArray: Array<RenderRegion>

    private val sortingUpdateCounter = UpdateCounter()
    private var lastSortingJob: Future<*>? = null
    private var lastSortedChunkArray: Array<ChunkBuilder.BuiltChunk> = chunks.copyOf()
    var chunkIndices = IntArray(chunks.size); private set

    private val caveCullingUpdateCounter = UpdateCounter()
    private var lastCaveCullingJob: Future<*>? = null
    private var caveCullingDirty = true
    var caveCullingBitSet = ExtendedBitSet(chunks.size); private set

    val sizeX0: Int
        get() = super.sizeX

    val sizeY0: Int
        get() = super.sizeY

    val sizeZ0: Int
        get() = super.sizeZ

    init {
        for (i in chunks.indices) {
            (chunks[i] as IPatchedBuiltChunk).index = i
        }

        @Suppress("UNCHECKED_CAST")
        regionArray = arrayOfNulls<RenderRegion>(regionSize * regionSize) as Array<RenderRegion>

        for (x in 0 until regionSize) {
            for (z in 0 until regionSize) {
                val i = regionPos2Index(x, z)
                val region = RenderRegion(i)
                region.setOrigin(x shl 8, z shl 8)
                region.chunks.ensureCapacity(chunks.size)
                regionArray[i] = region
            }
        }

        for (builtChunk in chunks) {
            builtChunk as IPatchedBuiltChunk
            val region = getRegionByBlock(builtChunk.origin.x, builtChunk.origin.z)
            builtChunk.region = region
            region.chunks.addFast(builtChunk.index)
        }
    }

    override fun updateCameraPosition(x: Double, z: Double) {
        throw UnsupportedOperationException()
    }

    suspend fun updateCameraPosition(cameraChunkOrigin: BlockPos, playerX: Double, playerZ: Double) {
        updateRegions(playerX, playerZ)
        updateChunkIndices(cameraChunkOrigin)
    }

    private suspend fun updateRegions(playerX: Double, playerZ: Double) {
        coroutineScope {
            val floorPlayerX = playerX.fastFloor()
            val floorPlayerZ = playerZ.fastFloor()

            val startChunkX = (floorPlayerX shr 4) - (sizeX0 shr 1)
            val startChunkZ = (floorPlayerZ shr 4) - (sizeZ0 shr 1)
            val endChunkX = startChunkX + sizeX0
            val endChunkZ = startChunkZ + sizeZ0

            val regionCenter = regionSize shr 1
            val originX = (floorPlayerX shr 8) - regionCenter
            val originZ = (floorPlayerZ shr 8) - regionCenter
            val centerX = Math.floorMod(originX, regionSize)
            val centerZ = Math.floorMod(originZ, regionSize)

            for (i in regionArray.indices) {
                launch(FastMcCoreScope.context) {
                    val region = regionArray[i]

                    val blockX = (Math.floorMod((i % regionSize) - centerX, regionSize) + originX) shl 8
                    val blockZ = (Math.floorMod((i / regionSize) - centerZ, regionSize) + originZ) shl 8

                    region.setOrigin(blockX, blockZ)
                    region.chunks.clear()
                    region.chunks.ensureCapacity(chunks.size)

                    val startX = max(blockX shr 4, startChunkX)
                    val startZ = max(blockZ shr 4, startChunkZ)
                    val endX = min((blockX shr 4) + 16, endChunkX)
                    val endZ = min((blockZ shr 4) + 16, endChunkZ)

                    for (x in startX until endX) {
                        for (z in startZ until endZ) {
                            for (y in 0 until 16) {
                                val builtChunk = getRenderedChunk0(x, y, z)
                                builtChunk as IPatchedBuiltChunk
                                builtChunk.setOrigin(x shl 4, y shl 4, z shl 4)
                                region.chunks.addFast(builtChunk.index)
                                builtChunk.region = region
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateChunkIndices(cameraChunkOrigin: BlockPos) {
        lastSortingJob = FastMcExtendScope.pool.submit {
            val chunkArray = lastSortedChunkArray.copyOf()
            val comparator = Comparator.comparingInt<ChunkBuilder.BuiltChunk> {
                val chunkOrigin = it.origin
                -distanceSq(
                    cameraChunkOrigin.x, cameraChunkOrigin.y, cameraChunkOrigin.z,
                    chunkOrigin.x, chunkOrigin.y, chunkOrigin.z
                )
            }
            Arrays.sort(chunkArray, comparator)

            val indexArray = IntArray(chunkArray.size)
            for (i in indexArray.indices) {
                indexArray[i] = (chunkArray[i] as IPatchedBuiltChunk).index
            }

            lastSortedChunkArray = chunkArray
            chunkIndices = indexArray
            sortingUpdateCounter.update()
        }
    }

    fun checkChunkIndicesUpdate(): Boolean {
        return sortingUpdateCounter.check()
    }

    fun markCaveCullingDirty() {
        caveCullingDirty = true
    }

    fun updateCulling(cameraChunkOrigin: BlockPos): Boolean {
        if (caveCullingDirty && lastCaveCullingJob.isDoneOrNull) {
            caveCullingDirty = false
            lastCaveCullingJob = FastMcExtendScope.pool.submit {
                val builtChunk = getRenderedChunkByBlock(cameraChunkOrigin)
                    ?: lastSortedChunkArray[lastSortedChunkArray.size - 1]
                val newCullingBitSet = ExtendedBitSet(chunks.size)
                newCullingBitSet.addFast((builtChunk as IPatchedBuiltChunk).index)

                recursiveUpdateCulling(
                    cameraChunkOrigin,
                    newCullingBitSet,
                    builtChunk,
                    null,
                    0
                )

                caveCullingUpdateCounter.update()
                caveCullingBitSet = newCullingBitSet
            }
        }

        return caveCullingUpdateCounter.check()
    }

    private fun recursiveUpdateCulling(
        cameraChunkBlockPos: BlockPos,
        cullingBitSet: ExtendedBitSet,
        builtChunk: BuiltChunk,
        direction: Direction?,
        excludedDirections: Int
    ) {
        val data = builtChunk.getData()

        for (nextDirection in DIRECTIONS) {
            val nextBuiltChunk = getAdjacentChunk(cameraChunkBlockPos, builtChunk, nextDirection) ?: continue
            val index = (nextBuiltChunk as IPatchedBuiltChunk).index
            if (cullingBitSet.containsFast(index)) continue

            if (excludedDirections and (1 shl nextDirection.ordinal) != 0) continue
            if (direction != null && !data.isVisibleThrough(direction.opposite, nextDirection)) continue

            cullingBitSet.addFast(index)
            recursiveUpdateCulling(
                cameraChunkBlockPos,
                cullingBitSet,
                nextBuiltChunk,
                nextDirection,
                excludedDirections or (1 shl nextDirection.opposite.ordinal)
            )
        }
    }

    private inline fun getAdjacentChunk(pos: BlockPos, chunk: BuiltChunk, direction: Direction): BuiltChunk? {
        val blockPos = chunk.getNeighborPosition(direction)
        if (blockPos.y < 0 || blockPos.y >= 256) return null
        val range = ((worldRenderer as AccessorWorldRenderer).viewDistance shl 4).sq
        if ((pos.x - blockPos.x).sq > range || (pos.z - blockPos.z).sq > range) return null
        return getRenderedChunkByBlock(blockPos)
    }

    fun getRegionByBlock(pos: BlockPos): RenderRegion {
        return regionArray[regionPos2Index(
            Math.floorMod(pos.x shr 8, regionSize),
            Math.floorMod(pos.z shr 8, regionSize)
        )]
    }

    fun getRegionByBlock(blockX: Int, blockZ: Int): RenderRegion {
        return regionArray[regionPos2Index(
            Math.floorMod(blockX shr 8, regionSize),
            Math.floorMod(blockZ shr 8, regionSize)
        )]
    }

    fun getRegionByChunk(chunkX: Int, chunkZ: Int): RenderRegion {
        return regionArray[regionPos2Index(
            Math.floorMod(chunkX shr 4, regionSize),
            Math.floorMod(chunkZ shr 4, regionSize)
        )]
    }

    fun getRegion(regionX: Int, regionZ: Int): RenderRegion {
        return regionArray[regionPos2Index(
            Math.floorMod(regionX, regionSize),
            Math.floorMod(regionZ, regionSize)
        )]
    }

    override fun scheduleRebuild(x: Int, y: Int, z: Int, important: Boolean) {
        chunks[chunkPos2Index(
            Math.floorMod(x, sizeX),
            Math.floorMod(y, sizeY),
            Math.floorMod(z, sizeZ)
        )].scheduleRebuild(important)
    }

    fun getRenderedChunk(chunkX: Int, chunkY: Int, chunkZ: Int): BuiltChunk? {
        return if (chunkY in 0 until sizeY) {
            chunks[chunkPos2Index(
                MathHelper.floorMod(chunkX, sizeX),
                chunkY,
                MathHelper.floorMod(chunkZ, sizeZ)
            )]
        } else {
            null
        }
    }

    fun getRenderedChunk0(chunkX: Int, chunkY: Int, chunkZ: Int): BuiltChunk {
        return chunks[chunkPos2Index(
            MathHelper.floorMod(chunkX, sizeX),
            chunkY,
            MathHelper.floorMod(chunkZ, sizeZ)
        )]
    }

    override fun getRenderedChunk(pos: BlockPos): BuiltChunk? {
        return getRenderedChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4)
    }

    fun getRenderedChunkByBlock(pos: BlockPos): BuiltChunk? {
        return getRenderedChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4)
    }

    inline fun chunkPos2Index(x: Int, y: Int, z: Int): Int {
        return y + (x + z * sizeX0) * sizeY0
    }

    inline fun regionPos2Index(x: Int, z: Int): Int {
        return x + z * regionSize
    }

    override fun clear() {
        for (builtChunk in chunks) {
            builtChunk.delete()
        }
        for (region in regionArray) {
            region.clear()
        }
        lastSortingJob?.cancel(true)
        lastCaveCullingJob?.cancel(true)
    }

    private companion object {
        @JvmField
        val DIRECTIONS = Direction.values()
    }
}