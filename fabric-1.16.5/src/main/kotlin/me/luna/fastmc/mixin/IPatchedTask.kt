package me.luna.fastmc.mixin

import com.mojang.blaze3d.systems.RenderSystem
import me.luna.fastmc.mixin.accessor.AccessorChunkBuilder
import me.luna.fastmc.shared.opengl.VboInfo
import me.luna.fastmc.shared.opengl.glNamedBufferSubData
import me.luna.fastmc.shared.util.EMPTY_RUNNABLE
import me.luna.fastmc.terrain.ChunkVertexData
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.chunk.ChunkBuilder
import net.minecraft.util.crash.CrashReport
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

interface IPatchedTask {
    val cancelled0: AtomicBoolean
    val chunkBuilder: ChunkBuilder
    val builtChunk: ChunkBuilder.BuiltChunk

    fun scheduleUpload(block: Runnable): CompletableFuture<ChunkBuilder.Result>? {
        val cancelled = cancelled0

        return CompletableFuture.runAsync(EMPTY_RUNNABLE) {
            (chunkBuilder as AccessorChunkBuilder).uploadQueue.add(it)
        }.thenCompose {
            if (!cancelled.get()) {
                if (!RenderSystem.isOnRenderThread()) {
                    CompletableFuture.runAsync(block) {
                        RenderSystem.recordRenderCall(it::run)
                    }
                } else {
                    block.run()
                    CompletableFuture.completedFuture(null)
                }
            } else {
                CompletableFuture.completedFuture(null)
            }
        }.handle { _, throwable ->
            if (throwable != null && throwable !is CancellationException && throwable !is InterruptedException) {
                MinecraftClient.getInstance().setCrashReport(CrashReport.create(throwable, "Rendering chunk"))
            }

            if (cancelled.get()) {
                ChunkBuilder.Result.CANCELLED
            } else {
                ChunkBuilder.Result.SUCCESSFUL
            }
        }
    }

    fun updateVertexData(
        chunkIndex: Int,
        dataArray: Array<ChunkVertexData?>,
        index: Int,
        builtOrigin: Long,
        newBuffer: ByteBuffer,
        vertexCount: Int
    ) {
        val vertexSize = newBuffer.remaining()
        val newVboSize = ((vertexSize + 4095) shr 12 shl 12) + 8192
        val maxVboSize = newVboSize + 16384
        val vbo = dataArray[index]?.updateVbo(vertexSize, newVboSize, maxVboSize) ?: ChunkVertexData.newVbo(newVboSize)

        glNamedBufferSubData(vbo.id, 0, newBuffer)
        dataArray[index] = ChunkVertexData(
            chunkIndex,
            builtOrigin,
            VboInfo(
                vbo,
                vertexCount,
                vertexSize
            )
        )
    }

    fun clearVertexData(
        chunkIndex: Int,
        dataArray: Array<ChunkVertexData?>,
        index: Int,
        builtOrigin: Long
    ) {
        val oldVertexData = dataArray[index]
        if (oldVertexData != null) oldVertexData.let {
            dataArray[index] = ChunkVertexData(
                chunkIndex,
                builtOrigin,
                VboInfo(
                    oldVertexData.vboInfo.vbo,
                    0,
                    oldVertexData.vboInfo.vertexSize
                )
            )
        }
    }
}