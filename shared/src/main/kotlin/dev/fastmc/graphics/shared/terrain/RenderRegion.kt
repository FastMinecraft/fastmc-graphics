package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.*
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.opengl.*
import dev.fastmc.graphics.shared.opengl.impl.RenderBufferPool
import dev.fastmc.graphics.shared.opengl.impl.buildAttribute
import org.joml.FrustumIntersection
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
class RenderRegion(
    private val renderer: TerrainRenderer,
    private val storage: RenderChunkStorage,
    @JvmField val index: Int
) {
    var originX = 0; private set
    val originY get() = storage.minChunkY shl 4
    var originZ = 0; private set

    @JvmField
    val frustumCull: FrustumCull = FrustumCullImpl()

    @JvmField
    val cullingLayerBatch = CullingLayerBatch()

    @JvmField
    val layerBatchArray = Array(renderer.layerCount) {
        if (it == 0) {
            cullingLayerBatch
        } else {
            DefaultLayerBatch(storage, it)
        }
    }

    @JvmField
    val visibleRenderChunkList = FastObjectArrayList.wrap(arrayOfNulls<RenderChunk>(storage.regionChunkCount), 0)

    @JvmField
    val tempVisibleBits = ByteArray(storage.regionChunkCount)

    @JvmField
    val vertexBufferPool = RenderBufferPool((4 * 1024 * 1024).countTrailingZeroBits())

    @JvmField
    val indexBufferPool = RenderBufferPool((4 * 1024 * 1024).countTrailingZeroBits())

    @JvmField
    val boundingBoxBuffer = BoundingBoxBuffer(storage)

    private var vbo = vertexBufferPool.bufferObject
    private var ibo = indexBufferPool.bufferObject

    @JvmField
    var vao = VertexArrayObject().apply {
        attachVbo(vbo, TerrainRenderer.VERTEX_ATTRIBUTE)
        attachIbo(ibo)
    }

    fun updateVao() {
        val newVbo = vertexBufferPool.bufferObject
        val newIbo = indexBufferPool.bufferObject
        if (vbo !== newVbo || ibo !== newIbo) {
            vbo = newVbo
            ibo = newIbo

            vao.destroyVao()
            vao = VertexArrayObject().apply {
                attachVbo(newVbo, TerrainRenderer.VERTEX_ATTRIBUTE)
                attachIbo(newIbo)
            }
        }
    }

    inline fun getLayer(index: Int): ILayerBatch {
        return layerBatchArray[index]
    }

    fun setPos(x: Int, z: Int) {
        if (x != originX || z != originZ) {
            originX = x
            originZ = z
            frustumCull.reset()
        }
    }

    fun destroy() {
        vao.destroyVao()
        vertexBufferPool.destroy()
        indexBufferPool.destroy()
        boundingBoxBuffer.destroy()
        for (layer in layerBatchArray) {
            layer.destroy()
        }
    }

    interface ILayerBatch {
        val layerIndex: Int
        val count: Int

        fun startUpdate()
        fun put(chunk: RenderChunk)
        fun endUpdate()
        fun render(pass: Int, renderPosX: Double, renderPosY: Double, renderPosZ: Double): Boolean {
            if (pass == 0) {
                endUpdate()
            }

            return count != 0
        }

        fun destroy()
    }

    inner class CullingLayerBatch : ILayerBatch {
        private val visibleBuffer = BufferObject.Immutable().allocate(storage.regionChunkCount * 8, 0)
        private val faceDataIndicesBuffer = BufferObject.Immutable().allocate(storage.regionChunkCount * 4, GL_DYNAMIC_STORAGE_BIT)
        private var faceDataBuffer: BufferObject? = null
        private var indirectBuffer: BufferObject? = null

        private val clientFaceDataIndicesByteBuffer = allocateByte(storage.regionChunkCount * 4)
        private val clientFaceDataIndicesIntBuffer = clientFaceDataIndicesByteBuffer.asIntBuffer()
        private val clientFaceDataBuffer = CachedBuffer(storage.regionChunkCount * 16)

        private var isDirty = false
        private var frameCounter = 0

        override val layerIndex: Int get() = 0
        override var count = 0; private set

        init {
            MemoryStack.use {
                withCalloc(1) {
                    glClearNamedBufferData(visibleBuffer.id, GL_R8UI, GL_RED_INTEGER, GL_UNSIGNED_BYTE, it)
                }
            }
        }

        override fun startUpdate() {
            isDirty = true
            frameCounter = 0
            count = 0

            for (i in 0 until clientFaceDataIndicesIntBuffer.capacity()) {
                clientFaceDataIndicesIntBuffer.put(i, -1)
            }
            clientFaceDataBuffer.getInt().clear()
        }

        override fun put(chunk: RenderChunk) {
            val layer = chunk.layers[layerIndex]
            val vertexRegion = layer.vertexRegion ?: return
            val indexRegion = layer.indexRegion ?: return

            val faceData = layer.faceData ?: return

            clientFaceDataIndicesIntBuffer.put(chunk.regionIndex, (faceData.dataSize shl 26) or count)
            val buffer = clientFaceDataBuffer.ensureCapacityInt((count + faceData.dataSize) * 4)
            buffer.position(count * 4)
            faceData.addToBuffer(buffer, vertexRegion.offset, indexRegion.offset)

            count += faceData.dataSize
        }

        override fun endUpdate() {
            if (isDirty && count != 0) {
                faceDataIndicesBuffer.invalidate()
                glNamedBufferSubData(faceDataIndicesBuffer.id, 0, clientFaceDataIndicesByteBuffer)

                val clientFaceData = clientFaceDataBuffer.getByte()
                clientFaceData.limit(count * 16)
                val faceDataBuffer = faceDataBuffer
                if (faceDataBuffer == null || faceDataBuffer.size < clientFaceData.remaining()) {
                    faceDataBuffer?.destroy()
                    this.faceDataBuffer = BufferObject.Immutable().allocate(clientFaceData, GL_DYNAMIC_STORAGE_BIT)
                } else {
                    faceDataBuffer.invalidate()
                    glNamedBufferSubData(faceDataBuffer.id, 0, clientFaceData)
                }

                val indirectBuffer = indirectBuffer
                if (indirectBuffer == null || indirectBuffer.size < 4 + count * 20) {
                    indirectBuffer?.destroy()
                    this.indirectBuffer = BufferObject.Immutable().allocate(4 + count * 20, 0)
                }
            }

            isDirty = false
        }

        override fun render(pass: Int, renderPosX: Double, renderPosY: Double, renderPosZ: Double): Boolean {
            if (!super.render(pass, renderPosX, renderPosY, renderPosZ)) {
                return false
            }

            when (frameCounter) {
                3 -> {
                    when (pass) {
                        0 -> drawIndirect(renderPosX, renderPosY, renderPosZ)
                        else -> return false
                    }
                }
                2 -> {
                    when (pass) {
                        0 -> {
                            dispatchIndirectCompute(
                                renderer.shaderManager.indirectShader[2],
                                faceDataBuffer ?: return false,
                                indirectBuffer ?: return false,
                                renderPosX,
                                renderPosY,
                                renderPosZ
                            )
                        }
                        1 -> {
                            drawIndirect(renderPosX, renderPosY, renderPosZ)
                            frameCounter++
                        }
                        else -> return false
                    }
                }
                0, 1 -> {
                    when (pass) {
                        0 -> {
                            dispatchIndirectCompute(
                                renderer.shaderManager.indirectShader[0],
                                faceDataBuffer ?: return false,
                                indirectBuffer ?: return false,
                                renderPosX,
                                renderPosY,
                                renderPosZ
                            )
                        }
                        1 -> {
                            drawIndirect(renderPosX, renderPosY, renderPosZ)
                        }
                        2 -> {
                            if (!boundingBoxBuffer.bind()) return false
                            drawBoundingBox(renderPosX, renderPosY, renderPosZ)
                        }
                        3 -> {
                            dispatchIndirectCompute(
                                renderer.shaderManager.indirectShader[1],
                                faceDataBuffer!!,
                                indirectBuffer!!,
                                renderPosX,
                                renderPosY,
                                renderPosZ
                            )
                        }
                        4 -> {
                            drawIndirect(renderPosX, renderPosY, renderPosZ)
                            frameCounter++
                        }
                        else -> return false
                    }

                }
            }

            return true
        }

        private fun drawBoundingBox(renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            checkMemBarrier(GL_SHADER_STORAGE_BARRIER_BIT)

            val shader = renderer.shaderManager.cullShader

            shader.bind()
            shader.setRegionOffset(
                (originX - renderPosX).toFloat(),
                (originY - renderPosY).toFloat(),
                (originZ - renderPosZ).toFloat()
            )
            shader.attachBuffer(GL_SHADER_STORAGE_BUFFER, visibleBuffer, "VisibleBuffer")

            glDisable(GL_CULL_FACE)
            glColorMask(false, false, false, false)
            glDepthMask(false)

            glDrawArraysInstanced(GL_TRIANGLES, 0, BoundingBoxBuffer.BOX_VERTEX_COUNT, boundingBoxBuffer.count)

            glEnable(GL_CULL_FACE)
            glColorMask(true, true, true, true)
            glDepthMask(true)

            setMemBarrierFlag(GL_SHADER_STORAGE_BARRIER_BIT)
        }

        private fun dispatchIndirectCompute(
            shader: TerrainShaderManager.TerrainShaderProgram,
            faceDataBuffer: BufferObject,
            indirectBuffer: BufferObject,
            renderPosX: Double,
            renderPosY: Double,
            renderPosZ: Double
        ) {
            checkMemBarrier(GL_SHADER_STORAGE_BARRIER_BIT)

            glClearNamedBufferData(indirectBuffer.id, GL_R32UI, GL_RED_INTEGER, GL_UNSIGNED_INT, null)

            shader.bind()
            shader.setRegionOffset(
                (originX - renderPosX).toFloat(),
                (originY - renderPosY).toFloat(),
                (originZ - renderPosZ).toFloat()
            )

            shader.attachBuffer(GL_SHADER_STORAGE_BUFFER, indirectBuffer, "IndirectBuffer", 0,  4 + count * 20)
            shader.attachBuffer(GL_SHADER_STORAGE_BUFFER, faceDataBuffer, "FaceDataBuffer", 0, count * 16)
            shader.attachBuffer(GL_SHADER_STORAGE_BUFFER, faceDataIndicesBuffer, "FaceDataIndicesBuffer")
            shader.attachBuffer(GL_SHADER_STORAGE_BUFFER, visibleBuffer, "VisibleBuffer")

            glDispatchCompute(1, storage.sizeY, 1)
            setMemBarrierFlag(GL_SHADER_STORAGE_BARRIER_BIT or GL_COMMAND_BARRIER_BIT)
        }

        private fun drawIndirect(renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            checkMemBarrier(GL_DRAW_INDIRECT_BUFFER)

            val indirectBuffer = indirectBuffer!!

            val shader = renderer.shaderManager.drawShader

            shader.bind()
            shader.setRegionOffset(
                (originX - renderPosX).toFloat(),
                (originY - renderPosY).toFloat(),
                (originZ - renderPosZ).toFloat()
            )

            indirectBuffer.bind(GL_DRAW_INDIRECT_BUFFER)

            vao.bind()
            glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 4L, count, 0)
        }

        override fun destroy() {
            visibleBuffer.destroy()
            faceDataIndicesBuffer.destroy()
            faceDataBuffer?.destroy()
            indirectBuffer?.destroy()

            clientFaceDataBuffer.free()
            clientFaceDataBuffer.free()
        }
    }

    inner class DefaultLayerBatch(private val storage: RenderChunkStorage, override val layerIndex: Int) : ILayerBatch {
        private var serverBuffer: BufferObject? = null
        private val cachedClientBuffer = CachedBuffer(storage.regionChunkCount * 20)
        private var index = 0
        private var isDirty = false

        override var count = 0; private set

        override fun startUpdate() {
            index = 0
            isDirty = true
            count = 0
        }

        override fun put(chunk: RenderChunk) {
            val layer = chunk.layers[layerIndex]
            val vertexRegion = layer.vertexRegion ?: return
            val indexRegion = layer.indexRegion ?: return
            layer.faceData?.addToBatch(
                this,
                vertexRegion.offset,
                indexRegion.offset,
                chunk.region.tempVisibleBits[chunk.regionIndex].toInt(),
                (chunk.originX and 255 shl 8)
                    or ((chunk.chunkY - storage.minChunkY) shl 20)
                    or (chunk.originZ and 255)
            )
        }

        fun put(vertexOffset: Int, indexOffset: Int, indexCount: Int, baseInstance: Int) {
            val clientBuffer = cachedClientBuffer.ensureCapacityByte(index + 20)
            val address = clientBuffer.address + index

            UNSAFE.putInt(address, indexCount)
            UNSAFE.putInt(address + 4L, 1)
            UNSAFE.putInt(address + 8L, indexOffset)
            UNSAFE.putInt(address + 12L, vertexOffset)
            UNSAFE.putInt(address + 16L, baseInstance)

            index += 20
            count++
        }

        override fun endUpdate() {
            if (isDirty && count != 0) {
                val clientBuffer = cachedClientBuffer.getByte()
                clientBuffer.limit(index)
                var buffer = serverBuffer
                if (buffer == null || buffer.size < clientBuffer.remaining() || buffer.size - clientBuffer.remaining() > 1024 * 1024) {
                    buffer?.destroy()
                    val newSize =
                        min(clientBuffer.remaining() * 2, storage.regionChunkCount * FaceData.MAX_COUNT * 20)
                    buffer = BufferObject.Immutable().allocate(newSize, GL_DYNAMIC_STORAGE_BIT)
                    glNamedBufferSubData(buffer.id, 0L, clientBuffer)
                    serverBuffer = buffer
                } else {
                    glInvalidateBufferData(buffer.id)
                    glNamedBufferSubData(buffer.id, 0L, clientBuffer)
                }
            }

            isDirty = false
        }

        override fun render(pass: Int, renderPosX: Double, renderPosY: Double, renderPosZ: Double): Boolean {
            if (!super.render(pass, renderPosX, renderPosY, renderPosZ)) {
                return false
            }

            when (pass) {
                0 -> {
                    val buffer = serverBuffer ?: return false

                    val shader = renderer.shaderManager.drawShader
                    shader.bind()
                    shader.setRegionOffset(
                        (originX - renderPosX).toFloat(),
                        (originY - renderPosY).toFloat(),
                        (originZ - renderPosZ).toFloat()
                    )

                    buffer.bind(GL_DRAW_INDIRECT_BUFFER)
                    vao.bind()
                    glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0L, count, 0)
                }
                else -> return false

            }
            return true
        }

        override fun destroy() {
            serverBuffer?.destroy()
            cachedClientBuffer.free()
        }
    }


    class BoundingBoxBuffer(storage: RenderChunkStorage) {
        private val vao = VertexArrayObject()
        private val serverBuffer = BufferObject.Immutable().allocate(storage.regionChunkCount * 8, GL_DYNAMIC_STORAGE_BIT)
        private val clientBuffer = allocateByte(storage.regionChunkCount * 8)

        init {
            vao.attachVbo(BOX_BUFFER, BOX_VERTEX_ARRTIBUTE)
            vao.attachVbo(serverBuffer, INSTANCE_ATTRIBUTE)
        }

        private var dirty = false
        private var index = 0
        var count = 0; private set
        val isEmpty get() = count == 0

        fun update() {
            index = 0
            count = 0
            dirty = true
        }

        fun put(renderChunk: RenderChunk) {
            val address = clientBuffer.address + index

            UNSAFE.putShort(address, renderChunk.regionIndex.toShort())
            UNSAFE.putByte(
                address + 2,
                (renderChunk.chunkX and 15).toByte()
            )
            UNSAFE.putByte(
                address + 3,
                (renderChunk.chunkY).toByte()
            )
            UNSAFE.putByte(
                address + 4,
                (renderChunk.chunkZ and 15).toByte()
            )

//            UNSAFE.putByte(
//                address + 2,
//                ((renderChunk.minX + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte()
//            )
//            UNSAFE.putByte(
//                address + 3,
//                ((renderChunk.minY + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte()
//            )
//            UNSAFE.putByte(
//                address + 4,
//                ((renderChunk.minZ + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte()
//            )
//
//            UNSAFE.putByte(
//                address + 5,
//                ((renderChunk.maxX - renderChunk.minX + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte()
//            )
//            UNSAFE.putByte(
//                address + 6,
//                ((renderChunk.maxY - renderChunk.minY + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte()
//            )
//            UNSAFE.putByte(
//                address + 7,
//                ((renderChunk.maxZ - renderChunk.minZ + 1.0f).coerceIn(0.0f, 18.0f) * 14.166667f).toInt().toByte()
//            )

            index += 8
            count++
        }

        fun bind(): Boolean {
            if (dirty && count != 0) {
                clientBuffer.limit(index)
                glInvalidateBufferData(serverBuffer.id)
                glNamedBufferSubData(serverBuffer.id, 0L, clientBuffer)
            }

            dirty = false

            if (count == 0) return false
            vao.bind()
            return true
        }

        fun destroy() {
            vao.destroyVao()
            serverBuffer.destroy()
            clientBuffer.free()
        }

        companion object {
            const val BOX_VERTEX_COUNT = 36

            @JvmField
            val BOX_BUFFER = run {
                MemoryStack.use {
                    withMalloc(144) {
                        // Down
                        it.put(0).put(0).put(0).skip(1)
                        it.put(1).put(0).put(1).skip(1)
                        it.put(0).put(0).put(1).skip(1)
                        it.put(0).put(0).put(0).skip(1)
                        it.put(1).put(0).put(0).skip(1)
                        it.put(1).put(0).put(1).skip(1)

                        // Up
                        it.put(0).put(1).put(1).skip(1)
                        it.put(1).put(1).put(0).skip(1)
                        it.put(0).put(1).put(0).skip(1)
                        it.put(0).put(1).put(1).skip(1)
                        it.put(1).put(1).put(1).skip(1)
                        it.put(1).put(1).put(0).skip(1)

                        // West
                        it.put(0).put(1).put(1).skip(1)
                        it.put(0).put(0).put(0).skip(1)
                        it.put(0).put(0).put(1).skip(1)
                        it.put(0).put(1).put(1).skip(1)
                        it.put(0).put(1).put(0).skip(1)
                        it.put(0).put(0).put(0).skip(1)

                        // East
                        it.put(1).put(1).put(0).skip(1)
                        it.put(1).put(0).put(1).skip(1)
                        it.put(1).put(0).put(0).skip(1)
                        it.put(1).put(1).put(0).skip(1)
                        it.put(1).put(1).put(1).skip(1)
                        it.put(1).put(0).put(1).skip(1)

                        // North
                        it.put(0).put(1).put(0).skip(1)
                        it.put(1).put(0).put(0).skip(1)
                        it.put(0).put(0).put(0).skip(1)
                        it.put(0).put(1).put(0).skip(1)
                        it.put(1).put(1).put(0).skip(1)
                        it.put(1).put(0).put(0).skip(1)

                        // South
                        it.put(1).put(1).put(1).skip(1)
                        it.put(0).put(0).put(1).skip(1)
                        it.put(1).put(0).put(1).skip(1)
                        it.put(1).put(1).put(1).skip(1)
                        it.put(0).put(1).put(1).skip(1)
                        it.put(0).put(0).put(1).skip(1)

                        it.flip()

                        BufferObject.Immutable().allocate(it, 0)
                    }
                }
            }

            @JvmField
            val BOX_VERTEX_ARRTIBUTE = buildAttribute(4) {
                float(0, 3, GLDataType.GL_BYTE, false)
                padding(1)
            }

            @JvmField
            val INSTANCE_ATTRIBUTE = buildAttribute(8, 1) {
                int(1, 1, GLDataType.GL_UNSIGNED_SHORT)
                float(2, 3, GLDataType.GL_UNSIGNED_BYTE, false)
                float(3, 3, GLDataType.GL_UNSIGNED_BYTE, false)
            }
        }
    }

    private inner class FrustumCullImpl : FrustumCull(renderer) {
        override fun isInFrustum(frustum: FrustumIntersection): Boolean {
            val x = (originX - renderer.renderPosX).toFloat()
            val y = (originY - renderer.renderPosY).toFloat()
            val z = (originZ - renderer.renderPosZ).toFloat()
            return frustum.testAab(x, y, z, x + 256.0f, y + (storage.sizeY shl 4), z + 256.0f)
        }
    }

    private companion object {
       private var memBarrierFlag = 0

        fun setMemBarrierFlag(bits: Int) {
            memBarrierFlag = memBarrierFlag or bits
        }

        fun checkMemBarrier(bits: Int) {
            val flag = memBarrierFlag
            if (flag and bits != 0) {
                glMemoryBarrier(bits)
                memBarrierFlag = flag and bits.inv()
            }
        }
    }
}