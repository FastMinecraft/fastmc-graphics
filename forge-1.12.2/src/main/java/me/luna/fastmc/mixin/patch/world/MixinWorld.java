package me.luna.fastmc.mixin.patch.world;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.luna.fastmc.mixin.IPatchedChunk;
import me.luna.fastmc.mixin.IPatchedWorld;
import me.luna.fastmc.shared.util.ITypeID;
import me.luna.fastmc.shared.util.collection.FastIntMap;
import me.luna.fastmc.util.RaytraceKt;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.server.timings.TimeTracker;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mixin(World.class)
public abstract class MixinWorld implements IPatchedWorld {

    @Shadow
    @Final
    public WorldProvider provider;

    @Shadow
    private boolean processingLoadedTiles;

    @Shadow
    @Final
    public Profiler profiler;

    @Shadow
    @Final
    private List<TileEntity> addedTileEntityList;

    @Shadow
    @Final
    public List<TileEntity> loadedTileEntityList;

    @Shadow
    @Final
    private WorldBorder worldBorder;

    @Shadow
    @Final
    public List<TileEntity> tickableTileEntities;

    @Shadow
    @Final
    private List<TileEntity> tileEntitiesToBeRemoved;

    @Shadow
    @Final
    public List<Entity> loadedEntityList;

    @Shadow
    @Final
    public List<Entity> weatherEffects;

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
    public abstract void removeTileEntity(BlockPos pos);

    @Shadow
    public abstract boolean isBlockLoaded(BlockPos pos, boolean allowEmpty);

    @Shadow
    public abstract void onEntityRemoved(Entity entityIn);

    @Shadow
    public abstract void removeEntity(Entity entityIn);

    @Shadow
    public abstract void updateEntity(Entity ent);

    @Shadow
    protected abstract void tickPlayers();

    @Shadow
    @Final
    protected List<Entity> unloadedEntityList;

    private final IntSet unloadedEntitiesOverride = new IntOpenHashSet();
    private final IntSet removingWeatherEffects = new IntOpenHashSet();
    private final IntSet removingEntities = new IntOpenHashSet();
    private final List<Entity> removingEntitiesList = new ArrayList<>();
    private final Set<TileEntity> removingTileEntity = new LinkedHashSet<>();
    private final FastIntMap<List<TileEntity>> groupedTickableTileEntity = new FastIntMap<>();

    @Override
    @NotNull
    public IntSet getUnloadedEntitiesOverride() {
        return unloadedEntitiesOverride;
    }

    @Override
    @NotNull
    public IntSet getRemovingEntities() {
        return removingEntities;
    }

    @NotNull
    @Override
    public List<Entity> getRemovingEntitiesList() {
        return removingEntitiesList;
    }

    @Override
    public void batchRemoveEntities() {
        this.loadedEntityList.removeIf(it -> getRemovingEntities().contains(it.getEntityId()));

        for (Entity entity : getRemovingEntitiesList()) {
            int chunkCoordX = entity.chunkCoordX;
            int chunkCoordZ = entity.chunkCoordZ;

            if (entity.addedToChunk && this.isChunkLoaded(chunkCoordX, chunkCoordZ, true)) {
                this.getChunk(chunkCoordX, chunkCoordZ).removeEntity(entity);
            }

            this.onEntityRemoved(entity);
        }

        getRemovingEntities().clear();
        getRemovingEntitiesList().clear();
    }

    /**
     * @author Luna
     * @reason Raytrace optimization
     */
    @Overwrite
    @Nullable
    public RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        return RaytraceKt.rayTrace((World) (Object) this, vec31, vec32, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock);
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
            for (TileEntity tileEntity : addedTileEntityList) {
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

            addedTileEntityList.clear();
        }
        this.profiler.endSection();

