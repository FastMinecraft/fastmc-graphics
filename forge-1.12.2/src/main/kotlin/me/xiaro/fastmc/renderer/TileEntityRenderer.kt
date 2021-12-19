package me.xiaro.fastmc.renderer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import me.xiaro.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.xiaro.fastmc.shared.renderbuilder.tileentity.*
import me.xiaro.fastmc.shared.renderer.AbstractTileEntityRenderer
import me.xiaro.fastmc.shared.renderer.AbstractWorldRenderer
import me.xiaro.fastmc.shared.util.ITypeID
import me.xiaro.fastmc.tileentity.ChestInfo
import net.minecraft.block.BlockChest
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.tileentity.*
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*

class TileEntityRenderer(private val mc: Minecraft, worldRenderer: AbstractWorldRenderer) :
    AbstractTileEntityRenderer<TileEntity>(worldRenderer) {
    init {
        register<TileEntityBed, BedRenderBuilder>()
        register<TileEntityShulkerBox, ShulkerBoxRenderBuilder>()
        register<TileEntityEnderChest, EnderChestRenderBuilder>()

        register(ChestRenderEntry())
    }

    override fun onPostTick() {
        renderEntryList.forEach {
            it.clear()
        }

        mc.world?.let { world ->
            world.loadedTileEntityList.forEach {
                renderEntryMap[(it as ITypeID).typeID]?.add(it)
            }

            updateRenderers()
        } ?: run {
            renderEntryList.forEach {
                it.destroyRenderer()
            }
        }
    }

    override fun render() {
        mc.profiler.startSection("render")

        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        super.render()

        mc.profiler.endSection()
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

            entity.checkForAdjacentChests()

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

        override fun addAll(list: Collection<TileEntityChest>) {
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
                    scope.launch(Dispatchers.Default) {
                        val builder = SmallChestRenderBuilder()

                        builder.init(this@TileEntityRenderer, smallChest.size)

                        smallChest.forEach {
                            builder.add(it as ChestInfo)
                        }

                        actor.send {
                            smallChestRenderer?.destroy()
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
                    scope.launch(Dispatchers.Default) {
                        val builder = LargeChestRenderBuilder()
                        builder.init(this@TileEntityRenderer, largeChest.size)

                        largeChest.forEach {
                            builder.add(it as ChestInfo)
                        }

                        actor.send {
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
            smallDirty = false
            largeDirty = false
        }

        override fun render(modelView: Matrix4f, renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            smallChestRenderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
            largeChestRenderer?.render(modelView, renderPosX, renderPosY, renderPosZ)
        }
    }
}