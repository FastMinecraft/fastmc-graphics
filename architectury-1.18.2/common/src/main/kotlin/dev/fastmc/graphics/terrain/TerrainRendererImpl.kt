package dev.fastmc.graphics.terrain

import dev.fastmc.common.BYTE_FALSE
import dev.fastmc.common.BYTE_TRUE
import dev.fastmc.common.BYTE_UNCHECKED
import dev.fastmc.common.collection.Int2ByteCacheMap
import dev.fastmc.common.collection.Int2ObjectCacheMap
import dev.fastmc.graphics.FastMcMod
import dev.fastmc.graphics.mixin.accessor.AccessorVoxelShape
import dev.fastmc.graphics.renderer.TileEntityInstancingRendererImpl
import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.terrain.*
import dev.fastmc.graphics.util.Minecraft
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayers
import net.minecraft.entity.Entity
import net.minecraft.fluid.Fluids
import net.minecraft.util.function.BooleanBiFunction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.shape.SlicedVoxelShape
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.chunk.ChunkStatus

private const val LAYER_COUNT = 3

class TerrainRendererImpl(renderer: WorldRenderer) : TerrainRenderer(
    renderer,
    LAYER_COUNT
) {
    override val minChunkY: Int
        get() {
            val world = Minecraft.getInstance().world
            return if (world != null) {
                world.bottomY shr 4
            } else {
                16
            }
        }

    override val maxChunkY: Int
        get() {
            val world = Minecraft.getInstance().world
            return if (world != null) {
                world.topY shr 4
            } else {
                16
            }
        }

    override val chunkBuilder: ChunkBuilder = ChunkBuilderImpl(this)
    override val contextProvider: ContextProvider = ChunkBuilderContextProviderImpl()

    private val mc = Minecraft.getInstance()

    override val viewDistance: Int
        get() = mc.options.viewDistance

    override val isDebugEnabled: Boolean
        get() = mc.options.debugEnabled

    override val caveCulling: Boolean
        get() {
            val player = mc.player ?: return true
            val world = mc.world ?: return true

            if (!mc.chunkCullingEnabled) return false
            val cameraBlockPos = BlockPos(
                cameraBlockX,
                cameraBlockY,
                cameraBlockZ
            )

            return !player.isSpectator || !world.getBlockState(cameraBlockPos).isOpaqueFullCube(world, cameraBlockPos)
        }

    override val chunkLoadingStatusCache = ChunkLoadingStatusCacheImpl()

    override fun update(uploadChunks: Boolean) {
        val world = mc.world ?: return

        if (viewDistance != lastViewDistance) {
            mc.worldRenderer.reload()
        }

        FastMcMod.profiler.start("pre")
        Entity.setRenderDistanceMultiplier(
            MathHelper.clamp(
                mc.options.viewDistance / 8.0,
                1.0,
                2.5
            ) * mc.options.entityDistanceScaling
        )

        (world.chunkManager.lightingProvider as OffThreadLightingProvider).doLightUpdates(Int.MAX_VALUE, world.hasNoChunkUpdaters(), true)

        update0(uploadChunks)

        FastMcMod.profiler.end()
    }
}

private class ChunkBuilderImpl(renderer: TerrainRenderer) : ChunkBuilder(renderer) {
    override fun newRebuildTask(scheduler: TaskFactory): RebuildTask {
        return RebuildTaskImpl(renderer, scheduler)
    }
}

