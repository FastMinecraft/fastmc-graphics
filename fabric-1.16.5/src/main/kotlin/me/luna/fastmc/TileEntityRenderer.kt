package me.luna.fastmc

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.*
import me.luna.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.luna.fastmc.shared.renderbuilder.tileentity.LargeChestRenderBuilder
import me.luna.fastmc.shared.renderbuilder.tileentity.SmallChestRenderBuilder
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IChestInfo
import me.luna.fastmc.shared.renderer.AbstractTileEntityRenderer
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer
import me.luna.fastmc.tileentity.ChestInfo
import me.luna.fastmc.util.blockState
import me.luna.fastmc.util.getPropertyOrDefault
import net.minecraft.block.enums.ChestType
import net.minecraft.util.math.Direction
import org.joml.Matrix4f
import kotlin.coroutines.CoroutineContext

class TileEntityRenderer(private val mc: Minecraft, worldRenderer: AbstractWorldRenderer) :
    AbstractTileEntityRenderer<TileEntity>(worldRenderer) {

    init {
//        register(BedRenderBuilder::class.java)
//        register(EnderChestRenderBuilder::class.java)
//        register(ShulkerBoxRenderBuilder::class.java)

        register(ChestRenderEntry())
    }

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        parentScope.launch(Dispatchers.Default) {
            launch(Dispatchers.Default) {
                renderEntryList.forEach {
                    it.clear()
                }

                mc.world?.let { world ->
//            world.loadedTileEntityList
//                .groupBy {
//                    it::class.java
//                }.forEach { (clazz, tileEntities) ->
//                    renderEntryMap[clazz]?.addAll(tileEntities)
//                }

                    coroutineScope {
                        for (entry in renderEntryList) {
                            launch(Dispatchers.Default) {
                                entry.markDirty()
                                entry.update(mainThreadContext, this)
                            }
                        }
                    }
                } ?: run {
                    withContext(mainThreadContext) {
                        renderEntryList.forEach {
                            it.destroyRenderer()
                        }
                    }
                }
            }
        }
    }

    override fun render() {
        mc.profiler.startSection("render")

        mc.gameRenderer.lightmapTextureManager.enable()
        RenderSystem.disableCull()
        RenderSystem.enableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.enableAlphaTest()

        super.render()

        mc.profiler.endSection()
    }

    private inner class ChestRenderEntry : AbstractRenderEntry<TileEntityChest, ChestInfo>() {
        private var smallChestRenderer: AbstractRenderBuilder.Renderer? = null
            set(value) {
                field?.destroy()
                field = value
            }

        private var largeChestRenderer: AbstractRenderBuilder.Renderer? = null
            set(value) {
                field?.destroy()
                field = value
            }

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

        override fun add(entity: TileEntityChest) {
            val blockState = entity.blockState
            val chestType = blockState.getPropertyOrDefault(BlockChest.CHEST_TYPE, ChestType.SINGLE)

            when (chestType) {
                ChestType.LEFT -> {
                    if (blockState.getPropertyOrDefault(BlockChest.FACING, Direction.SOUTH).horizontal >= 2) {
                        largeChest.add(entity)
                        largeDirty = true
                    }
                }
                ChestType.RIGHT -> {
                    if (blockState.getPropertyOrDefault(BlockChest.FACING, Direction.SOUTH).horizontal < 2) {
                        largeChest.add(entity)
                        largeDirty = true
                    }
                }
                else -> {
                    smallChest.add(entity)
                    smallDirty = true
                }
            }
        }

        override fun addAll(collection: Collection<TileEntityChest>) {
            collection.forEach {
                add(it)
            }
        }

        override fun remove(entity: TileEntityChest): Boolean {
            val smallRemoved = smallChest.remove(entity)
            val largeRemoved = largeChest.remove(entity)
            smallDirty = smallRemoved || smallDirty
            largeDirty = largeRemoved || largeDirty
            return smallRemoved || largeRemoved
        }

        @Suppress("ConvertArgumentToSet")
        override fun removeAll(collection: Collection<TileEntityChest>) {
            smallDirty = smallChest.removeAll(collection) || smallDirty
            largeDirty = largeChest.removeAll(collection) || largeDirty
        }

        override suspend fun update(
            mainThreadContext: CoroutineContext,
            parentScope: CoroutineScope
        ) {
            parentScope.launch(Dispatchers.Default) {
                if (smallDirty) {
                    smallDirty = false

                    if (smallChest.isEmpty()) {
                        withContext(mainThreadContext) {
                            smallChestRenderer = null
                        }
                    } else {
                        val builder = SmallChestRenderBuilder()
                        builder.init(this@TileEntityRenderer, smallChest.size)
                        @Suppress("UNCHECKED_CAST")
                        builder.addAll(smallChest as List<IChestInfo<*>>)
                        withContext(mainThreadContext) {
                            smallChestRenderer?.destroy()
                            smallChestRenderer = builder.build()
                        }
                    }
                }
            }

            parentScope.launch(Dispatchers.Default) {
                if (largeDirty) {
                    largeDirty = false

                    if (largeChest.isEmpty()) {
                        withContext(mainThreadContext) {
                            largeChestRenderer = null
                        }
                    } else {
                        largeDirty = false
                        val builder = LargeChestRenderBuilder()
                        builder.init(this@TileEntityRenderer, largeChest.size)
                        @Suppress("UNCHECKED_CAST")
                        builder.addAll(largeChest as List<IChestInfo<*>>)
                        withContext(mainThreadContext) {
                            largeChestRenderer?.destroy()
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
            smallDirty = true
            largeDirty = true
        }

        override fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            smallChestRenderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
            largeChestRenderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
        }

        override fun markDirty() {
            smallDirty = true
            largeDirty = true
        }
    }
}