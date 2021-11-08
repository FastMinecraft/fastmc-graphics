package me.xiaro.fastmc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.xiaro.fastmc.tileentity.*
import net.minecraft.block.BlockChest
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityChest
import org.joml.Matrix4f

class TileEntityRenderer(private val mc: Minecraft, worldRenderer: AbstractWorldRenderer) : AbstractTileEntityRenderer<TileEntity>(worldRenderer) {
    init {
        register(BedInfo::class.java, BedRenderBuilder::class.java)
        register(EnderChestInfo::class.java, EnderChestRenderBuilder::class.java)
        register(ShulkerBoxInfo::class.java, ShulkerBoxRenderBuilder::class.java)

        addRenderEntry(ChestRenderEntry())
    }

    override fun onPostTick() {
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
    }

    override fun render() {
        mc.profiler.startSection("render")

        GlStateManager.disableCull()
        super.render()

        mc.profiler.endSection()
    }

    private inner class ChestRenderEntry : AbstractRenderEntry<TileEntityChest, ChestInfo>() {
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

        override fun remove(tileEntity: TileEntityChest): Boolean {
            val smallRemoved = smallChest.remove(tileEntity)
            val largeRemoved = largeChest.remove(tileEntity)
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
                    scope.launch {
                        val builder = SmallChestRenderBuilder()
                        val entityInfo = ChestInfo()

                        builder.init(this@TileEntityRenderer, smallChest.size)

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
                        val builder = LargeChestRenderBuilder()
                        val entityInfo = ChestInfo()

                        builder.init(this@TileEntityRenderer, largeChest.size)

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