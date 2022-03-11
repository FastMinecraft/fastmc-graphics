package me.luna.fastmc.mixin

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.luna.fastmc.mixin.accessor.AccessorRenderGlobal
import me.luna.fastmc.shared.util.DoubleBufferedCollection
import me.luna.fastmc.shared.util.ParallelUtils
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import me.luna.fastmc.shared.util.fastFloor
import me.luna.fastmc.util.counter
import me.luna.fastmc.util.facing
import me.luna.fastmc.util.renderChunk
import me.luna.fastmc.util.setFacing
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.chunk.RenderChunk
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator
import net.minecraft.client.renderer.RenderGlobal.ContainerLocalRenderInformation as RenderInfo

interface IPatchedRenderGlobal {
    val renderEntityList: DoubleBufferedCollection<FastObjectArrayList<Entity>>

    fun updateRenderEntityList(scope: CoroutineScope, mc: Minecraft, world: World) {
        this as RenderGlobal
        this as AccessorRenderGlobal
        world as IPatchedWorld

        renderEntityList.swap()
        val mainList = renderEntityList.get()
        mainList.ensureCapacity(world.loadedEntityList.size)

        val renderViewEntity = mc.renderViewEntity ?: return
        val renderSelf =
            mc.gameSettings.thirdPersonView != 0 || renderViewEntity is EntityLivingBase && renderViewEntity.isPlayerSleeping
        val renderInfos = this.renderInfos

        ParallelUtils.splitListIndex(
            renderInfos.size,
            blockForEach = { start, end ->
                scope.launch {
                    for (i in start until end) {
                        val renderInfo = renderInfos[i]
                        val chunk = world.getChunk(renderInfo.renderChunk.position)
                        val multiMap = chunk.entityLists[renderInfo.renderChunk.position.y / 16]

                        if (!multiMap.isEmpty()) {
                            mainList.ensureCapacity(mainList.size + multiMap.size)
                            for (it in multiMap) {
                                if (!renderSelf && it === renderViewEntity) continue
                                if (it.posY >= 0.0 && it.posY < 256.0 && !world.isBlockLoaded(
                                        it.posX.fastFloor(),
                                        it.posZ.fastFloor()
                                    )
                                ) continue

                                mainList.add(it)
                            }
                        }
                    }
                }
            }
        )
    }

    fun getRenderChunkOffset0(
        playerPos: BlockPos?,
        renderChunkBase: RenderChunk?,
        facing: EnumFacing?
    ): RenderChunk?

    suspend fun iterationRecursive(
        thisRef: RenderGlobal,
        scope: CoroutineScope,
        actor: SendChannel<RenderInfo>,
        counts: AtomicInteger,
        playerPos: BlockPos,
        flag: Boolean,
        frameCount: Int,
        camera: ICamera,
        renderInfos: MutableList<RenderInfo>,
        info: RenderInfo
    ) {
        if (counts.getAndUpdate(updateFunction) > 0) {
            scope.launch(Dispatchers.Default) {
                actor.send(info)
                val renderChunk = info.renderChunk
                val facing = info.facing
                for (nextFacing in EnumFacing.values()) {
                    val nextRenderChunk = getRenderChunkOffset0(
                        playerPos,
                        renderChunk, nextFacing
                    )
                    if ((!flag || !info.hasDirection(nextFacing.opposite))
                        && (!flag || facing == null || renderChunk.getCompiledChunk()
                            .isVisible(facing.opposite, nextFacing))
                        && nextRenderChunk != null && nextRenderChunk.setFrameIndex(frameCount) && camera.isBoundingBoxInFrustum(
                            nextRenderChunk.boundingBox
                        )
                    ) {
                        val nextInfo = thisRef.ContainerLocalRenderInformation(
                            nextRenderChunk,
                            nextFacing,
                            info.counter + 1
                        )
                        nextInfo.setDirection(info.setFacing, nextFacing)
                        iterationRecursive(
                            thisRef,
                            scope,
                            actor,
                            counts,
                            playerPos,
                            flag,
                            frameCount,
                            camera,
                            renderInfos,
                            nextInfo
                        )
                    }
                }
                counts.getAndIncrement()
            }
        } else {
            actor.send(info)
            val renderChunk = info.renderChunk
            val facing = info.facing
            for (nextFacing in EnumFacing.values()) {
                val nextRenderChunk = getRenderChunkOffset0(playerPos, renderChunk, nextFacing)
                if ((!flag || !info.hasDirection(nextFacing.opposite))
                    && (!flag || facing == null || renderChunk.getCompiledChunk()
                        .isVisible(facing.opposite, nextFacing))
                    && nextRenderChunk != null && nextRenderChunk.setFrameIndex(frameCount) && camera.isBoundingBoxInFrustum(
                        nextRenderChunk.boundingBox
                    )
                ) {
                    val nextInfo =
                        thisRef.ContainerLocalRenderInformation(nextRenderChunk, nextFacing, info.counter + 1)
                    nextInfo.setDirection(info.setFacing, nextFacing)
                    iterationRecursive(
                        thisRef,
                        scope,
                        actor,
                        counts,
                        playerPos,
                        flag,
                        frameCount,
                        camera,
                        renderInfos,
                        nextInfo
                    )
                }
            }
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    fun iterationParallel(
        thisRef: RenderGlobal,
        playerPos: BlockPos,
        flag: Boolean,
        frameCount: Int,
        camera: ICamera,
        renderInfos: MutableList<RenderInfo>,
        queue: Queue<RenderInfo>
    ) {
        runBlocking {
            val actor = actor<RenderInfo>(capacity = ParallelUtils.CPU_THREADS * 6 * 10) {
                for (info in channel) {
                    renderInfos.add(info)
                }
            }

            val count = AtomicInteger(ParallelUtils.CPU_THREADS)

            coroutineScope {
                for (info in queue) {
                    iterationRecursive(
                        thisRef,
                        this,
                        actor,
                        count,
                        playerPos,
                        flag,
                        frameCount,
                        camera,
                        renderInfos,
                        info
                    )
                }
            }

            actor.close()
        }
    }

    private companion object {
        val updateFunction = IntUnaryOperator {
            if (it > 0) it - 1 else it
        }
    }
}