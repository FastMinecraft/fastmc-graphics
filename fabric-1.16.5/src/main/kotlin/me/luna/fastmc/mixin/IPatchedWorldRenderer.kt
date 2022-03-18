package me.luna.fastmc.mixin

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.luna.fastmc.mixin.accessor.AccessorWorldRenderer
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.ParallelUtils
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.Frustum
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator
import kotlin.math.max

interface IPatchedWorldRenderer {
    var ticked: Boolean
    val renderTileEntityList: List<BlockEntity>

    fun recursiveSetupTerrainIteration(
        scope: CoroutineScope,
        frustum: Frustum,
        frame: Int,
        cameraChunkBlockPos: BlockPos,
        chunkCulling: Boolean,
        queue: ConcurrentLinkedQueue<WorldRenderer.ChunkInfo>,
        counter: AtomicInteger,
        chunkInfo: WorldRenderer.ChunkInfo
    ) {
        this as AccessorWorldRenderer
        this as WorldRenderer

        val builtChunk = chunkInfo.chunk
        val direction = chunkInfo.direction

        for (nextDirection in DIRECTIONS) {
            val nextBuiltChunk = this.invokeGetAdjacentChunk(cameraChunkBlockPos, builtChunk, nextDirection)

            if (nextBuiltChunk != null
                && (!chunkCulling || !chunkInfo.canCull(nextDirection.opposite))
                && (!chunkCulling || direction == null || builtChunk.getData()
                    .isVisibleThrough(direction.opposite, nextDirection))
                && nextBuiltChunk.shouldBuild()
                && nextBuiltChunk.setRebuildFrame(frame)
                && frustum.isVisible(nextBuiltChunk.boundingBox)
            ) {
                val nextChunkInfo = this.ChunkInfo(nextBuiltChunk, nextDirection, chunkInfo.propagationLevel + 1)
                nextChunkInfo.updateCullingState(chunkInfo.cullingState, nextDirection)
                queue.add(nextChunkInfo)

                if (counter.getAndUpdate(UPDATE_FUNCTION) > 0) {
                    scope.launch(FastMcCoreScope.context) {
                        recursiveSetupTerrainIteration(
                            scope,
                            frustum,
                            frame,
                            cameraChunkBlockPos,
                            chunkCulling,
                            queue,
                            counter,
                            nextChunkInfo
                        )
                        counter.getAndIncrement()
                    }
                } else {
                    recursiveSetupTerrainIteration(
                        scope,
                        frustum,
                        frame,
                        cameraChunkBlockPos,
                        chunkCulling,
                        queue,
                        counter,
                        nextChunkInfo
                    )
                }
            }
        }
    }

    suspend fun setupTerrainIteration0(
        scope: CoroutineScope,
        visibleChunks: FastObjectArrayList<WorldRenderer.ChunkInfo>,
        renderTileEntityList: FastObjectArrayList<BlockEntity>,
        filterRenderInfos: Array<FastObjectArrayList<ChunkBuilder.BuiltChunk>>,
        frustum: Frustum,
        frame: Int,
        cameraChunkBlockPos: BlockPos,
        chunkCulling: Boolean,
        chunkInfoList: ObjectArrayList<WorldRenderer.ChunkInfo>
    ) {
        this as WorldRenderer
        val queue = ConcurrentLinkedQueue(chunkInfoList)
        val dummy = this.ChunkInfo(null, null, 0)

        scope.launch(FastMcCoreScope.context) {
            var it: WorldRenderer.ChunkInfo?
            while (true) {
                do {
                    it = queue.poll()
                } while (it == null)

                if (it === dummy) break

                val builtChunk = it.chunk
                val chunkData = builtChunk.getData()
                val list = chunkData.blockEntities

                if (list.isNotEmpty()) {
                    renderTileEntityList.addAll(list as ObjectArrayList<BlockEntity>)
                }

                for (i in RenderLayer.getBlockLayers().indices) {
                    val layer = RenderLayer.getBlockLayers()[i]
                    if (!chunkData.isEmpty(layer)) {
                        filterRenderInfos[i].add(builtChunk)
                    }
                }

                visibleChunks.add(it)
            }
        }

        coroutineScope {
            val counter = AtomicInteger(ParallelUtils.CPU_THREADS * 2)
            for (i in chunkInfoList.indices) {
                recursiveSetupTerrainIteration(
                    this,
                    frustum,
                    frame,
                    cameraChunkBlockPos,
                    chunkCulling,
                    queue,
                    counter,
                    chunkInfoList[i]
                )
            }
        }

        queue.add(dummy)
    }

    fun setupTerrainIteration(
        visibleChunks: FastObjectArrayList<WorldRenderer.ChunkInfo>,
        renderTileEntityList: FastObjectArrayList<BlockEntity>,
        filterRenderInfos: Array<FastObjectArrayList<ChunkBuilder.BuiltChunk>>,
        frustum: Frustum,
        frame: Int,
        cameraChunkBlockPos: BlockPos,
        chunkCulling: Boolean,
        chunkInfoList: ObjectArrayList<WorldRenderer.ChunkInfo>
    ) {
        runBlocking {
            setupTerrainIteration0(
                this,
                visibleChunks,
                renderTileEntityList,
                filterRenderInfos,
                frustum,
                frame,
                cameraChunkBlockPos,
                chunkCulling,
                chunkInfoList
            )
        }
    }

    companion object {
        @JvmField
        val DIRECTIONS = Direction.values()

        @JvmField
        val UPDATE_FUNCTION = IntUnaryOperator {
            max(it - 1, 0)
        }
    }
}