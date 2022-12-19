package dev.fastmc.graphics.renderer

import dev.fastmc.common.FastMcCoreScope
import dev.fastmc.graphics.shared.instancing.AbstractInstancingBuilder
import dev.fastmc.graphics.shared.instancing.tileentity.*
import dev.fastmc.graphics.shared.instancing.tileentity.info.IChestInfo
import dev.fastmc.graphics.shared.renderer.IRenderer
import dev.fastmc.graphics.shared.renderer.TileEntityRenderer
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.tileentity.ChestInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.block.BlockChest
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.tileentity.*
import org.lwjgl.opengl.GL11.*
import kotlin.coroutines.CoroutineContext

class TileEntityRendererImpl(private val mc: Minecraft, worldRenderer: WorldRenderer) :
    TileEntityRenderer<TileEntity>(worldRenderer) {
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
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        super.render()
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
            if (entity.hasWorld()) {
                val block = entity.blockType

                if (block is BlockChest && entity.blockMetadata == 0) {
                    block.checkForSurroundingChests(
                        entity.world,
                        entity.pos,
                        entity.world.getBlockState(entity.pos)
                    )
                }
            }

            if (entity.adjacentChestXNeg == null && entity.adjacentChestZNeg == null) {
                if (entity.adjacentChestXPos == null && entity.adjacentChestZPos == null) {
                    smallChest.add(entity)
                    smallDirty = true
                } else {
                    largeChest.add(entity)
                    largeDirty = true
                }
            }
        }

        override fun addAll(collection: Collection<TileEntityChest>) {
            collection.forEach {
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

                if (it.adjacentChestXNeg == null && it.adjacentChestZNeg == null) {
                    if (it.adjacentChestXPos == null && it.adjacentChestZPos == null) {
                        smallChest.add(it)
                        smallDirty = true
                    } else {
                        largeChest.add(it)
                        largeDirty = true
                    }
                }
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

        override suspend fun update(mainThreadContext: CoroutineContext, parentScope: CoroutineScope) {
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
                            smallChest.forEach {
                                it.checkForAdjacentChests()
                            }

                            val builder = SmallChestInstancingBuilder()
                            builder.init(this@TileEntityRendererImpl, smallChest.size)
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
                            largeChest.forEach {
                                it.checkForAdjacentChests()
                            }

                            val builder = LargeChestInstancingBuilder()
                            builder.init(this@TileEntityRendererImpl, largeChest.size)
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