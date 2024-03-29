package dev.fastmc.graphics.terrain

import dev.fastmc.graphics.FastMcMod
import dev.fastmc.graphics.renderer.TileEntityRendererImpl
import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo
import dev.fastmc.graphics.shared.terrain.*
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

private const val LAYER_COUNT = 2

class TerrainRendererImpl(renderer: dev.fastmc.graphics.shared.renderer.WorldRenderer) : TerrainRenderer(
    renderer,
    LAYER_COUNT,
) {
    override val minChunkY get() = 0
    override val maxChunkY get() = 16

    override val chunkBuilder: ChunkBuilder = ChunkBuilderImpl(this)
    override val contextProvider: ContextProvider = ChunkBuilderContextProviderImpl()

    private val mc = Minecraft.getMinecraft()

    override val viewDistance: Int
        get() = mc.gameSettings.renderDistanceChunks

    override val isDebugEnabled: Boolean
        get() = mc.gameSettings.showDebugInfo

    override val caveCulling: Boolean
        get() {
            val player = mc.player ?: return true
            val world = mc.world ?: return true

            if (!mc.renderChunksMany) return false
            val cameraBlockPos = BlockPos(
                cameraBlockX,
                cameraBlockY,
                cameraBlockZ
            )

            return !player.isSpectator || !world.getBlockState(cameraBlockPos).isOpaqueCube
        }

    override val chunkLoadingStatusCache = ChunkLoadingStatusCacheImpl()

    override fun update(uploadChunks: Boolean) {
        if (viewDistance != lastViewDistance) {
            mc.renderGlobal.loadRenderers()
        }

        FastMcMod.profiler.start("pre")
        update0(uploadChunks)
        FastMcMod.profiler.end()
    }
}

private class ChunkBuilderImpl(renderer: TerrainRenderer) : ChunkBuilder(renderer) {
    override fun newRebuildTask(scheduler: TaskFactory): RebuildTask {
        return RebuildTaskImpl(renderer, scheduler)
    }
}

private class RebuildTaskImpl(renderer: TerrainRenderer, scheduler: ChunkBuilder.TaskFactory) :
    RebuildTask(renderer, scheduler) {
    override fun init0(renderChunk: RenderChunk) {
        super.init0(renderChunk)
        val world = Minecraft.getMinecraft().world
        if (world != null) {
            val chunk = world.chunkProvider.provideChunk(chunkX, chunkZ)
            if (chunk.isEmpty) {
                throw CancelInitException
            }

            val section = chunk.blockStorageArray[chunkY]
            if (section == null || section.isEmpty) {
                throw CancelInitException
            }
        } else {
            throw CancelInitException
        }
    }
}

private class ChunkBuilderContextProviderImpl : ContextProvider() {
    init {
        postConstruct()
    }

    override fun newRebuildContext(pool: ContextPool<RebuildContext>): RebuildContext {
        return object : RebuildContextImpl() {
            override fun release0() {
                super.release0()
                pool.put(this)
            }
        }
    }
}

@Suppress("LeakingThis", "NOTHING_TO_INLINE")
abstract class RebuildContextImpl : RebuildContext(LAYER_COUNT) {
    override val worldSnapshot = WorldSnapshotImpl(this)
    override val blockRenderer by lazy { BlockRendererImpl(this) }

    val renderBlockPos = BlockPos.MutableBlockPos()

    override fun renderChunk(task: RebuildTask) {
        val startX = chunkX shl 4
        val startY = chunkY shl 4
        val startZ = chunkZ shl 4
        val endX = startX + 15
        val endY = startY + 15
        val endZ = startZ + 15

        val blockRenderer = blockRenderer
        val tileEntityRenderer = FastMcMod.worldRenderer.tileEntityRenderer as TileEntityRendererImpl

        for (y in startY..endY) {
            for (z in startZ..endZ) {
                for (x in startX..endX) {
                    var blockState = worldSnapshot.getBlockState(x, y, z)
                    if (blockState.material == Material.AIR) continue

                    renderBlockPos.setPos(x, y, z)
                    blockX = x
                    blockY = y
                    blockZ = z

                    blockState = blockState.getActualState(worldSnapshot, renderBlockPos)
                    blockState = blockState.block.getExtendedState(blockState, worldSnapshot, renderBlockPos)

                    val block = blockState.block

                    when (blockState.renderType) {
                        EnumBlockRenderType.MODEL -> {
                            setActiveLayer(task, block.renderLayer as IPatchedRenderLayer)
                            blockRenderer.renderBlock(blockState)
                        }
                        EnumBlockRenderType.LIQUID -> {
                            setActiveLayer(task, block.renderLayer as IPatchedRenderLayer)
                            blockRenderer.renderFluid(blockState, blockState)
                        }
                        else -> {

                        }
                    }

                    if (block.hasTileEntity(blockState)) {
                        val tileEntity = worldSnapshot.getTileEntity(renderBlockPos)
                        if (tileEntity != null) {
                            when {
                                tileEntityRenderer.hasRenderer(tileEntity) -> {
                                    instancingTileEntityList.add(tileEntity as ITileEntityInfo<*>)
                                }
                                TileEntityRendererDispatcher.instance.getRenderer<TileEntity>(tileEntity)
                                    ?.isGlobalRenderer(tileEntity) == true -> {
                                    globalTileEntityList.add(tileEntity as ITileEntityInfo<*>)
                                }
                                else -> {
                                    tileEntityList.add(tileEntity as ITileEntityInfo<*>)
                                }
                            }
                        }
                    }

                    if (worldSnapshot.unpackIsOpaqueFullCube(worldSnapshot.getLightBits(x, y, z))) {
                        occlusionDataBuilder.markBlocked(
                            x and 15,
                            y and 15,
                            z and 15
                        )
                    }
                }
            }
        }
    }

    inline fun calculateAO(
        thisX: Int,
        thisY: Int,
        thisZ: Int,
        direction: EnumFacing,
        shaded: Boolean
    ) {
        return calculateAO(thisX, thisY, thisZ, direction.ordinal, shaded)
    }
}