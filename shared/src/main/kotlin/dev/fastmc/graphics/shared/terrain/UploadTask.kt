package dev.fastmc.graphics.shared.terrain

import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.graphics.shared.instancing.tileentity.info.ITileEntityInfo
import dev.fastmc.graphics.shared.opengl.glCopyNamedBufferSubData

internal class UploadTask(
    private val parentTask: ChunkBuilderTask,
    private val bound: RenderChunk.Bound?,
    private val occlusionData: ChunkOcclusionData?,
    private val translucentData: TranslucentData?,
    private val modifyTranslucentData: Boolean,
    private val updates: Array<LayerUpdate?>,
    private val tileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?,
    private val instancingTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?,
    private val globalTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?,
    private val modifyTileEntity: Boolean,
) {
    @JvmField
    val renderChunk = parentTask.renderChunk

    private var cleared = false

    fun runClear(): Long {
        var newLength = 0L
        if (!parentTask.isCancelled) {
            for (i in updates.indices) {
                val update = updates[i]
                if (update != null) {
                    newLength += update.clear(parentTask, i)
                }
            }
            cleared = true
        }
        return newLength
    }

    fun runUpdate(): Boolean {
        if (cleared) {
            if (bound != null)  renderChunk.bound = bound
            if (occlusionData != null) renderChunk.occlusionData = occlusionData
            if (modifyTranslucentData) renderChunk.translucentData = translucentData
            if (modifyTileEntity) {
                renderChunk.tileEntityList = tileEntityList
                renderChunk.instancingTileEntityList = instancingTileEntityList
                renderChunk.globalTileEntityList = globalTileEntityList
            }
            for (i in updates.indices) {
                updates[i]?.update(parentTask, i)
            }
            renderChunk.onUpdate()
        }
        parentTask.onUpload()
        parentTask.onFinish()
        return cleared
    }

    fun cancel() {
        parentTask.onUpload()
        parentTask.onFinish()
    }

    class Builder(
        private val parentTask: ChunkBuilderTask,
    ) {
        private var bound: RenderChunk.Bound? = null
        private var occlusionData: ChunkOcclusionData? = null
        private var translucentData: TranslucentData? = null
        private var modifyTranslucentData = false
        private val updates = arrayOfNulls<LayerUpdate>(parentTask.renderer.layerCount)
        private var tileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null
        private var instancingTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null
        private var globalTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null
        private var modifyTileEntity = false

        fun bound(bound: RenderChunk.Bound) {
            this.bound = bound
        }

        fun occlusionData(data: ChunkOcclusionData?) {
            occlusionData = data
        }

        fun translucentData(data: TranslucentData?) {
            translucentData = data
            modifyTranslucentData = true
        }

        fun updateLayer(index: Int, vertexBuffer: BufferContext?, indexBuffer: BufferContext?, faceData: FaceData?) {
            if (indexBuffer != null) {
                if (vertexBuffer != null) {
                    updates[index] = LayerUpdate.UpdateAll(vertexBuffer, indexBuffer, faceData)
                } else {
                    updates[index] = LayerUpdate.UpdateIndex(indexBuffer, faceData)
                }
            } else {
                updates[index] = LayerUpdate.Clear
            }
        }

        fun updateTileEntity(
            tileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?,
            instancingTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?,
            globalTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>?
        ) {
            this.tileEntityList = tileEntityList
            this.instancingTileEntityList = instancingTileEntityList
            this.globalTileEntityList = globalTileEntityList
            modifyTileEntity = true
        }

        internal fun build(): UploadTask {
            return UploadTask(
                parentTask,
                bound,
                occlusionData,
                translucentData,
                modifyTranslucentData,
                updates,
                tileEntityList,
                instancingTileEntityList,
                globalTileEntityList,
                modifyTileEntity
            )
        }
    }

    internal sealed class LayerUpdate {
        abstract fun clear(task: ChunkBuilderTask, index: Int): Long
        abstract fun update(task: ChunkBuilderTask, index: Int)

        object Clear : LayerUpdate() {
            override fun clear(task: ChunkBuilderTask, index: Int): Long {
                val layer = task.renderChunk.layers[index]
                layer.vertexRegion = null
                layer.indexRegion = null
                return 0L
            }

            override fun update(task: ChunkBuilderTask, index: Int) {}
        }

        open class UpdateIndex(private val indexBuffer: BufferContext, private val faceData: FaceData?) :
            LayerUpdate() {
            override fun clear(task: ChunkBuilderTask, index: Int): Long {
                val layer = task.renderChunk.layers[index]
                val region = layer.indexRegion
                val updateSize = indexBuffer.region.buffer.remaining()

                return if (region != null) {
                    if (region.length != updateSize) {
                        layer.indexRegion = null
                        updateSize.toLong()
                    } else {
                        0L
                    }
                } else {
                    updateSize.toLong()
                }
            }

            override fun update(task: ChunkBuilderTask, index: Int) {
                val updateSize = indexBuffer.region.buffer.remaining()
                val layer = task.renderChunk.layers[index]
                var region = layer.indexRegion

                if (region == null) {
                    region = task.renderChunk.renderRegion.indexBufferPool.allocate(updateSize)
                    layer.indexRegion = region
                } else {
                    region.invalidate()
                }

                glCopyNamedBufferSubData(
                    indexBuffer.region.vboID,
                    region.bufferObjectID,
                    indexBuffer.region.offset.toLong(),
                    region.offset.toLong(),
                    updateSize.toLong()
                )

                if (faceData != null) {
                    layer.faceData = faceData
                }

                indexBuffer.release(task)
            }
        }


        class UpdateAll(private val vertexBuffer: BufferContext, indexBuffer: BufferContext, faceData: FaceData?) :
            UpdateIndex(indexBuffer, faceData) {
            override fun clear(task: ChunkBuilderTask, index: Int): Long {
                val layer = task.renderChunk.layers[index]
                val region = layer.vertexRegion
                val updateSize = vertexBuffer.region.buffer.remaining()

                val vertexUpdateSize = if (region != null) {
                    if (region.length != updateSize) {
                        layer.vertexRegion = null
                        updateSize.toLong()
                    } else {
                        0L
                    }
                } else {
                    updateSize.toLong()
                }

                return (vertexUpdateSize shl 32) or super.clear(task, index)
            }

            override fun update(task: ChunkBuilderTask, index: Int) {
                super.update(task, index)

                val updateSize = vertexBuffer.region.buffer.remaining()
                val layer = task.renderChunk.layers[index]
                var region = layer.vertexRegion

                if (region == null) {
                    region = task.renderChunk.renderRegion.vertexBufferPool.allocate(updateSize)
                    layer.vertexRegion = region
                } else {
                    region.invalidate()
                }

                glCopyNamedBufferSubData(
                    vertexBuffer.region.vboID,
                    region.bufferObjectID,
                    vertexBuffer.region.offset.toLong(),
                    region.offset.toLong(),
                    updateSize.toLong()
                )

                vertexBuffer.release(task)
            }
        }
    }
}