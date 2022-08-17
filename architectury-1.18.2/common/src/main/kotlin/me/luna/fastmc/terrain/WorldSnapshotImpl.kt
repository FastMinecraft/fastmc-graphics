@file:Suppress("NOTHING_TO_INLINE")

package me.luna.fastmc.terrain

import me.luna.fastmc.mixin.accessor.AccessorLightStorage
import me.luna.fastmc.shared.terrain.WorldSnapshot113
import me.luna.fastmc.util.lightStorage
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.RegistryEntry
import net.minecraft.world.BlockRenderView
import net.minecraft.world.LightType
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.source.BiomeAccess
import net.minecraft.world.chunk.ChunkNibbleArray
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.chunk.light.ChunkLightProvider
import net.minecraft.world.chunk.light.LightingProvider
import net.minecraft.world.chunk.light.SkyLightStorage
import net.minecraft.world.level.ColorResolver
import kotlin.concurrent.read

class WorldSnapshotImpl(override val context: RebuildContextImpl) :
    WorldSnapshot113<WorldChunk, BlockState, FluidState, ColorResolver>(
        chunkArray(),
        stateArray(),
        stateArray(),
        Blocks.AIR.defaultState,
        Fluids.EMPTY.defaultState
    ),
    BlockRenderView, BiomeAccess.Storage {

    private lateinit var world: ClientWorld
    private val tempPosLight = BlockPos.Mutable()

    override fun init(): Boolean {
        val mc = MinecraftClient.getInstance()
        world = mc.world ?: return false

        minChunkY = world.bottomY shr 4
        maxChunkY = world.topY shr 4
        biomeBlendRadius = mc.options.biomeBlendRadius

        var i = 0
        var sectionIndex = 0

        for (cx in context.chunkX - 1..context.chunkX + 1) {
            for (cz in context.chunkZ - 1..context.chunkZ + 1) {
                val chunk = world.getChunk(cx, cz)
                val sectionArray = chunk.sectionArray
                chunkArray[i++] = chunk
                for (cy in context.chunkY - 1 - minChunkY..context.chunkY + 1 - minChunkY) {
                    if (cy != -1 && cy != sectionArray.size) {
                        val section = sectionArray[cy]
                        if (section != null && !section.isEmpty) {
                            val infoArray = sectionLocalInfo[sectionIndex]
                            for (info in infoArray) {
                                val index = info and 0xFFFF
                                val blockX = info shr 24
                                val blockY = info shr 20 and 15
                                val blockZ = info shr 16 and 15
                                val blockState = section.getBlockState(blockX, blockY, blockZ)
                                blockStateArray[index] = blockState
                                fluidStateArray[index] = blockState.fluidState
                            }
                        }
                    }
                    sectionIndex++
                }
            }
        }

        sectionIndex = 0
        val lightingProvider = world.lightingProvider as OffThreadLightingProvider
        val blockLightStorage = (lightingProvider.get(LightType.BLOCK) as ChunkLightProvider<*, *>).lightStorage
        val skyLightStorage =
            (lightingProvider.get(LightType.SKY) as ChunkLightProvider<*, *>).lightStorage as SkyLightStorage

        @Suppress("UNCHECKED_CAST")
        val accessor = skyLightStorage as AccessorLightStorage<SkyLightStorage.Data>

        lightingProvider.readWriteLock.read {
            val data = accessor.uncachedStorage as SkyLightStorage.Data

            for (cx in context.chunkX - 1..context.chunkX + 1) {
                for (cz in context.chunkZ - 1..context.chunkZ + 1) {
                    val longXZ = ChunkSectionPos.asLong(cx, 0, cz)
                    val topSection = data.columnToTopSection[longXZ]
                    for (cy in context.chunkY - 1..context.chunkY + 1) {
                        val sectionPos = longXZ or cy.toLong()
                        val blockLightSection = blockLightStorage.getLightSection(sectionPos)

                        var skyLightArray: ChunkNibbleArray? = null
                        var blocked = false
                        var lightY = cy
                        if (topSection != data.minSectionY && lightY < topSection) {
                            skyLightArray = accessor.invokeGetLightSection(data, longXZ or lightY.toLong())
                            if (skyLightArray == null) {
                                blocked = true
                                while (skyLightArray == null) {
                                    if (++lightY >= topSection) {
                                        break
                                    }
                                    skyLightArray = accessor.invokeGetLightSection(data, longXZ or lightY.toLong())
                                }
                            }
                        }

                        val infoArray = sectionLocalInfo[sectionIndex]
                        for (info in infoArray) {
                            val index = info and 0xFFFF
                            val localX = info shr 24
                            val localY = info shr 20 and 15
                            val localZ = info shr 16 and 15
                            val blockState = blockStateArray[index]

                            val blockX = (cx shl 4) + localX
                            val blockY = (cy shl 4) + localY
                            val blockZ = (cz shl 4) + localZ
                            tempPosLight.set(blockX, blockY, blockZ)
                            val isOpaqueFullCube = blockState.isOpaqueFullCube(this, tempPosLight)

                            val light = if (blockState.hasEmissiveLighting(world, tempPosLight)) {
                                0xF0F0
                            } else if (isOpaqueFullCube) {
                                0
                            } else {
                                val sky = skyLightArray?.get(localX, if (blocked) 0 else localY, localZ) ?: 15
                                var block = blockLightSection?.get(localX, localY, localZ) ?: 0
                                val luminance = blockState.luminance
                                if (block < luminance) {
                                    block = luminance
                                }
                                (sky shl 12) or (block shl 4)
                            }

                            val aoLight =
                                (blockState.getAmbientOcclusionLightLevel(this, tempPosLight) * 255.0f).toInt() shl 16

                            val isOpaqueFullCubeBit = if (isOpaqueFullCube) {
                                0b0001_0000_0000_0000_0000_0000_0000
                            } else {
                                0b0000_0000_0000_0000_0000_0000_0000
                            }

                            lightArray[index] = isOpaqueFullCubeBit or aoLight or light
                        }
                        sectionIndex++
                    }
                }
            }
        }


        return true
    }

    override fun getHeight(): Int {
        return world.height
    }

    override fun getBottomY(): Int {
        return world.bottomY
    }

    override fun getBlockEntity(pos: BlockPos): BlockEntity? {
        val chunk = getChunk(pos.x shr 4, pos.z shr 4)
        return chunk.getBlockEntity(pos, WorldChunk.CreationType.CHECK)
    }

    override fun getBlockState(pos: BlockPos): BlockState {
        return getBlockState0(pos.x, pos.y, pos.z)
    }

    override fun getFluidState(pos: BlockPos): FluidState {
        return getFluidState(pos.x, pos.y, pos.z)
    }

    override fun getBrightness(direction: Direction, shaded: Boolean): Float {
        return world.getBrightness(direction, shaded)
    }

    override fun getWorldBrightness(direction: Int, shaded: Boolean): Int {
        val darkened = world.dimensionEffects.isDarkened
        return if (!shaded) {
            if (darkened) 229 else 255
        } else {
            when (direction) {
                0 -> if (darkened) 229 else 127
                1 -> if (darkened) 229 else 255
                2, 3 -> 204
                else -> 153
            }
        }
    }

    override fun getLightingProvider(): LightingProvider {
        return world.lightingProvider
    }

    private val tempPosBlockColor = BlockPos.Mutable()

    override fun getRawBlockColor(x: Int, y: Int, z: Int, blockState: BlockState): Int {
        return blockColorMap.getColor(blockState, this, tempPosBlockColor.set(x, y, z), 114514)
    }

    override fun getColor(pos: BlockPos, colorResolver: ColorResolver): Int {
        return getBlendedColor(pos.x, pos.y, pos.z, colorResolver)
    }

    private val tempPosColor = BlockPos.Mutable()

    override fun getColor(x: Int, y: Int, z: Int, colorResolver: ColorResolver): Int {
        return colorResolver.getColor(world.getBiome(tempPosColor.set(x, y, z)).value(), x.toDouble(), z.toDouble())
    }

    override fun getBiomeForNoiseGen(biomeX: Int, biomeY: Int, biomeZ: Int): RegistryEntry<Biome>? {
        val chunk = getChunk(biomeX shr 2, biomeZ shr 2)
        return chunk.getBiomeForNoiseGen(biomeX, biomeY, biomeZ)
            ?: world.getGeneratorStoredBiome(biomeX, biomeY, biomeZ)
    }

    companion object {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        @JvmField
        val blockColorMap = MinecraftClient.getInstance().blockColors!!
    }
}