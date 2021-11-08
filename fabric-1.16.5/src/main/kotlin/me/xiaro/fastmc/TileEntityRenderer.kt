package me.xiaro.fastmc

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import me.xiaro.fastmc.tileentity.*
import me.xiaro.fastmc.util.blockState
import me.xiaro.fastmc.util.getPropertyOrDefault
import me.xiaro.fastmc.util.getPropertyOrNull
import net.minecraft.block.enums.ChestType
import net.minecraft.util.math.Direction
import org.joml.Matrix4f

class TileEntityRenderer(private val mc: Minecraft, worldRenderer: AbstractWorldRenderer) :
    AbstractTileEntityRenderer<TileEntity>(worldRenderer) {

    init {
        register(BedInfo::class.java, BedRenderBuilder::class.java)
        register(EnderChestInfo::class.java, EnderChestRenderBuilder::class.java)
        register(ShulkerBoxInfo::class.java, ShulkerBoxRenderBuilder::class.java)

        addRenderEntry(ChestRenderEntry())
    }

    override fun onPostTick() {
//        return
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
//        return
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
            val blockState = tileEntity.blockState
            val chestType = blockState.getPropertyOrDefault(BlockChest.CHEST_TYPE, ChestType.SINGLE)

            when (chestType) {
                ChestType.LEFT -> {
                    if (blockState.getPropertyOrDefault(BlockChest.FACING, Direction.SOUTH).horizontal >= 2) {
                        largeChest.add(tileEntity)
                        largeDirty = true
                    }
                }
                ChestType.RIGHT -> {
                    if (blockState.getPropertyOrDefault(BlockChest.FACING, Direction.SOUTH).horizontal < 2) {
                        largeChest.add(tileEntity)
                        largeDirty = true
                    }
                }
                else -> {
                    smallChest.add(tileEntity)
                    smallDirty = true
                }
            }
        }

        override fun addAll(list: Collection<TileEntityChest>) {
            list.forEach {
                add(it)
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