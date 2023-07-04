package dev.fastmc.graphics.renderer

import dev.fastmc.graphics.shared.instancing.AbstractInstancingBuilder
import dev.fastmc.graphics.shared.instancing.tileentity.*
import dev.fastmc.graphics.shared.instancing.tileentity.info.IChestInfo
import dev.fastmc.graphics.shared.renderer.IRenderer
import dev.fastmc.graphics.shared.renderer.TileEntityInstancingRenderer
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.tileentity.ChestInfo
import dev.fastmc.graphics.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.block.enums.ChestType
import net.minecraft.util.math.Direction
import kotlin.coroutines.CoroutineContext

class TileEntityInstancingRendererImpl(private val mc: Minecraft, worldRenderer: WorldRenderer) :
    TileEntityInstancingRenderer<TileEntity>(worldRenderer) {

    init {
        register<TileEntityBed, BedInstancingBuilder>()
        register<TileEntityShulkerBox, ShulkerBoxInstancingBuilder>()
        register<TileEntityEnderChest, EnderChestInstancingBuilder>()

        register(ChestRenderEntry())
    }

    override fun onPostTick(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
        parentScope.launch(FastMcCoreScope.context) {
            mc.world?.let {
                coroutineScope {
                    for (entry in renderEntryList) {
                        launch(FastMcCoreScope.context) {
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

    override fun render() {
        mc.gameRenderer.lightmapTextureManager.enable()
        super.render()
        mc.gameRenderer.lightmapTextureManager.disable()
    }

    private inner class ChestRenderEntry : AbstractRenderEntry<TileEntityChest, ChestInfo>() {
        private var smallChestRenderer: AbstractInstancingBuilder.Renderer? = null
        private var largeChestRenderer: AbstractInstancingBuilder.Renderer? = null

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
            coroutineScope {
                val smallDirty = this@ChestRenderEntry.smallDirty
                this@ChestRenderEntry.smallDirty = false

                val largeDirty = this@ChestRenderEntry.largeDirty
                this@ChestRenderEntry.largeDirty = false

                if (smallDirty) {
                    if (smallChest.isEmpty()) {
                        withContext(mainThreadContext) {
                            smallChestRenderer?.destroy()
                            smallChestRenderer = null
                        }
                    } else {
                        launch(FastMcCoreScope.context) {
                            val builder = SmallChestInstancingBuilder()
                            builder.init(this@TileEntityInstancingRendererImpl, smallChest.size)
                            @Suppress("UNCHECKED_CAST")
                            builder.addAll(smallChest as List<IChestInfo<*>>)
                            withContext(mainThreadContext) {
                                smallChestRenderer?.destroy()
                                smallChestRenderer = builder.build()
                            }
                        }
                    }
                }

                if (largeDirty) {
                    if (largeChest.isEmpty()) {
                        withContext(mainThreadContext) {
                            largeChestRenderer?.destroy()
                            largeChestRenderer = null
                        }
                    } else {
                        launch(FastMcCoreScope.context) {
                            val builder = LargeChestInstancingBuilder()
                            builder.init(this@TileEntityInstancingRendererImpl, largeChest.size)
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
        }

        override fun destroyRenderer() {
            smallChestRenderer?.destroy()
            largeChestRenderer?.destroy()
            smallChestRenderer = null
            largeChestRenderer = null
            smallDirty = true
            largeDirty = true
        }

        override fun render(renderer: IRenderer) {
            smallChestRenderer?.render(renderer)
            largeChestRenderer?.render(renderer)
        }

        override fun markDirty() {
            smallDirty = true
            largeDirty = true
        }
    }
}