private class RebuildTaskImpl(renderer: TerrainRenderer, scheduler: ChunkBuilder.TaskFactory) : RebuildTask(
    renderer,
    scheduler
) {
    override fun init0(renderChunk: RenderChunk) {
        super.init0(renderChunk)
        val world = MinecraftClient.getInstance().world
        if (world != null) {
            val chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) ?: throw CancelInitException
            val section = chunk.sectionArray[chunkY - (world.bottomY shr 4)]
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
abstract class RebuildContextImpl : RebuildContext(3) {
    override val worldSnapshot = WorldSnapshotImpl(this)
    override val blockRenderer by lazy { BlockRendererImpl(this) }

    private val faceCullingCache = ByteArray(16 * 16 * 16 * 6)
    private val coveredSideCache = Int2ByteCacheMap(512, BYTE_UNCHECKED)
    private val matchVoxelShapeCache = Int2ByteCacheMap(2048, BYTE_UNCHECKED)
    private val cullingFaceCache = Int2ObjectCacheMap<VoxelShape>(2048)

    val renderBlockPos = BlockPos.Mutable()

    override fun init(task: ChunkBuilderTask) {
        super.init(task)
        faceCullingCache.fill(BYTE_UNCHECKED)
    }

    override fun renderChunk(task: RebuildTask) {
        val startX = chunkX shl 4
        val startY = chunkY shl 4
        val startZ = chunkZ shl 4
        val endX = startX + 15
        val endY = startY + 15
        val endZ = startZ + 15

        val blockRenderer = blockRenderer
        val tileEntityRenderer = FastMcMod.worldRenderer.tileEntityRenderer as TileEntityInstancingRendererImpl
        val emptyFluid = Fluids.EMPTY

        for (y in startY..endY) {
            for (z in startZ..endZ) {
                for (x in startX..endX) {
                    renderBlockPos.set(x, y, z)
                    val blockState = worldSnapshot.getBlockState0(x, y, z)
                    if (blockState.isAir) continue

                    blockX = x
                    blockY = y
                    blockZ = z

                    if (blockState.renderType === BlockRenderType.MODEL) {
                        setActiveLayer(task, RenderLayers.getBlockLayer(blockState) as IPatchedRenderLayer)
                        blockRenderer.renderBlock(blockState)
                    }

                    val fluidState = worldSnapshot.getFluidState(x, y, z)
                    if (fluidState.fluid !== emptyFluid) {
                        setActiveLayer(task, RenderLayers.getFluidLayer(fluidState) as IPatchedRenderLayer)
                        blockRenderer.renderFluid(fluidState, blockState)
                    }

                    if (blockState.hasBlockEntity()) {
                        val blockEntity = worldSnapshot.getBlockEntity(renderBlockPos)
                        if (blockEntity != null) {
                            when {
                                tileEntityRenderer.hasRenderer(blockEntity) -> {
                                    instancingTileEntityList.add(blockEntity as ITileEntityInfo<*>)
                                }
                                Minecraft.getInstance().blockEntityRenderDispatcher.get(blockEntity)
                                    ?.rendersOutsideBoundingBox(blockEntity) == true -> {
                                    globalTileEntityList.add(blockEntity as ITileEntityInfo<*>)
                                }
                                else -> {
                                    tileEntityList.add(blockEntity as ITileEntityInfo<*>)
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
        direction: Direction,
        shaded: Boolean
    ) {
        return calculateAO(thisX, thisY, thisZ, direction.ordinal, shaded)
    }

    fun isSideCovered(
        shape: VoxelShape,
        neighbor: VoxelShape,
        direction: Direction
    ): Boolean {
        return when {
            shape === VoxelShapes.fullCube() && neighbor === VoxelShapes.fullCube() -> {
                true
            }
            neighbor.isEmpty -> {
                false
            }
            else -> {
                var hash = shape.hashCode()
                hash = hash * 31 + neighbor.hashCode()
                hash = hash * 31 + direction.ordinal

                when (coveredSideCache.get(hash)) {
                    BYTE_TRUE -> {
                        true
                    }
                    BYTE_FALSE -> {
                        false
                    }
                    else -> {
                        val axis = direction.axis
                        val isPositive = direction.direction == Direction.AxisDirection.POSITIVE
                        val a: VoxelShape
                        val b: VoxelShape

                        if (isPositive) {
                            a = shape
                            b = neighbor
                        } else {
                            a = neighbor
                            b = shape
                        }

                        if (!fuzzyEqualSq(a.getMax(axis), 1.0, 0.000025)
                            || !fuzzyEqualSq(b.getMin(axis), 0.0, 0.000025)
                        ) {
                            return false
                        }

                        val slicedA = SlicedVoxelShape(a, axis, (a as AccessorVoxelShape).voxels.getSize(axis) - 1)
                        val slicedB = SlicedVoxelShape(b, axis, 0)

                        val result = if (isPositive) {
                            !matchVoxelShape(slicedA, slicedB)
                        } else {
                            !matchVoxelShape(slicedB, slicedA)
                        }
                        coveredSideCache.put(hash, result)
                        result
                    }
                }
            }
        }
    }

    inline fun fuzzyEqualSq(a: Double, b: Double, tolerance: Double): Boolean {
        val c = a - b
        return c * c <= tolerance
    }

    @Suppress("DEPRECATION")
    fun shouldDrawSide(
        self: BlockState,
        direction: Direction
    ): Boolean {
        val index = index(blockX, blockY, blockZ) * 6 + direction.ordinal

        return when (faceCullingCache[index]) {
            BYTE_TRUE -> {
                true
            }
            BYTE_FALSE -> {
                false
            }
            else -> {
                val otherX = blockX + direction.offsetX
                val otherY = blockY + direction.offsetY
                val otherZ = blockZ + direction.offsetZ

                val other = worldSnapshot.getBlockState0(otherX, otherY, otherZ)
                val otherDirection = direction.opposite
                var selfShape: VoxelShape? = null
                var otherShape: VoxelShape? = null

                val selfResult = if (self.block.isSideInvisible(self, other, direction)) {
                    false
                } else if (other.isOpaque) {
                    selfShape = getCullingFace(blockX, blockY, blockZ, self, direction)
                    otherShape = getCullingFace(otherX, otherY, otherZ, other, otherDirection)
                    matchVoxelShape(selfShape, otherShape)
                } else {
                    true
                }

                if (((otherX shr 4) xor chunkX) or ((otherY shr 4) xor chunkY) or ((otherZ shr 4) xor chunkZ) == 0) {
                    val otherResult = if (other.block.isSideInvisible(other, self, otherDirection)) {
                        false
                    } else if (self.isOpaque) {
                        if (selfShape == null) {
                            selfShape = getCullingFace(blockX, blockY, blockZ, self, direction)
                            otherShape = getCullingFace(otherX, otherY, otherZ, other, otherDirection)
                        }
                        matchVoxelShape(otherShape!!, selfShape)
                    } else {
                        true
                    }

                    val otherIndex = index(otherX, otherY, otherZ) * 6 + otherDirection.ordinal
                    faceCullingCache[otherIndex] = if (otherResult) BYTE_TRUE else BYTE_FALSE
                }

                faceCullingCache[index] = if (selfResult) BYTE_TRUE else BYTE_FALSE
                selfResult
            }
        }
    }

    private val cullingFaceTempPos = BlockPos.Mutable()

    private fun getCullingFace(x: Int, y: Int, z: Int, state: BlockState, direction: Direction): VoxelShape {
        var result: VoxelShape? = null
        val shapeCache = state.shapeCache
        if (shapeCache != null) {
            val extrudedFaces = shapeCache.extrudedFaces
            if (extrudedFaces != null) {
                result = extrudedFaces[direction.ordinal]
            }
        }

        if (result == null) {
            val shape = state.getCullingShape(worldSnapshot, cullingFaceTempPos.set(x, y, z))
            val hash = shape.hashCode() * 31 + direction.ordinal
            result = cullingFaceCache.get(hash)
            if (result == null) {
                result = VoxelShapes.extrudeFace(shape, direction)!!
                cullingFaceCache.put(hash, result)
            }
        }

        return result
    }

    private fun matchVoxelShape(a: VoxelShape, b: VoxelShape): Boolean {
        if (a === VoxelShapes.fullCube() && b === VoxelShapes.fullCube()) return false

        val hashAB = hash(a, b)
        return when (matchVoxelShapeCache.get(hashAB)) {
            BYTE_TRUE -> {
                true
            }
            BYTE_FALSE -> {
                false
            }
            else -> {
                val hashBA = hash(b, a)
                val resultAB = VoxelShapes.matchesAnywhere(a, b, BooleanBiFunction.ONLY_FIRST)
                val resultBA = VoxelShapes.matchesAnywhere(b, a, BooleanBiFunction.ONLY_FIRST)
                matchVoxelShapeCache.put(hashBA, resultBA)
                matchVoxelShapeCache.put(hashAB, resultAB)
                resultAB
            }
        }
    }

    private inline fun hash(a: VoxelShape, b: VoxelShape): Int {
        return a.hashCode() * 31 + b.hashCode()
    }

    private inline fun index(x: Int, y: Int, z: Int): Int {
        return (x and 15 shl 8) or (y and 15 shl 4) or (z and 15)
    }
}