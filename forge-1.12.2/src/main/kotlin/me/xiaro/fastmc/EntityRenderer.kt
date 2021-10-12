package me.xiaro.fastmc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.xiaro.fastmc.opengl.glBindTexture
import me.xiaro.fastmc.opengl.glBindVertexArray
import me.xiaro.fastmc.opengl.glUniform1f
import me.xiaro.fastmc.opengl.glUseProgramForce
import me.xiaro.fastmc.resource.IResourceManager
import me.xiaro.fastmc.resource.ResourceManager
import me.xiaro.fastmc.tileentity.*
import me.xiaro.fastmc.tileentity.info.ITileEntityInfo
import me.xiaro.fastmc.utils.MathUtils
import me.xiaro.fastmc.utils.MatrixUtils
import me.xiaro.fastmc.utils.partialTicks
import net.minecraft.block.BlockChest
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityChest
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0

class EntityRenderer(private val mc: Minecraft, resourceManager: IResourceManager) :
    AbstractEntityRenderer(resourceManager) {
    private val renderEntryMap = HashMap<Class<out TileEntity>, IRenderEntry<TileEntity, *>>()
    private val renderEntryList = ArrayList<IRenderEntry<TileEntity, *>>()

    init {
        register(BedInfo::class.java) {
            BedRenderBuilder(resourceManager, renderPosX, renderPosY, renderPosZ, it)
        }
        register(EnderChestInfo::class.java) {
            EnderChestRenderBuilder(resourceManager, renderPosX, renderPosY, renderPosZ, it)
        }
        register(ShulkerBoxInfo::class.java) {
            ShulkerBoxRenderBuilder(resourceManager, renderPosX, renderPosY, renderPosZ, it)
        }

        addRenderEntry(ChestRenderEntry())
    }

    private inline fun <reified E : TileEntity, T : TileEntityInfo<E>> register(
        clazz: Class<T>,
        noinline newBuilder: (Int) -> TileEntityRenderBuilder<in T>
    ) {
        val tileEntityClass = E::class.java
        if (!renderEntryMap.containsKey(tileEntityClass)) {
            addRenderEntry(RenderEntry(clazz, newBuilder))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified E : TileEntity> addRenderEntry(renderEntry: IRenderEntry<out TileEntity, out TileEntityInfo<E>>) {
        renderEntryMap[E::class.java] = renderEntry as IRenderEntry<TileEntity, *>
        renderEntryList.add(renderEntry)
    }

    override fun onPostTick() {
        mc.profiler.startSection("tileEntityRenderer")

        renderEntryList.forEach {
            it.clear()
        }

        mc.world?.let { world ->
            world.loadedTileEntityList
                .groupBy {
                    it::class.java
                }.forEach { (clazz, tileEntities) ->
                    renderEntryMap[clazz]?.addAll(tileEntities)
                }

            updateRenderers()
        } ?: run {
            renderEntryList.forEach {
                it.destroyRenderer()
            }
        }

        mc.profiler.endSection()
    }

    override fun preRender() {
        mc.profiler.startSection("pre")

        glUseProgramForce(0)

        val partialTicks = mc.partialTicks

        MatrixUtils.putMatrix(projectionMatrix)

        resourceManager.tileEntityShader.resources.forEach {
            it.bind()
            glUniform1f(it.partialTicksUniform, partialTicks)
            it.updateProjectionMatrix(MatrixUtils.matrixBuffer)
        }

        val entity = mc.renderViewEntity ?: mc.player
        if (entity != null) {
            renderPosX = MathUtils.lerp(entity.lastTickPosX, entity.posX, partialTicks)
            renderPosY = MathUtils.lerp(entity.lastTickPosY, entity.posY, partialTicks)
            renderPosZ = MathUtils.lerp(entity.lastTickPosZ, entity.posZ, partialTicks)
        }

        mc.profiler.endSection()
    }

    override fun renderTileEntities() {
        mc.profiler.startSection("render")

        GlStateManager.disableCull()

        glGetFloat(GL_MODELVIEW_MATRIX, MatrixUtils.matrixBuffer)

        renderEntryList.forEach {
            it.render(modelViewMatrix, renderPosX, renderPosY, renderPosZ)
        }

        mc.profiler.endSection()
    }

    override fun renderEntities() {

    }

    override fun postRender() {
        mc.profiler.startSection("post")

        glBindVertexArray(0)
        glBindTexture(0)
        glUseProgramForce(0)

        mc.profiler.endSection()
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun updateRenderers() {
        runBlocking {
            val actor = actor<() -> Unit> {
                for (block in channel) {
                    block.invoke()
                }
            }

            coroutineScope {
                for (entry in renderEntryList) {
                    entry.update(this, actor)
                }
            }

            actor.close()
        }
    }

    private interface IRenderEntry<E : TileEntity, T : ITileEntityInfo> {
        fun clear()

        fun add(tileEntity: E)

        fun addAll(list: Collection<E>)

        fun remove(tileEntity: E)

        fun update(scope: CoroutineScope, actor: SendChannel<() -> Unit>)

        fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double)

        fun destroyRenderer()
    }

    private class RenderEntry<E : TileEntity, T : TileEntityInfo<E>>(
        private val tileEntityInfoClass: Class<T>,
        private val newBuilder: (Int) -> TileEntityRenderBuilder<in T>,
    ) : IRenderEntry<E, T> {
        private var renderer: TileEntityRenderBuilder.Renderer? = null
        private val tileEntities = ArrayList<E>()
        private var dirty = false

        override fun clear() {
            if (tileEntities.isNotEmpty()) {
                tileEntities.clear()
                dirty = true
            }
        }

        override fun add(tileEntity: E) {
            tileEntities.add(tileEntity)
            dirty = true
        }

        override fun addAll(list: Collection<E>) {
            tileEntities.addAll(list)
            dirty = true
        }

        override fun remove(tileEntity: E) {
            dirty = tileEntities.remove(tileEntity) || dirty
        }

        override fun update(
            scope: CoroutineScope,
            actor: SendChannel<() -> Unit>
        ) {
            if (!dirty) return

            if (tileEntities.isEmpty()) {
                destroyRenderer()
            } else {
                dirty = false
                scope.launch {
                    val builder = newBuilder.invoke(tileEntities.size)
                    val entityInfo = tileEntityInfoClass.newInstance()

                    tileEntities.forEach {
                        entityInfo.tileEntity = it
                        builder.add(entityInfo)
                    }

                    actor.send {
                        renderer = builder.build()
                    }
                }
            }
        }

        override fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            renderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
        }

        override fun destroyRenderer() {
            renderer?.destroy()
            renderer = null
            dirty = false
        }
    }

    private inner class ChestRenderEntry : IRenderEntry<TileEntityChest, ChestInfo> {
        private var smallChestRenderer: TileEntityRenderBuilder.Renderer? = null
        private var largeChestRenderer: TileEntityRenderBuilder.Renderer? = null

        private val smallChest = ArrayList<TileEntityChest>()
        private val largeChest = ArrayList<TileEntityChest>()

        private var smallDirty = false
        private var largeDirty = false

        override fun clear() {
            if (smallChest.isNotEmpty()) {
                smallChest.clear()
                smallDirty = true
            }

            if (largeChest.isNotEmpty()) {
                largeChest.clear()
                largeDirty = true
            }
        }

        override fun add(tileEntity: TileEntityChest) {
            if (tileEntity.hasWorld()) {
                val block = tileEntity.blockType

                if (block is BlockChest && tileEntity.blockMetadata == 0) {
                    block.checkForSurroundingChests(
                        tileEntity.world,
                        tileEntity.pos,
                        tileEntity.world.getBlockState(tileEntity.pos)
                    )
                }
            }

            tileEntity.checkForAdjacentChests()

            if (tileEntity.adjacentChestXNeg == null && tileEntity.adjacentChestZNeg == null) {
                if (tileEntity.adjacentChestXPos == null && tileEntity.adjacentChestZPos == null) {
                    smallChest.add(tileEntity)
                    smallDirty = true
                } else {
                    largeChest.add(tileEntity)
                    largeDirty = true
                }
            }
        }

        override fun addAll(list: Collection<TileEntityChest>) {
            val smallChestList = ArrayList<TileEntityChest>()
            val largeChestList = ArrayList<TileEntityChest>()

            list.forEach {
                if (it.hasWorld()) {
                    val block = it.blockType

                    if (block is BlockChest && it.blockMetadata == 0) {
                        block.checkForSurroundingChests(
                            it.world,
                            it.pos,
                            it.world.getBlockState(it.pos)
                        )
                    }
                }

                it.checkForAdjacentChests()

                if (it.adjacentChestXNeg == null && it.adjacentChestZNeg == null) {
                    if (it.adjacentChestXPos == null && it.adjacentChestZPos == null) {
                        smallChestList.add(it)
                        smallDirty = true
                    } else {
                        largeChestList.add(it)
                        largeDirty = true
                    }
                }
            }

            smallChest.addAll(smallChestList)
            largeChest.addAll(largeChestList)
        }

        override fun remove(tileEntity: TileEntityChest) {
            smallDirty = smallChest.remove(tileEntity) || smallDirty
            largeDirty = largeChest.remove(tileEntity) || largeDirty
        }

        override fun update(
            scope: CoroutineScope,
            actor: SendChannel<() -> Unit>
        ) {
            if (smallDirty) {
                if (smallChest.isEmpty()) {
                    smallChestRenderer?.destroy()
                    smallChestRenderer = null
                } else {
                    smallDirty = false
                    scope.launch {
                        val builder = SmallChestRenderBuilder(
                            resourceManager,
                            renderPosX,
                            renderPosY,
                            renderPosZ,
                            smallChest.size
                        )
                        val entityInfo = ChestInfo()

                        smallChest.forEach {
                            entityInfo.tileEntity = it
                            builder.add(entityInfo)
                        }

                        actor.send {
                            smallChestRenderer = builder.build()
                        }
                    }
                }
            }

            if (largeDirty) {
                if (largeChest.isEmpty()) {
                    largeChestRenderer?.destroy()
                    largeChestRenderer = null
                    largeDirty = false
                } else {
                    largeDirty = false
                    scope.launch {
                        val builder = LargeChestRenderBuilder(
                            resourceManager,
                            renderPosX,
                            renderPosY,
                            renderPosZ,
                            largeChest.size
                        )
                        val entityInfo = ChestInfo()

                        largeChest.forEach {
                            entityInfo.tileEntity = it
                            builder.add(entityInfo)
                        }

                        actor.send {
                            largeChestRenderer = builder.build()
                        }
                    }
                }
            }
        }

        override fun destroyRenderer() {
            smallChestRenderer?.destroy()
            largeChestRenderer?.destroy()
            smallChestRenderer = null
            largeChestRenderer = null
            smallDirty = false
        }

        override fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            smallChestRenderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
            largeChestRenderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
        }
    }
}