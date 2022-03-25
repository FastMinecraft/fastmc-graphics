package me.luna.fastmc.terrain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.luna.fastmc.mixin.IPatchedBuiltChunk
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.ParallelUtils
import me.luna.fastmc.shared.util.distanceSq
import net.minecraft.client.render.BuiltChunkStorage
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import java.util.*

@Suppress("NOTHING_TO_INLINE")
class RegionBuiltChunkStorage(
    chunkBuilder: ChunkBuilder,
    world: World,
    viewDistance: Int,
    worldRenderer: WorldRenderer
) : BuiltChunkStorage(chunkBuilder, world, viewDistance, worldRenderer) {
    val regionSize = (super.sizeX + 7) shr 3
    val regionArray: Array<RenderRegion>

    private var lastSorting = BlockPos.ORIGIN
    private val sortedChunkArray: Array<ChunkBuilder.BuiltChunk> = chunks.copyOf()
    var sortedChunkIndices = IntArray(chunks.size); private set

    init {
        for (i in chunks.indices) {
            (chunks[i] as IPatchedBuiltChunk).index = i
        }

        @Suppress("UNCHECKED_CAST")
        regionArray = arrayOfNulls<RenderRegion>(regionSize * regionSize) as Array<RenderRegion>

        for (x in 0 until regionSize) {
            for (z in 0 until regionSize) {
                val i = getRegionIndex(x, z)
                val region = RenderRegion(i)
                region.setOrigin(x shl 7, z shl 7)
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

    private inline fun getChunkIndex(x: Int, y: Int, z: Int): Int {
        return y + (x + z * sizeX) * sizeY
    }

    private inline fun getRegionIndex(x: Int, z: Int): Int {
        return x + z * regionSize
    }

    override fun updateCameraPosition(x: Double, z: Double) {
        runBlocking {
            val floorX = MathHelper.floor(x)
            val floorZ = MathHelper.floor(z)

            coroutineScope {
                for (k in 0 until sizeX) {
                    launch(FastMcCoreScope.context) {
                        val l = sizeX * 16
                        val m = floorX - 8 - l / 2
                        val n = m + Math.floorMod(k * 16 - m, l)
                        for (o in 0 until sizeZ) {
                            val p = sizeZ * 16
                            val q = floorZ - 8 - p / 2
                            val r = q + Math.floorMod(o * 16 - q, p)
                            for (s in 0 until sizeY) {
                                val t = s * 16
                                val builtChunk = chunks[getChunkIndex(k, s, o)]
                                builtChunk.setOrigin(n, t, r)
                            }
                        }
                    }
                }

                for (region in regionArray) {
                    region.chunks.clear()
                    region.chunks.ensureCapacity(chunks.size)
                }
            }

            ParallelUtils.splitListIndex(
                chunks.size,
                blockForEach = { start, end ->
                    launch(FastMcCoreScope.context) {
                        for (i in start until end) {
                            val builtChunk = chunks[i]
                            builtChunk as IPatchedBuiltChunk
                            val region = getRegionByBlock(builtChunk.origin)
                            region.setOrigin(builtChunk.origin.x shr 7 shl 7, builtChunk.origin.z shr 7 shl 7)
                            region.chunks.addFast(builtChunk.index)
                            builtChunk.region = region
                        }
                    }
                }
            )
        }
    }

    fun updateSorting(cameraChunkBlockPos: BlockPos) {
        if (cameraChunkBlockPos != lastSorting) {
            val comparator = Comparator.comparingInt<ChunkBuilder.BuiltChunk> {
                val chunkOrigin = it.origin
                -distanceSq(
                    cameraChunkBlockPos.x, cameraChunkBlockPos.y, cameraChunkBlockPos.z,
                    chunkOrigin.x, chunkOrigin.y, chunkOrigin.y
                )
            }
            Arrays.sort(sortedChunkArray, comparator)

            val indexArray = IntArray(sortedChunkArray.size)
            for (i in indexArray.indices) {
                indexArray[i] = (sortedChunkArray[i] as IPatchedBuiltChunk).index
            }
            sortedChunkIndices = indexArray
            lastSorting = cameraChunkBlockPos
        }
    }

    fun getRegionByBlock(pos: BlockPos): RenderRegion {
        return regionArray[getRegionIndex(
            Math.floorMod(pos.x shr 7, regionSize),
            Math.floorMod(pos.z shr 7, regionSize)
        )]
    }

    fun getRegionByBlock(blockX: Int, blockZ: Int): RenderRegion {
        return regionArray[getRegionIndex(
            Math.floorMod(blockX shr 7, regionSize),
            Math.floorMod(blockZ shr 7, regionSize)
        )]
    }

    fun getRegionByChunk(chunkX: Int, chunkZ: Int): RenderRegion {
        return regionArray[getRegionIndex(
            Math.floorMod(chunkX shr 3, regionSize),
            Math.floorMod(chunkZ shr 3, regionSize)
        )]
    }

    fun getRegion(regionX: Int, regionZ: Int): RenderRegion {
        return regionArray[getRegionIndex(
            Math.floorMod(regionX, regionSize),
            Math.floorMod(regionZ, regionSize)
        )]
    }

    override fun scheduleRebuild(x: Int, y: Int, z: Int, important: Boolean) {
        chunks[getChunkIndex(
            Math.floorMod(x, sizeX),
            Math.floorMod(y, sizeY),
            Math.floorMod(z, sizeZ)
        )].scheduleRebuild(important)
    }

    fun getRenderedChunk(chunkX: Int, chunkY: Int, chunkZ: Int): BuiltChunk? {
        return if (chunkY in 0 until sizeY) {
            chunks[getChunkIndex(
                MathHelper.floorMod(chunkX, sizeX),
                chunkY,
                MathHelper.floorMod(chunkZ, sizeZ)
            )]
        } else {
            null
        }
    }

    override fun getRenderedChunk(pos: BlockPos): BuiltChunk? {
        return getRenderedChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4)
    }

    fun getRenderedChunkByBlock(pos: BlockPos): BuiltChunk? {
        return getRenderedChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4)
    }


    override fun clear() {
        for (builtChunk in chunks) {
            builtChunk.delete()
        }
        for (region in regionArray) {
            region.clear()
        }
    }
}