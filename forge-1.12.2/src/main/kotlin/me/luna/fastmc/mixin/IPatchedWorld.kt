package me.luna.fastmc.mixin

import it.unimi.dsi.fastutil.ints.IntSet
import kotlinx.coroutines.*
import me.luna.fastmc.shared.renderbuilder.IParallelUpdate
import me.luna.fastmc.shared.util.ParallelUtils
import me.luna.fastmc.shared.util.collection.FastIntMap
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

interface IPatchedWorld {
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

    @OptIn(ObsoleteCoroutinesApi::class)
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
            val anyInvalid = booleanArrayOf(false)

            coroutineScope {
                for (list in groupedTickableTileEntity.values) {
                    if (list.isEmpty()) continue
                    val first = list[0]

                    if (first is IParallelUpdate) {
                        updateParallel(mainContext, anyInvalid, list)
                    } else {
                        updateSerial(mainContext, anyInvalid, list)
                    }
                }
            }

            if (anyInvalid[0]) {
                launch(Dispatchers.Default) { addedTileEntityList.removeIf { it.isInvalid } }
                launch(Dispatchers.Default) { world.tickableTileEntities.removeIf { it.isInvalid } }
                launch(Dispatchers.Default) { world.loadedTileEntityList.removeIf { it.isInvalid } }
            }
        }

        processingLoadedTiles = false
    }

    fun CoroutineScope.updateParallel(
        mainContext: CoroutineContext,
        anyInvalid: BooleanArray,
        list: MutableList<TileEntity>
    ) {
        launch(Dispatchers.Default) {
            coroutineScope {
                ParallelUtils.splitListIndex(
                    total = list.size,
                    blockForEach = { start, end ->
                        launch(Dispatchers.Default) {
                            val mainThreadCommand = ArrayList<Runnable>()
                            val parallelCommand = ArrayList<Runnable>()

                            for (i in start until end) {
                                updateParallelTileEntity(anyInvalid, mainThreadCommand, parallelCommand, list[i])
                            }

                            this@updateParallel.launch(mainContext) {
                                mainThreadCommand.forEach {
                                    it.run()
                                }

                                handleParallelCommand(parallelCommand)
                            }
                        }
                    }
                )
            }

            list.removeIf { it.isInvalid }
        }
    }

    fun updateParallelTileEntity(
        anyInvalid: BooleanArray,
        mainThreadCommand: MutableList<Runnable>,
        parallelCommand: MutableList<Runnable>,
        tileEntity: TileEntity) {
        if (tileEntity.isInvalid) {
            anyInvalid[0] = true
        } else if (tileEntity.hasWorld()) {
            val pos = tileEntity.pos

            if (world.isBlockLoaded(pos, false) && world.worldBorder.contains(pos)) {
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

    fun CoroutineScope.handleParallelCommand(
        commands: MutableList<Runnable>
    ) {
        launch(Dispatchers.Default) {
            ParallelUtils.splitListIndex(
                commands.size,
                blockForEach = { start, end ->
                    launch {
                        for (i in start until end) {
                            commands[i].run()
                        }
                    }
                }
            )
        }
    }

    fun CoroutineScope.updateSerial(
        mainContext: CoroutineContext,
        anyInvalid: BooleanArray,
        list: MutableList<TileEntity>
    ) {
        launch(mainContext) {
            list.removeIf { tileEntity ->
                if (tileEntity.isInvalid) {
                    anyInvalid[0] = true
                } else if (tileEntity.hasWorld()) {
                    val pos = tileEntity.pos

                    //Forge: Fix TE's getting an extra tick on the client side....
                    if (world.isBlockLoaded(pos, false) && world.worldBorder.contains(pos)) {
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