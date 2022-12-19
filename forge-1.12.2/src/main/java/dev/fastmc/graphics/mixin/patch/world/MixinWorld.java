package dev.fastmc.graphics.mixin.patch.world;

import dev.fastmc.common.DoubleBufferedCollection;
import dev.fastmc.common.collection.FastIntMap;
import dev.fastmc.graphics.mixin.IPatchedIBlockAccess;
import dev.fastmc.graphics.mixin.IPatchedWorld;
import dev.fastmc.graphics.shared.util.ITypeID;
import dev.fastmc.graphics.util.RaytraceKt;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.server.timings.TimeTracker;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(World.class)
public abstract class MixinWorld implements IPatchedWorld, IPatchedIBlockAccess {
    private final DoubleBufferedCollection<IntSet> removingWeatherEffects = new DoubleBufferedCollection<>(
        new IntOpenHashSet(),
        new IntOpenHashSet()
    );
    private final DoubleBufferedCollection<IntSet> removingEntities = new DoubleBufferedCollection<>(
        new IntOpenHashSet(),
        new IntOpenHashSet()
    );
    private final DoubleBufferedCollection<ArrayList<Entity>> removingEntitiesList = new DoubleBufferedCollection<>(
        new ArrayList<>(),
        new ArrayList<>()
    );
    private final FastIntMap<List<TileEntity>> groupedTickableTileEntity = new FastIntMap<>();
    @Shadow
    @Final
    public Profiler profiler;
    @Shadow
    @Final
    public List<TileEntity> loadedTileEntityList;
    @Shadow
    @Final
    public List<Entity> loadedEntityList;
    @Shadow
    @Final
    public List<Entity> weatherEffects;
    @Shadow
    private boolean processingLoadedTiles;
    @Mutable
    @Shadow
    @Final
    private List<TileEntity> addedTileEntityList;
    @Mutable
    @Shadow
    @Final
    private List<TileEntity> tileEntitiesToBeRemoved;

    @Shadow
    public abstract IBlockState getBlockState(BlockPos pos);

