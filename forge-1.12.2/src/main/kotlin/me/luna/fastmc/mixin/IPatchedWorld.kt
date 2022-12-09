package me.luna.fastmc.mixin

import dev.fastmc.common.FastMcCoreScope
import dev.fastmc.common.ParallelUtils
import dev.fastmc.common.collection.FastIntMap
import it.unimi.dsi.fastutil.ints.IntSet
import kotlinx.coroutines.*
import me.luna.fastmc.shared.instancing.IParallelUpdate
import net.minecraft.crash.CrashReport
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ITickable
import net.minecraft.util.ReportedException
import net.minecraft.world.World
import net.minecraftforge.common.ForgeModContainer
import net.minecraftforge.fml.common.FMLLog
import net.minecraftforge.server.timings.TimeTracker
import kotlin.coroutines.CoroutineContext

interface IPatchedWorld : IPatchedIBlockAccess {
    val world: World; get() = this as World

    var processingLoadedTiles: Boolean
    var tileEntitiesToBeRemoved: MutableList<TileEntity>
    var addedTileEntityList: MutableList<TileEntity>

    val removingEntities: IntSet
    val removingEntitiesList: ArrayList<Entity>
    val groupedTickableTileEntity: FastIntMap<MutableList<TileEntity>>

    fun markRemoving(entity: Entity) {
        removingEntities.add(entity.entityId)
        removingEntitiesList.add(entity)
    }

    fun batchRemoveEntities()

    fun tickTileEntity() {
        processingLoadedTiles = true //FML Move above remove to prevent CMEs

        if (tileEntitiesToBeRemoved.isNotEmpty()) {
            val temp = this.tileEntitiesToBeRemoved
            tileEntitiesToBeRemoved = ArrayList()

            for (tileEntity in temp) {
                tileEntity.onChunkUnload()
                tileEntity.invalidate()
            }
        }

        runBlocking {
            val mainContext = this.coroutineContext
            val anyInvalidAll = booleanArrayOf(false)

            coroutineScope {
                for (list in groupedTickableTileEntity.values) {
                    if (list.isEmpty()) continue
                    val first = list[0]

                    if (first is IParallelUpdate) {
                        updateParallel(this, mainContext, anyInvalidAll, list)
                    } else {
                        updateSerial(this, mainContext, anyInvalidAll, list)
                    }
                }
            }

            if (anyInvalidAll[0]) {
                launch(FastMcCoreScope.context) { addedTileEntityList.removeIf { it.isInvalid } }
                launch(FastMcCoreScope.context) { world.tickableTileEntities.removeIf { it.isInvalid } }
                launch(FastMcCoreScope.context) { world.loadedTileEntityList.removeIf { it.isInvalid } }
            }
        }

        processingLoadedTiles = false
    }

    fun updateParallel(
        scope: CoroutineScope,
        mainContext: CoroutineContext,
        anyInvalid: BooleanArray,
        list: MutableList<TileEntity>
    ) {
        scope.launch(FastMcCoreScope.context) outer@{
            val anyInvalidGroup = booleanArrayOf(false)

            coroutineScope {
                ParallelUtils.splitListIndex(
                    total = list.size,
                    blockForEach = { start, end ->
                        launch {
                            val mainThreadCommand = ArrayList<Runnable>()
                            val parallelCommand = ArrayList<Runnable>()

                            for (i in start until end) {
                                updateParallelTileEntity(
                                    anyInvalid,
                                    anyInvalidGroup,
                                    mainThreadCommand,
                                    parallelCommand,
                                    list[i]
                                )
                            }

                            this@outer.launch(mainContext) {
                                mainThreadCommand.forEach {
                                    it.run()
                                }

                                withContext(FastMcCoreScope.context) {
                                    ParallelUtils.splitListIndex(
                                        parallelCommand.size,
                                        blockForEach = { start, end ->
                                            launch(FastMcCoreScope.context) {
                                                for (i in start until end) {
                                                    parallelCommand[i].run()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            if (anyInvalidGroup[0]) list.removeIf { it.isInvalid }
        }
    }

    fun updateParallelTileEntity(
        anyInvalid: BooleanArray,
        anyInvalidGroup: BooleanArray,
        mainThreadCommand: MutableList<Runnable>,
        parallelCommand: MutableList<Runnable>,
        tileEntity: TileEntity
    ) {
        if (tileEntity.isInvalid) {
            anyInvalid[0] = true
            anyInvalidGroup[0] = true
        } else if (tileEntity.hasWorld()) {
            val pos = tileEntity.pos

            if (world.worldBorder.contains(pos) && world.isBlockLoaded(pos, false)) {
                try {
                    (tileEntity as IParallelUpdate).updateParallel(mainThreadCommand, parallelCommand)
                } catch (throwable: Throwable) {
                    val crashReport =
                        CrashReport.makeCrashReport(
                            throwable,
                            "Ticking block entity"
                        )
                    tileEntity.addInfoToCrashReport(crashReport.makeCategory("Block entity being ticked"))

                    if (ForgeModContainer.removeErroringTileEntities) {
                        FMLLog.log.fatal("{}", crashReport.completeReport)
                        tileEntity.invalidate()
                    } else {
                        throw ReportedException(crashReport)
                    }
                }
            }
        }
    }

    fun updateSerial(
        scope: CoroutineScope,
        mainContext: CoroutineContext,
        anyInvalid: BooleanArray,
        list: MutableList<TileEntity>
    ) {
        scope.launch(mainContext) {
            list.removeIf { tileEntity ->
                if (tileEntity.isInvalid) {
                    anyInvalid[0] = true
                } else if (tileEntity.hasWorld()) {
                    val pos = tileEntity.pos

                    if (world.worldBorder.contains(pos) && world.isBlockLoaded(pos, false)) {
                        try {
                            TimeTracker.TILE_ENTITY_UPDATE.trackStart(tileEntity)
                            (tileEntity as ITickable).update()
                            TimeTracker.TILE_ENTITY_UPDATE.trackEnd(tileEntity)
                        } catch (throwable: Throwable) {
                            val crashReport =
                                CrashReport.makeCrashReport(throwable, "Ticking block entity")
                            tileEntity.addInfoToCrashReport(crashReport.makeCategory("Block entity being ticked"))

                            if (ForgeModContainer.removeErroringTileEntities) {
                                FMLLog.log.fatal("{}", crashReport.completeReport)
                                tileEntity.invalidate()
                            } else {
                                throw ReportedException(crashReport)
                            }
                        }
                    }
                }

                tileEntity.isInvalid
            }
        }
    }
}