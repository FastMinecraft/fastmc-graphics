package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.Cancellable
import dev.fastmc.common.collection.FastObjectArrayList
import dev.luna5ama.kmogus.MutableArr
import dev.luna5ama.kmogus.memcpy
import dev.luna5ama.kmogus.usePtr
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator

sealed class ChunkBuilderTask(val renderer: TerrainRenderer, private val factory: ChunkBuilder.TaskFactory) :
    Cancellable, Runnable {
    @JvmField
    internal var renderChunkNullable: RenderChunk? = null
    val renderChunk get() = renderChunkNullable!!

    private val id0 = AtomicInteger(nextId())
    val id get() = id0.get()

    private var thread: Thread? = null
    private val resources = FastObjectArrayList<Context>()

    var relativeCameraX = Float.MAX_VALUE; private set
    var relativeCameraY = Float.MAX_VALUE; private set
    var relativeCameraZ = Float.MAX_VALUE; private set

    var originX = Int.MAX_VALUE; private set
    var originY = Int.MAX_VALUE; private set
    var originZ = Int.MAX_VALUE; private set

    val chunkX get() = originX shr 4
    val chunkY get() = originY shr 4
    val chunkZ get() = originZ shr 4

    private val cancelState = AtomicBoolean(false)

    override val isCancelled get() = (renderChunkNullable?.isDestroyed ?: false) || (cancelState.get())

    internal fun init(renderChunk: RenderChunk): Boolean {
        return try {
            init0(renderChunk)
            true
        } catch (e: CancellationException) {
            reset()
            false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    protected open fun init0(renderChunk: RenderChunk) {
        this.cancelState.set(false)
        this.renderChunkNullable = renderChunk
        id0.set(nextId())

        relativeCameraX = (renderer.camera.posX - renderChunk.originX).toFloat()
        relativeCameraY = (renderer.camera.posY - renderChunk.originY).toFloat()
        relativeCameraZ = (renderer.camera.posZ - renderChunk.originZ).toFloat()

        originX = renderChunk.originX
        originY = renderChunk.originY
        originZ = renderChunk.originZ
    }

    open fun reset() {
        this.cancelState.set(false)
        this.renderChunkNullable = null

        resources.clear()

        relativeCameraX = Float.MAX_VALUE
        relativeCameraY = Float.MAX_VALUE
        relativeCameraZ = Float.MAX_VALUE

        originX = Int.MAX_VALUE
        originY = Int.MAX_VALUE
        originZ = Int.MAX_VALUE
    }

    internal fun registerResource(resource: Context) {
        resources.add(resource)
    }

    internal fun releaseResource() {
        for (i in resources.indices) {
            resources[i].release(this@ChunkBuilderTask)
        }
    }

    override fun run() {
        val renderChunk = renderChunk
        factory.onTaskStart(this@ChunkBuilderTask, renderChunk)
        try {
            checkCancelled()
            run0()
        } catch (e: TaskFinishedException) {
            //
        } catch (e: CancellationException) {
            onFinish()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    abstract fun run0()

    internal fun onUpload() {
        factory.onTaskUpload()
    }

    internal fun onFinish() {
        factory.onTaskFinish(this)
    }

    fun checkCancelled() {
        if (cancelState.get()) {
            throw CancellationException()
        }
    }

    internal fun cancel() {
        cancelState.set(true)
    }

    internal fun finish() {
        throw TaskFinishedException
    }

    protected fun TranslucentData?.writeElementIndices(bufferContext: BufferContext, quadCountIn: Int): BufferContext {
        val region = bufferContext.region
        val arr = region.arr

        val quadCount = if (this != null) quadIndices.size else quadCountIn
        val elementIndicesLen = quadCount * 6L * 4L

        while (arr.rem < elementIndicesLen) {
            region.expand(this@ChunkBuilderTask)
        }

        if (this != null) {
            val quadIndices = quadIndices

            for (i in 0 until quadCount) {
                val index = quadIndices[i] * 4
                arr.usePtr {
                    setIntInc(index)
                        .setIntInc(index + 1)
                        .setIntInc(index + 2)
                        .setIntInc(index + 2)
                        .setIntInc(index + 3)
                        .setIntInc(index + 0)
                }
            }
        } else {
            for (i in 0 until quadCount) {
                val index = i * 4
                arr.usePtr {
                    setIntInc(index)
                        .setIntInc(index + 1)
                        .setIntInc(index + 2)
                        .setIntInc(index + 2)
                        .setIntInc(index + 3)
                        .setIntInc(index + 0)
                }
            }
        }

        return bufferContext
    }

    protected fun TranslucentData?.writeElementIndices(quadCount: Int): BufferContext {
        val bufferContext = renderer.contextProvider.getBufferContext(this@ChunkBuilderTask)
        return writeElementIndices(bufferContext, quadCount)
    }

    protected object CancelInitException : CancellationException() {
        init {
            stackTrace = emptyArray()
        }
    }

    private object TaskFinishedException : RuntimeException() {
        init {
            stackTrace = emptyArray()
        }
    }

    companion object {
        private val idCounter = AtomicInteger(Int.MIN_VALUE + 1)
        private val updateFunc = IntUnaryOperator {
            var value = it + 1
            if (value == Int.MIN_VALUE) {
                value++
            }
            value
        }

        @JvmStatic
        private fun nextId(): Int {
            return idCounter.getAndUpdate(updateFunc)
        }
    }
}

abstract class RebuildTask(renderer: TerrainRenderer, scheduler: ChunkBuilder.TaskFactory) :
    ChunkBuilderTask(renderer, scheduler) {
    final override fun run0() {
        val rebuildContext = renderer.contextProvider.getRebuildContext(this@RebuildTask)

        if (rebuildContext.worldSnapshot.init()) {
            rebuildContext.renderChunk(this@RebuildTask)

            val bufferGroupArray =
                Array(rebuildContext.vertexBuilderArray.size) { i ->
                    rebuildContext.vertexBuilderArray[i].finish()
                }

            val occlusionData = rebuildContext.occlusionDataBuilder.build()
            val tileEntityList = rebuildContext.tileEntityList.copyOrNull()
            val instancingTileEntityList = rebuildContext.instancingTileEntityList.copyOrNull()
            val globalTileEntityList = rebuildContext.globalTileEntityList.copyOrNull()

            val translucentBufferPair = bufferGroupArray[1]
            val translucentData = if (translucentBufferPair != null) {
                val quadCount = rebuildContext.translucentVertexBuilder.posArrayList.size / 12
                val quadCenterArray = rebuildContext.translucentVertexBuilder.getQuadCenterArray()
                rebuildContext.release(this@RebuildTask)

                val sortContext = renderer.contextProvider.getSortContext(this@RebuildTask)
                val data = sortContext.sortQuads(this@RebuildTask, quadCenterArray, quadCount)
                sortContext.release(this@RebuildTask)

                data
            } else {
                rebuildContext.release(this@RebuildTask)
                null
            }

            renderer.chunkBuilder.scheduleUpload(this@RebuildTask) {
                occlusionData(occlusionData)
                translucentData(translucentData)
                updateTileEntity(tileEntityList, instancingTileEntityList, globalTileEntityList)

                for (i in bufferGroupArray.indices) {
                    val bufferGroup = bufferGroupArray[i]
                    var faceData: FaceData? = null
                    var vertexBuffer: BufferContext? = null
                    var indexBuffer: BufferContext? = null

                    if (bufferGroup != null) {
                        combineBuffers(if (i == 1) translucentData else null, bufferGroup)?.let {
                            faceData = it.first
                            vertexBuffer = it.second
                            indexBuffer = it.third
                        }
                    }

                    updateLayer(i, vertexBuffer, indexBuffer, faceData)
                }
            }
        } else {
            rebuildContext.release(this@RebuildTask)

            renderer.chunkBuilder.scheduleUpload(this@RebuildTask) {
                occlusionData(null)
                translucentData(null)
                for (i in 0 until renderer.layerCount) {
                    updateLayer(i, null, null, null)
                }
            }
        }
    }

    private fun combineBuffers(
        translucentData: TranslucentData?,
        bufferGroup: TerrainMeshBuilder.BufferGroup
    ): Triple<FaceData, BufferContext, BufferContext>? {
        return if (bufferGroup.vertexBuffers.size == 1) {
            val quadCount = bufferGroup.quadCounts[0]
            val vertexBuffer = bufferGroup.vertexBuffers[0]!!
            val elementBuffer = translucentData.writeElementIndices(quadCount)
            elementBuffer.region.arr.flip()
            Triple(FaceData.Singleton(elementBuffer.region.arr.rem.toInt()), vertexBuffer, elementBuffer)
        } else {
            var dataArray: IntArray? = null

            var resultVertexBuffer: BufferContext? = null
            var resultElementBuffer: BufferContext? = null

            for (i in bufferGroup.vertexBuffers.indices) {
                val vertexBuffer = bufferGroup.vertexBuffers[i] ?: continue
                val faceQuadCount = bufferGroup.quadCounts[i]

                val vertArr = vertexBuffer.region.arr
                val vertexLength = vertArr.rem
                val indexCount = faceQuadCount * 6
                val indexLength = indexCount * 4

                val resultVertArr: MutableArr
                val resultElemArr: MutableArr

                if (resultVertexBuffer == null) {
                    vertArr.reset()
                    resultVertexBuffer = vertexBuffer
                    resultVertArr = vertArr

                    resultElementBuffer = translucentData.writeElementIndices(faceQuadCount)
                    resultElemArr = resultElementBuffer.region.arr
                } else {
                    resultVertArr = resultVertexBuffer.region.arr
                    resultElemArr = resultElementBuffer!!.region.arr

                    while (resultVertArr.rem < vertexLength) {
                        resultVertexBuffer.region.expand(this)
                    }

                    memcpy(vertArr.ptr, resultVertArr.ptr, vertexLength)
                    translucentData.writeElementIndices(resultElementBuffer, faceQuadCount)

                    vertexBuffer.release(this)
                }

                if (dataArray == null) {
                    dataArray = IntArray(63 * 3) { -1 }
                }

                val dataIndex = i * 3
                dataArray[dataIndex] = resultVertArr.pos.toInt()
                dataArray[dataIndex + 1] = (resultElemArr.pos - indexLength).toInt()
                dataArray[dataIndex + 2] = indexLength

                resultVertArr.pos += vertexLength
            }

            if (dataArray != null) {
                resultVertexBuffer!!.region.arr.flip()
                resultElementBuffer!!.region.arr.flip()
                Triple(FaceData.Multiple(dataArray), resultVertexBuffer, resultElementBuffer)
            } else {
                null
            }
        }
    }

    private inline fun <reified E> FastObjectArrayList<E>.copyOrNull(): FastObjectArrayList<E>? {
        return if (this.isNotEmpty()) {
            val list = FastObjectArrayList<E>(this.size)
            list.addAll(this)
            list
        } else {
            null
        }
    }
}

class SortTask(renderer: TerrainRenderer, scheduler: ChunkBuilder.TaskFactory) :
    ChunkBuilderTask(renderer, scheduler) {
    private var data: TranslucentData? = null

    override fun init0(renderChunk: RenderChunk) {
        super.init0(renderChunk)
        data = renderChunk.translucentData ?: throw CancelInitException
    }

    override fun reset() {
        super.reset()
        data = null
    }

    override fun run0() {
        val sortContext = renderer.contextProvider.getSortContext(this@SortTask)
        val newData = sortContext.sortQuads(this@SortTask, data!!)
        sortContext.release(this@SortTask)

        val bufferContext = newData.writeElementIndices(newData.quadCount)
        bufferContext.region.arr.flip()

        renderer.chunkBuilder.scheduleUpload(this) {
            translucentData(newData)
            updateLayer(1, null, bufferContext, null)
        }
    }
}