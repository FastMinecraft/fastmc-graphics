package me.luna.fastmc.shared.terrain

import me.luna.fastmc.shared.opengl.glCopyNamedBufferSubData
import me.luna.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo
import me.luna.fastmc.shared.util.collection.FastObjectArrayList

internal class UploadTask(
    private val parentTask: ChunkBuilderTask,
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
    val updateSize: Int

    @JvmField
    val renderChunk = parentTask.renderChunk

    private var cleared = false

    init {
        var sum = 0
        for (update in updates) {
            if (update == null) continue
            sum += update.updateSize
        }
        updateSize = sum
    }

    fun runClear() {
        if (!parentTask.isCancelled) {
            for (i in updates.indices) {
                updates[i]?.clear(parentTask, i)
            }
            cleared = true
        }
    }

    fun runUpdate(): Boolean {
        if (cleared) {
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
        private var occlusionData: ChunkOcclusionData? = null
        private var translucentData: TranslucentData? = null
        private var modifyTranslucentData = false
        private val updates = arrayOfNulls<LayerUpdate>(parentTask.renderer.layerCount)
        private var tileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null
        private var instancingTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null
        private var globalTileEntityList: FastObjectArrayList<ITileEntityInfo<*>>? = null
        private var modifyTileEntity = false

        fun occlusionData(data: ChunkOcclusionData?) {
            occlusionData = data
        }

        fun translucentData(data: TranslucentData?) {
            translucentData = data
            modifyTranslucentData = true
        }

        fun updateLayer(index: Int, bufferContext: BufferContext?) {
            if (bufferContext != null) {
                updates[index] = LayerUpdateReplace(bufferContext)
            } else {
                updates[index] = LayerUpdateClear
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

    internal sealed class LayerUpdate(val updateSize: Int) {
        fun clear(task: ChunkBuilderTask, index: Int) {
            val data = task.renderChunk.layers[index]
            if (data != null) {
                data.region.release()
                task.renderChunk.layers[index] = null
            }
        }

        abstract fun update(task: ChunkBuilderTask, index: Int)
    }

    private object LayerUpdateClear : LayerUpdate(0) {
        override fun update(task: ChunkBuilderTask, index: Int) {}
    }

    private class LayerUpdateReplace(private val bufferContext: BufferContext) :
        LayerUpdate(bufferContext.region.buffer.remaining()) {
        override fun update(task: ChunkBuilderTask, index: Int) {
            val region = task.renderChunk.renderRegion.bufferPool.allocate(updateSize)
            glCopyNamedBufferSubData(
                bufferContext.region.vboID,
                region.vboID,
                bufferContext.region.offset.toLong(),
                region.offset.toLong(),
                updateSize.toLong()
            )
            task.renderChunk.layers[index] = RenderChunk.Layer(region)
            bufferContext.release(task)
        }
    }
}