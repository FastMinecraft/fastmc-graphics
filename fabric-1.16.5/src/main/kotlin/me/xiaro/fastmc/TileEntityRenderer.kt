package me.xiaro.fastmc

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import me.xiaro.fastmc.tileentity.*
import net.minecraft.block.enums.ChestType
import org.joml.Matrix4f

class TileEntityRenderer(private val mc: Minecraft, worldRenderer: AbstractWorldRenderer) :
    AbstractTileEntityRenderer<TileEntity>(worldRenderer) {

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

        mc.gameRenderer.lightmapTextureManager.enable()
        RenderSystem.disableCull()
        RenderSystem.enableDepthTest()
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
            val chestType = if (tileEntity.hasWorld()) {
                tileEntity.cachedState.getOrEmpty(BlockChest.CHEST_TYPE).orElse(ChestType.SINGLE)
            } else {
                ChestType.SINGLE
            }

            if (chestType == ChestType.SINGLE) {
                smallChest.add(tileEntity)
                smallDirty = true
            } else if (chestType == ChestType.LEFT) {
                largeChest.add(tileEntity)
                largeDirty = true
            }
        }

        override fun addAll(list: Collection<TileEntityChest>) {
            list.forEach {
                val chestType = if (it.hasWorld()) {
                    it.cachedState.getOrEmpty(BlockChest.CHEST_TYPE).orElse(ChestType.SINGLE)
                } else {
                    ChestType.SINGLE
                }

                if (chestType == ChestType.SINGLE) {
                    smallChest.add(it)
                    smallDirty = true
                } else if (chestType == ChestType.LEFT) {
                    largeChest.add(it)
                    largeDirty = true
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