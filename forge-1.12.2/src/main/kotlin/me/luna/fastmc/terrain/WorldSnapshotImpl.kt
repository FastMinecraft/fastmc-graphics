@file:Suppress("NOTHING_TO_INLINE")

package me.luna.fastmc.terrain

import me.luna.fastmc.mixin.IPatchedIBlockAccess
import me.luna.fastmc.shared.terrain.WorldSnapshot112
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.EntityRenderer
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.EnumSkyBlock
import net.minecraft.world.IBlockAccess
import net.minecraft.world.WorldType
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.BiomeColorHelper
import net.minecraft.world.chunk.Chunk

class WorldSnapshotImpl(override val context: RebuildContextImpl) :
    WorldSnapshot112<Chunk, IBlockState, BiomeColorHelper.ColorResolver>(
        chunkArray(),
        stateArray(),
        Blocks.AIR.defaultState,
    ),
    IBlockAccess, IPatchedIBlockAccess {

    private lateinit var world: WorldClient
    private val tempPosLight = BlockPos.MutableBlockPos()

    override fun init(): Boolean {
        val mc = Minecraft.getMinecraft()
        world = mc.world ?: return false
        biomeBlendRadius = 2

        var i = 0
        var sectionIndex = 0

        for (cx in context.chunkX - 1..context.chunkX + 1) {
            for (cz in context.chunkZ - 1..context.chunkZ + 1) {
                val chunk = world.getChunk(cx, cz)
                val sectionArray = chunk.blockStorageArray
                chunkArray[i++] = chunk
                for (cy in context.chunkY - 1..context.chunkY + 1) {
                    if (cy != -1 && cy != sectionArray.size) {
                        val section = sectionArray[cy]
                        if (section != null && !section.isEmpty) {
                            val infoArray = sectionLocalInfo[sectionIndex]
                            for (info in infoArray) {
                                val index = info and 0xFFFF
                                val blockX = info shr 24
                                val blockY = info shr 20 and 15
                                val blockZ = info shr 16 and 15
                                blockStateArray[index] = section.get(blockX, blockY, blockZ)
                            }
                        }
                    }
                    sectionIndex++
                }
            }
        }

        sectionIndex = 0

        for (cx in context.chunkX - 1..context.chunkX + 1) {
            for (cz in context.chunkZ - 1..context.chunkZ + 1) {
                for (cy in context.chunkY - 1..context.chunkY + 1) {
                    val infoArray = sectionLocalInfo[sectionIndex++]
                    for (info in infoArray) {
                        val index = info and 0xFFFF
                        val localX = info shr 24
                        val localY = info shr 20 and 15
                        val localZ = info shr 16 and 15
                        val blockState = blockStateArray[index]

                        val blockX = (cx shl 4) + localX
                        val blockY = (cy shl 4) + localY
                        val blockZ = (cz shl 4) + localZ
                        tempPosLight.setPos(blockX, blockY, blockZ)
                        val isOpaqueFullCube = blockState.isOpaqueCube

                        val light = if (isOpaqueFullCube) {
                            0
                        } else {
                            val sky = world.getLightFromNeighborsFor(EnumSkyBlock.SKY, tempPosLight)
                            var block = world.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, tempPosLight)
                            val luminance = blockState.getLightValue(this, tempPosLight)
                            if (block < luminance) {
                                block = luminance
                            }
                            (sky shl 12) or (block shl 4)
                        }

                        val aoLight = (blockState.ambientOcclusionLightValue * 255.0f).toInt() shl 16

                        val isOpaqueFullCubeBit = if (isOpaqueFullCube) {
                            0b0001_0000_0000_0000_0000_0000_0000
                        } else {
                            0b0000_0000_0000_0000_0000_0000_0000
                        }

                        lightArray[index] = isOpaqueFullCubeBit or aoLight or light
                    }
                }
            }
        }


        return true
    }

    override fun getTileEntity(pos: BlockPos): TileEntity? {
        val chunk = getChunk(pos.x shr 4, pos.z shr 4)
        return chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK)
    }

    override fun getWorldBrightness(direction: Int, shaded: Boolean): Int {
        return if (!shaded) {
            255
        } else {
            when (direction) {
                0 -> 127
                1 -> 255
                2, 3 -> 204
                else -> 153
            }
        }
    }

    private val tempPosBlockColor = BlockPos.MutableBlockPos()

    override fun getRawBlockColor(x: Int, y: Int, z: Int, blockState: IBlockState): Int {
        var color = blockColorMap.colorMultiplier(blockState, this, tempPosBlockColor.setPos(x, y, z), 114514)
        if (EntityRenderer.anaglyphEnable) {
            color = TextureUtil.anaglyphColor(color)
        }
        return color
    }

    override fun getColor(pos: BlockPos, colorResolver: BiomeColorHelper.ColorResolver): Int {
        return getBlendedColor(pos.x, pos.y, pos.z, colorResolver)
    }

    private val tempPosColor = BlockPos.MutableBlockPos()

    override fun getColor(x: Int, y: Int, z: Int, colorResolver: BiomeColorHelper.ColorResolver): Int {
        tempPosColor.setPos(x, y, z)
        return colorResolver.getColorAtPos(
            getChunk(x shr 4, z shr 4).getBiome(tempPosColor, world.biomeProvider),
            tempPosColor
        )
    }

    override fun getCombinedLight(pos: BlockPos, lightValue: Int): Int {
        throw UnsupportedOperationException()
    }

    override fun getBlockState(x: Int, y: Int, z: Int): IBlockState {
        return getBlockState0(x, y, z)
    }

    override fun getBlockState(pos: BlockPos): IBlockState {
        return getBlockState(pos.x, pos.y, pos.z)
    }

    override fun isAirBlock(pos: BlockPos): Boolean {
        return getBlockState(pos).material === Material.AIR
    }

    override fun getBiome(pos: BlockPos): Biome {
        return getChunk(pos.x shr 4, pos.z shr 4).getBiome(pos, world.biomeProvider)
    }

    override fun getStrongPower(pos: BlockPos, direction: EnumFacing): Int {
        throw UnsupportedOperationException()
    }

    override fun getWorldType(): WorldType {
        throw UnsupportedOperationException()
    }

    override fun isSideSolid(pos: BlockPos, side: EnumFacing, _default: Boolean): Boolean {
        throw UnsupportedOperationException()
    }

    companion object {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        @JvmField
        val blockColorMap = Minecraft.getMinecraft().blockColors!!
    }
}