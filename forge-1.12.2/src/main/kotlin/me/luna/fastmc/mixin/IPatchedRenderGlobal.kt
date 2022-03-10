package me.luna.fastmc.mixin

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.luna.fastmc.shared.util.ParallelUtils
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.util.math.BlockPos
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import net.minecraft.client.renderer.RenderGlobal.ContainerLocalRenderInformation as RenderInfo

interface IPatchedRenderGlobal {
    fun iterationRecursive(
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
        if (counts.getAndUpdate { if (it > 0) it - 1 else it } > 0) {
            scope.launch(Dispatchers.Default) {
                actor.send(info)
                iterationRecursive0(
                    thisRef,
                    scope,
                    actor,
                    counts,
                    playerPos,
                    flag,
                    frameCount,
                    camera,
                    renderInfos,
                    info
                )
                counts.getAndIncrement()
            }
        } else {
            actor.trySend(info)
            iterationRecursive0(thisRef, scope, actor, counts, playerPos, flag, frameCount, camera, renderInfos, info)
        }
    }

    fun iterationRecursive0(
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
    )

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
            val actor = actor<RenderInfo>(capacity = Channel.UNLIMITED) {
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
}