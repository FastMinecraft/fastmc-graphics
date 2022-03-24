package me.luna.fastmc.terrain

import me.luna.fastmc.mixin.IPatchedBuiltChunk
import net.minecraft.client.render.BuiltChunkStorage
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World

class RegionBuiltChunkStorage(
    chunkBuilder: ChunkBuilder,
    world: World,
    viewDistance: Int,
    worldRenderer: WorldRenderer
) : BuiltChunkStorage(chunkBuilder, world, viewDistance, worldRenderer) {
    val regionSize = ((super.sizeX + 15) shr 4) + 1
    val regionArray: Array<RenderRegion>

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
                region.setOrigin(x shl 8, z shl 8)
                regionArray[i] = region
            }
        }

        for (builtChunk in chunks) {
            (builtChunk as IPatchedBuiltChunk).region = getRegionByBlock(builtChunk.origin.x, builtChunk.origin.z)
        }
    }

    private fun getChunkIndex(x: Int, y: Int, z: Int): Int {
        return (z * sizeY + y) * sizeX + x
    }

    private fun getRegionIndex(x: Int, z: Int): Int {
        return z * regionSize + x
    }

    override fun updateCameraPosition(cameraX: Double, cameraZ: Double) {
        super.updateCameraPosition(cameraX, cameraZ)

        for (builtChunk in chunks) {
            val region = getRegionByBlock(builtChunk.origin)
            region.setOrigin(builtChunk.origin.x shr 8 shl 8, builtChunk.origin.z shr 8 shl 8)
            (builtChunk as IPatchedBuiltChunk).region = region
        }
    }

    fun getRenderedChunkByBlock(pos: BlockPos): BuiltChunk? {
        return getRenderedChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4)
    }

    fun getRenderedChunk(chunkX: Int, chunkY: Int, chunkZ: Int): BuiltChunk? {
        var x1 = chunkX
        var z1 = chunkZ
        return if (chunkY in 0 until sizeY) {
            x1 = MathHelper.floorMod(x1, sizeX)
            z1 = MathHelper.floorMod(z1, sizeZ)
            chunks[(z1 * sizeY + chunkY) * sizeX + x1]
        } else {
            null
        }
    }

    fun getRegionByBlock(pos: BlockPos): RenderRegion {
        return regionArray[getRegionIndex(
            Math.floorMod(pos.x shr 8, regionSize),
            Math.floorMod(pos.z shr 8, regionSize)
        )]
    }

    fun getRegionByBlock(blockX: Int, blockZ: Int): RenderRegion {
        return regionArray[getRegionIndex(
            Math.floorMod(blockX shr 8, regionSize),
            Math.floorMod(blockZ shr 8, regionSize)
        )]
    }

    fun getRegionByChunk(chunkX: Int, chunkZ: Int): RenderRegion {
        return regionArray[getRegionIndex(
            Math.floorMod(chunkX shr 4, regionSize),
            Math.floorMod(chunkZ shr 4, regionSize)
        )]
    }

    fun getRegion(regionX: Int, regionZ: Int): RenderRegion {
        return regionArray[getRegionIndex(
            Math.floorMod(regionX, regionSize),
            Math.floorMod(regionZ, regionSize)
        )]
    }

    override fun clear() {
        chunks.forEach {
            it.delete()
        }
        regionArray.forEach {
            it.clear()
        }
    }
}