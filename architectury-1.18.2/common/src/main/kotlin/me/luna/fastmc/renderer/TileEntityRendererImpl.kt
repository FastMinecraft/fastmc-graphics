package me.luna.fastmc.renderer

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.luna.fastmc.shared.opengl.GL_TEXTURE0
import me.luna.fastmc.shared.opengl.GL_TEXTURE2
import me.luna.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.luna.fastmc.shared.renderbuilder.tileentity.*
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IChestInfo
import me.luna.fastmc.shared.renderer.TileEntityRenderer
import me.luna.fastmc.shared.renderer.IRenderer
import me.luna.fastmc.shared.renderer.WorldRenderer
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.tileentity.ChestInfo
import me.luna.fastmc.util.*
import net.minecraft.block.enums.ChestType
import net.minecraft.util.math.Direction
import kotlin.coroutines.CoroutineContext

class TileEntityRendererImpl(private val mc: Minecraft, worldRenderer: WorldRenderer) :
    TileEntityRenderer<TileEntity>(worldRenderer) {

    init {
        register<TileEntityBed, BedRenderBuilder>()
        register<TileEntityShulkerBox, ShulkerBoxRenderBuilder>()
        register<TileEntityEnderChest, EnderChestRenderBuilder>()

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

    @Suppress("DEPRECATION")
    override fun render() {
        mc.gameRenderer.lightmapTextureManager.enable()
        GlStateManager._activeTexture(GL_TEXTURE2)
        GlStateManager._bindTexture(RenderSystem.getShaderTexture(2))
        GlStateManager._activeTexture(GL_TEXTURE0)
        RenderSystem.disableCull()
        RenderSystem.enableDepthTest()
        RenderSystem.enableBlend()

        super.render()

        mc.gameRenderer.lightmapTextureManager.disable()
    }

    private inner class ChestRenderEntry : AbstractRenderEntry<TileEntityChest, ChestInfo>() {
        private var smallChestRenderer: AbstractRenderBuilder.Renderer? = null
        private var largeChestRenderer: AbstractRenderBuilder.Renderer? = null

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
                            val builder = SmallChestRenderBuilder()
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
                            val builder = LargeChestRenderBuilder()
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