    @Shadow
    protected abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);

    @Shadow
    public abstract Chunk getChunk(int chunkX, int chunkZ);

    @Shadow
    public abstract void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState, int flags);

    @Shadow
    public abstract boolean isBlockLoaded(BlockPos pos);

    @Shadow
    public abstract Chunk getChunk(BlockPos pos);

    @Shadow
    public abstract boolean addTileEntity(TileEntity tile);

    @Shadow
    public abstract void onEntityRemoved(Entity entityIn);

    @Shadow
    public abstract void updateEntity(Entity ent);

    @Shadow
    protected abstract void tickPlayers();

    @Shadow
    public abstract TileEntity getTileEntity(BlockPos par1);

    @Override
    public void batchRemoveEntities() {
        if (getRemovingEntities().isEmpty()) return;

        IntSet tempSet = removingEntities.get();
        List<Entity> tempList = removingEntitiesList.get();

        this.loadedEntityList.removeIf(it -> tempSet.contains(it.getEntityId()));

        for (Entity entity : tempList) {
            int chunkCoordX = entity.chunkCoordX;
            int chunkCoordZ = entity.chunkCoordZ;

            if (entity.addedToChunk && this.isChunkLoaded(chunkCoordX, chunkCoordZ, true)) {
                this.getChunk(chunkCoordX, chunkCoordZ).removeEntity(entity);
            }

            this.onEntityRemoved(entity);
        }
    }

    /**
     * @author Luna
     * @reason Raytrace optimization
     */
    @Overwrite
    @Nullable
    public RayTraceResult rayTraceBlocks(
        Vec3d vec31,
        Vec3d vec32,
        boolean stopOnLiquid,
        boolean ignoreBlockWithoutBoundingBox,
        boolean returnLastUncollidableBlock
    ) {
        //noinspection ConstantConditions
        return RaytraceKt.rayTrace(
            (World) (Object) this,
            vec31,
            vec32,
            stopOnLiquid,
            ignoreBlockWithoutBoundingBox,
            returnLastUncollidableBlock
        );
    }

    /**
     * @author Luna
     * @reason Faster air check
     */
    @Overwrite
    public boolean isAirBlock(BlockPos pos) {
        return this.getBlockState(pos).getMaterial() == Material.AIR;
    }

    /**
     * @author Luna
     * @reason General optimization
     */
    @Overwrite
    public void updateEntities() {
        this.profiler.startSection("entities");

        this.profiler.startSection("weather");
        tickWeather();

        this.profiler.endStartSection("remove");
        tickRemove();

        this.tickPlayers();

        this.profiler.endStartSection("regular");
        tickEntity();

        this.profiler.endStartSection("tileEntities");
        tickTileEntity();

        this.profiler.endStartSection("pendingBlockEntities");
        if (!addedTileEntityList.isEmpty()) {
            List<TileEntity> temp = addedTileEntityList;
            addedTileEntityList = new ArrayList<>();

            for (TileEntity tileEntity : temp) {
                if (!tileEntity.isInvalid()) {
                    if (!loadedTileEntityList.contains(tileEntity)) {
                        addTileEntity(tileEntity);
                    }

                    BlockPos pos = tileEntity.getPos();

                    if (isBlockLoaded(pos)) {
                        Chunk chunk = getChunk(pos);
                        IBlockState iblockstate = chunk.getBlockState(pos);
                        chunk.addTileEntity(pos, tileEntity);
                        notifyBlockUpdate(pos, iblockstate, iblockstate, 3);
                    }
                }
            }
        }

        this.profiler.endSection();

        this.profiler.endSection();
    }

    private void tickWeather() {
        for (Entity entity : weatherEffects) {
            try {
                if (entity.updateBlocked) continue;
                ++entity.ticksExisted;
                entity.onUpdate();
            } catch (Throwable throwable2) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable2, "Ticking entity");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being ticked");

                if (entity == null) {
                    crashreportcategory.addCrashSection("Entity", "~~NULL~~");
                } else {
                    entity.addEntityCrashInfo(crashreportcategory);
                }

                if (ForgeModContainer.removeErroringEntities) {
                    FMLLog.log.fatal("{}", crashreport.getCompleteReport());
                    removingWeatherEffects.get().add(entity.getEntityId());
                } else {
                    throw new ReportedException(crashreport);
                }
            }

            if (entity.isDead) {
                removingWeatherEffects.get().add(entity.getEntityId());
            }
        }
    }

    private void tickRemove() {
        if (!removingWeatherEffects.get().isEmpty()) {
            IntSet temp = removingWeatherEffects.getAndSwap();
            this.weatherEffects.removeIf(it -> temp.contains(it.getEntityId()));
        }

        batchRemoveEntities();
    }

    private void tickEntity() {
        this.profiler.startSection("tick");

        List<Entity> temp = this.loadedEntityList;
        for (int i = 0, entityListSize = temp.size(); i < entityListSize; i++) {
            Entity entity = temp.get(i);
            Entity ridingEntity = entity.getRidingEntity();

            if (ridingEntity != null) {
                if (!ridingEntity.isDead && ridingEntity.isPassenger(entity)) {
                    continue;
                }

                entity.dismountRidingEntity();
            }

            if (entity.isDead) {
                markRemoving(entity);
            } else if (!(entity instanceof EntityPlayerMP)) {
                try {
                    TimeTracker.ENTITY_UPDATE.trackStart(entity);
                    this.updateEntity(entity);
                    TimeTracker.ENTITY_UPDATE.trackEnd(entity);
                } catch (Throwable throwable1) {
                    CrashReport crashReport = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                    CrashReportCategory crashReportCategory = crashReport.makeCategory("Entity being ticked");
                    entity.addEntityCrashInfo(crashReportCategory);
                    if (ForgeModContainer.removeErroringEntities) {
                        FMLLog.log.fatal("{}", crashReport.getCompleteReport());
                        markRemoving(entity);
                    } else {
                        throw new ReportedException(crashReport);
                    }
                }
            }
        }

        this.profiler.endStartSection("remove");
        batchRemoveEntities();
        this.profiler.endSection();
    }

    @Inject(method = "addTileEntity", at = @At("TAIL"))
    private void addTileEntity$Inject$TAIL(TileEntity tile, CallbackInfoReturnable<Boolean> cir) {
        if (tile instanceof ITickable) {
            int id = ((ITypeID) tile).getTypeID();
            List<TileEntity> list;

            list = groupedTickableTileEntity.get(id);

            if (list == null) {
                list = new ArrayList<>();
                groupedTickableTileEntity.put(id, list);
            }

            list.add(tile);
        }
    }

    /**
     * @author Luna
     * @reason Optimization
     */
    @Overwrite
    public void unloadEntities(Collection<Entity> entityCollection) {
        if (entityCollection.isEmpty()) return;

        ArrayList<Entity> list = getRemovingEntitiesList();
        IntSet set = getRemovingEntities();
        list.ensureCapacity(list.size() + entityCollection.size());

        for (Entity entity : entityCollection) {
            list.add(entity);
            set.add(entity.getEntityId());
        }
    }

    /**
     * @author Luna
     * @reason Optimization
     */
    @Overwrite
    public void removeTileEntity(BlockPos pos) {
        TileEntity tileEntity = this.getTileEntity(pos);
        if (tileEntity != null) {
            tileEntity.invalidate();
        }
    }

    @Override
    public boolean getProcessingLoadedTiles() {
        return this.processingLoadedTiles;
    }

    @Override
    public void setProcessingLoadedTiles(boolean processingLoadedTiles) {
        this.processingLoadedTiles = processingLoadedTiles;
    }

    @NotNull
    @Override
    public List<TileEntity> getTileEntitiesToBeRemoved() {
        return this.tileEntitiesToBeRemoved;
    }

    @Override
    public void setTileEntitiesToBeRemoved(@NotNull List<TileEntity> tileEntitiesToBeRemoved) {
        this.tileEntitiesToBeRemoved = tileEntitiesToBeRemoved;
    }

    @NotNull
    @Override
    public List<TileEntity> getAddedTileEntityList() {
        return this.addedTileEntityList;
    }

    @Override
    public void setAddedTileEntityList(@NotNull List<TileEntity> addedTileEntityList) {
        this.addedTileEntityList = addedTileEntityList;
    }

    @NotNull
    @Override
    public IntSet getRemovingEntities() {
        return this.removingEntities.get();
    }

    @NotNull
    @Override
    public ArrayList<Entity> getRemovingEntitiesList() {
        return this.removingEntitiesList.get();
    }

    @NotNull
    @Override
    public FastIntMap<List<TileEntity>> getGroupedTickableTileEntity() {
        return this.groupedTickableTileEntity;
    }
}