        this.profiler.endSection();
    }

    private void tickWeather() {
        removingWeatherEffects.clear();

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
                    removeEntity(entity);
                } else
                    throw new ReportedException(crashreport);
            }

            if (entity.isDead) {
                removingWeatherEffects.add(entity.getEntityId());
            }
        }
    }

    private void tickRemove() {
        this.weatherEffects.removeIf(it -> removingWeatherEffects.contains(it.getEntityId()));
        this.loadedEntityList.removeIf(it -> unloadedEntitiesOverride.contains(it.getEntityId()));

        for (Entity entity : this.unloadedEntityList) {
            int x = entity.chunkCoordX;
            int y = entity.chunkCoordZ;

            if (entity.addedToChunk && this.isChunkLoaded(x, y, true)) {
                this.getChunk(x, y).removeEntity(entity);
            }
        }

        for (Entity entity : this.unloadedEntityList) {
            this.onEntityRemoved(entity);
        }

        unloadedEntityList.clear();
        unloadedEntitiesOverride.clear();
    }

    private void tickEntity() {
        this.profiler.startSection("tick");

        for (Entity entity : this.loadedEntityList) {
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
                        removeEntity(entity);
                    } else
                        throw new ReportedException(crashReport);
                }
            }
        }

        this.profiler.endStartSection("remove");
        batchRemoveEntities();
        this.profiler.endSection();
    }

    private void tickTileEntity() {
        this.processingLoadedTiles = true; //FML Move above remove to prevent CMEs

        if (!this.tileEntitiesToBeRemoved.isEmpty()) {
            for (TileEntity tile : tileEntitiesToBeRemoved) {
                tile.onChunkUnload();
            }

            removingTileEntity.addAll(tileEntitiesToBeRemoved);

            tickableTileEntities.removeAll(removingTileEntity);
            loadedTileEntityList.removeAll(removingTileEntity);

            removingTileEntity.clear();
            tileEntitiesToBeRemoved.clear();
        }

        for (List<TileEntity> list : groupedTickableTileEntity.values()) {
            list.clear();
        }

        for (TileEntity tileEntity : tickableTileEntities) {
            if (tileEntity.isInvalid()) {
                removingTileEntity.add(tileEntity);
            } else {
                int id = ((ITypeID) tileEntity).getTypeID();
                List<TileEntity> list = groupedTickableTileEntity.get(id);

                if (list == null) {
                    list = new ArrayList<>();
                    groupedTickableTileEntity.put(id, list);
                }

                list.add(tileEntity);
            }
        }

        tickableTileEntities.removeAll(removingTileEntity);
        loadedTileEntityList.removeAll(removingTileEntity);

        for (TileEntity tileEntity : removingTileEntity) {
            BlockPos pos = tileEntity.getPos();

            if (this.isBlockLoaded(pos)) {
                //Forge: Bugfix: If we set the tile entity it immediately sets it in the chunk, so we could be desyned
                Chunk chunk = this.getChunk(pos);
                if (chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) == tileEntity)
                    chunk.removeTileEntity(pos);
            }
        }

        removingTileEntity.clear();

        for (List<TileEntity> list : groupedTickableTileEntity.values()) {
            if (list.isEmpty()) continue;

            TileEntity first = list.get(0);
            profiler.func_194340_a(() -> String.valueOf(TileEntity.getKey(first.getClass())));

            for (TileEntity tileEntity : list) {
                if (!tileEntity.isInvalid() && tileEntity.hasWorld()) {
                    BlockPos pos = tileEntity.getPos();

                    //Forge: Fix TE's getting an extra tick on the client side....
                    if (this.isBlockLoaded(pos, false) && worldBorder.contains(pos)) {
                        try {
                            TimeTracker.TILE_ENTITY_UPDATE.trackStart(tileEntity);
                            ((ITickable) tileEntity).update();
                            TimeTracker.TILE_ENTITY_UPDATE.trackEnd(tileEntity);
                        } catch (Throwable throwable) {
                            CrashReport crashReport = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                            CrashReportCategory crashReportCategory = crashReport.makeCategory("Block entity being ticked");
                            tileEntity.addInfoToCrashReport(crashReportCategory);

                            if (ForgeModContainer.removeErroringTileEntities) {
                                FMLLog.log.fatal("{}", crashReport.getCompleteReport());
                                tileEntity.invalidate();
                                removeTileEntity(pos);
                            } else {
                                throw new ReportedException(crashReport);
                            }
                        }
                    }
                }
            }

            profiler.endSection();
        }

        this.processingLoadedTiles = false;
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        return getLightFor(type, pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    @SideOnly(Side.CLIENT)
    public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos) {
        return getLightFromNeighborsFor(type, pos.getX(), pos.getY(), pos.getZ());
    }

    public int getLightFromNeighborsFor(EnumSkyBlock type, int x, int y, int z) {
        if (type == EnumSkyBlock.SKY && !this.provider.hasSkyLight()) {
            return 0;
        } else {
            if (y < 0) {
                y = 0;
            }

            if (!this.isValid(x, y, z)) {
                return type.defaultLightValue;
            } else if (!this.isBlockLoaded(x, y, z)) {
                return type.defaultLightValue;
            } else if (this.getBlockState(x, y, z).useNeighborBrightness()) {
                int i1 = this.getLightFor(type, x, y + 1, z);
                int i = this.getLightFor(type, x + 1, y, z);
                int j = this.getLightFor(type, x - 1, y, z);
                int k = this.getLightFor(type, x, y, z + 1);
                int l = this.getLightFor(type, x, y, z - 1);

                if (i > i1) {
                    i1 = i;
                }

                if (j > i1) {
                    i1 = j;
                }

                if (k > i1) {
                    i1 = k;
                }

                if (l > i1) {
                    i1 = l;
                }

                return i1;
            } else {
                IPatchedChunk chunk = (IPatchedChunk) this.getChunk(x, y, z);
                return chunk.getLightFor(type, x, y, z);
            }
        }
    }

    public int getLightFor(EnumSkyBlock type, int x, int y, int z) {
        if (y < 0) {
            y = 0;
        }

        if (!this.isValid(x, y, z)) {
            return type.defaultLightValue;
        } else if (!this.isBlockLoaded(x, y, z)) {
            return type.defaultLightValue;
        } else {
            IPatchedChunk chunk = (IPatchedChunk) this.getChunk(x, y, z);
            return chunk.getLightFor(type, x, y, z);
        }
    }

    public boolean isValid(int x, int y, int z) {
        return !this.isOutsideBuildHeight(x, y, z) && x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000;
    }

    public boolean isOutsideBuildHeight(int x, int y, int z) {
        return y < 0 || y >= 256;
    }

    public boolean isBlockLoaded(int x, int y, int z) {
        return this.isBlockLoaded(x, y, z, true);
    }

    public boolean isBlockLoaded(int x, int y, int z, boolean allowEmpty) {
        return this.isChunkLoaded(x >> 4, z >> 4, allowEmpty);
    }

    public Chunk getChunk(int x, int y, int z) {
        return this.getChunk(x >> 4, z >> 4);
    }

    public IBlockState getBlockState(int x, int y, int z) {
        if (this.isOutsideBuildHeight(x, y, z)) {
            return Blocks.AIR.getDefaultState();
        } else {
            Chunk chunk = this.getChunk(x, y, z);
            return chunk.getBlockState(x, y, z);
        }
    }
}
