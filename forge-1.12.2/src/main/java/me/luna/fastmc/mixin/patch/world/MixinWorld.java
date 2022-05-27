package me.luna.fastmc.mixin.patch.world;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.luna.fastmc.mixin.IPatchedChunk;
import me.luna.fastmc.mixin.IPatchedIBlockAccess;
import me.luna.fastmc.mixin.IPatchedWorld;
import me.luna.fastmc.shared.util.DoubleBufferedCollection;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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
    public WorldProvider provider;
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

    @Shadow
    int[] lightUpdateBlockList;

    @Shadow
    protected abstract boolean isAreaLoaded(
        int xStart,
        int yStart,
        int zStart,
        int xEnd,
        int yEnd,
        int zEnd,
        boolean allowEmpty
    );

    @Shadow
    public abstract void notifyLightSet(BlockPos pos);

    @Shadow
    public abstract void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2);

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

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public void markBlocksDirtyVertical(int x, int z, int y1, int y2) {
        if (y1 > y2) {
            int i = y2;
            y2 = y1;
            y1 = i;
        }

        if (this.provider.hasSkyLight()) {
            for (int y = y1; y <= y2; ++y) {
                this.checkLightFor(EnumSkyBlock.SKY, x, y, z);
            }
        }

        this.markBlockRangeForRenderUpdate(x, y1, z, x, y2, z);
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos) {
        return checkLightFor(lightType, pos.getX(), pos.getY(), pos.getZ());
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean checkLightFor(@NotNull EnumSkyBlock lightType, int x, int y, int z) {
        if (!this.isAreaLoaded(x, y, z, 16, false)) {
            return false;
        } else {
            int updateRange = this.isAreaLoaded(x, y, z, 18, false) ? 17 : 15;
            int i = 0;
            this.profiler.startSection("getBrightness");
            int light = this.getLightFor(lightType, x, y, z);
            int rawLight = this.getRawLight(x, y, z, lightType);

            if (rawLight > light) {
                this.lightUpdateBlockList[i++] = 133152;
            } else if (rawLight < light) {
                this.lightUpdateBlockList[i++] = 133152 | light << 18;

                int i2 = 0;
                while (i2 < i) {
                    int lightUpdateValue = this.lightUpdateBlockList[i2++];
                    int cx = (lightUpdateValue & 63) - 32 + x;
                    int cy = (lightUpdateValue >> 6 & 63) - 32 + y;
                    int cz = (lightUpdateValue >> 12 & 63) - 32 + z;
                    int i5 = lightUpdateValue >> 18 & 15;
                    int cLight = this.getLightFor(lightType, cx, cy, cz);

                    if (cLight == i5) {
                        this.setLightFor(lightType, cx, cy, cz, 0);

                        if (i5 > 0) {
                            int k5 = MathHelper.abs(cx - x);
                            int l5 = MathHelper.abs(cy - y);
                            int i6 = MathHelper.abs(cz - z);

                            if (k5 + l5 + i6 < updateRange) {
                                EnumFacing[] values = EnumFacing.values();
                                for (int iFacing = 0; iFacing < values.length; iFacing++) {
                                    EnumFacing facing = values[iFacing];
                                    int ox = cx + facing.getXOffset();
                                    int oy = cy + facing.getYOffset();
                                    int oz = cz + facing.getZOffset();
                                    IBlockState blockState = this.getBlockState(ox, oy, oz);
                                    int i7 = Math.max(1, blockState.getLightOpacity());
                                    cLight = this.getLightFor(lightType, ox, oy, oz);

                                    if (cLight == i5 - i7 && i < this.lightUpdateBlockList.length) {
                                        this.lightUpdateBlockList[i++] = ox - x + 32 | oy - y + 32 << 6 | oz - z + 32 << 12 | i5 - i7 << 18;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.profiler.endSection();
            this.profiler.startSection("checkedPosition < toCheckCount");
            int i2 = 0;

            while (i2 < i) {
                int cPosIndex = this.lightUpdateBlockList[i2++];
                int cx = (cPosIndex & 63) - 32 + x;
                int cy = (cPosIndex >> 6 & 63) - 32 + y;
                int cz = (cPosIndex >> 12 & 63) - 32 + z;
                int cLight = this.getLightFor(lightType, cx, cy, cz);
                int cRawLight = this.getRawLight(cx, cy, cz, lightType);

                if (cRawLight != cLight) {
                    this.setLightFor(lightType, cx, cy, cz, cRawLight);

                    if (cRawLight > cLight) {
                        int diffX = Math.abs(cx - x);
                        int diffY = Math.abs(cy - y);
                        int diffZ = Math.abs(cz - z);

                        if (diffX + diffY + diffZ < updateRange && i < this.lightUpdateBlockList.length - 6) {
                            if (this.getLightFor(lightType, cx, cy - 1, cz) < cRawLight) {
                                this.lightUpdateBlockList[i++] = cx - x + 32 + (cy - 1 - y + 32 << 6) + (cz - z + 32 << 12);
                            }

                            if (this.getLightFor(lightType, cx, cy + 1, cz) < cRawLight) {
                                this.lightUpdateBlockList[i++] = cx - x + 32 + (cy + 1 - y + 32 << 6) + (cz - z + 32 << 12);
                            }

                            if (this.getLightFor(lightType, cx, cy, cz - 1) < cRawLight) {
                                this.lightUpdateBlockList[i++] = cx - x + 32 + (cy - y + 32 << 6) + (cz - 1 - z + 32 << 12);
                            }

                            if (this.getLightFor(lightType, cx, cy, cz + 1) < cRawLight) {
                                this.lightUpdateBlockList[i++] = cx - x + 32 + (cy - y + 32 << 6) + (cz + 1 - z + 32 << 12);
                            }

                            if (this.getLightFor(lightType, cx - 1, cy, cz) < cRawLight) {
                                this.lightUpdateBlockList[i++] = cx - 1 - x + 32 + (cy - y + 32 << 6) + (cz - z + 32 << 12);
                            }

                            if (this.getLightFor(lightType, cx + 1, cy, cz) < cRawLight) {
                                this.lightUpdateBlockList[i++] = cx + 1 - x + 32 + (cy - y + 32 << 6) + (cz - z + 32 << 12);
                            }
                        }
                    }
                }
            }

            this.profiler.endSection();
            return true;
        }
    }


    @SuppressWarnings("deprecation")
    private int getRawLight(int x, int y, int z, EnumSkyBlock lightType) {
        if (lightType == EnumSkyBlock.SKY && this.canSeeSky(x, y, z)) {
            return 15;
        } else {
            IBlockState blockState = this.getBlockState(x, y, z);
            int lightValue = lightType == EnumSkyBlock.SKY ? 0 : blockState.getLightValue();
            int lightOpacity = blockState.getLightOpacity();

            if (lightOpacity < 1) {
                lightOpacity = 1;
            }

            if (lightOpacity < 15 && lightValue < 14) {
                int temp = this.getLightFor(lightType, x, y - 1, z) - lightOpacity;
                if (temp >= 14) return temp;
                else if (temp > lightValue) lightValue = temp;

                temp = this.getLightFor(lightType, x, y + 1, z) - lightOpacity;
                if (temp >= 14) return temp;
                else if (temp > lightValue) lightValue = temp;

                temp = this.getLightFor(lightType, x, y, z - 1) - lightOpacity;
                if (temp >= 14) return temp;
                else if (temp > lightValue) lightValue = temp;

                temp = this.getLightFor(lightType, x, y, z + 1) - lightOpacity;
                if (temp >= 14) return temp;
                else if (temp > lightValue) lightValue = temp;

                temp = this.getLightFor(lightType, x - 1, y, z) - lightOpacity;
                if (temp >= 14) return temp;
                else if (temp > lightValue) lightValue = temp;

                temp = this.getLightFor(lightType, x + 1, y, z) - lightOpacity;
                if (temp >= 14) return temp;
                else if (temp > lightValue) lightValue = temp;
            }

            return lightValue;
        }
    }

    @Override
    public boolean isAreaLoaded(int x, int y, int z, int radius, boolean allowEmpty) {
        return this.isAreaLoaded(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius, allowEmpty);
    }

    public boolean canSeeSky(int x, int y, int z) {
        return ((IPatchedChunk) this.getChunk(x >> 4, z >> 4)).canSeeSky(x, y, z);
    }

    @Override
    public int getLightFromNeighborsFor(@NotNull EnumSkyBlock type, int x, int y, int z) {
        if (type == EnumSkyBlock.SKY && !this.provider.hasSkyLight()) {
            return 0;
        } else {
            if (y < 0) {
                y = 0;
            }

            if (!this.isValid(x, y, z)) {
                return type.defaultLightValue;
            } else if (!this.isBlockLoaded(x, z)) {
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
                IPatchedChunk chunk = (IPatchedChunk) this.getChunk(x >> 4, z >> 4);
                return chunk.getLightFor(type, x, y, z);
            }
        }
    }

    public void setLightFor(EnumSkyBlock type, int x, int y, int z, int lightValue) {
        if (this.isValid(x, y, z)) {
            if (this.isBlockLoaded(x, z)) {
                BlockPos pos = new BlockPos(x, y, z);
                Chunk chunk = getChunk(pos);
                chunk.setLightFor(type, pos, lightValue);
                this.notifyLightSet(pos);
            }
        }
    }

    @Override
    public int getLightFor(@NotNull EnumSkyBlock type, int x, int y, int z) {
        if (y < 0) {
            y = 0;
        }

        if (!this.isValid(x, y, z) || !this.isBlockLoaded(x, z)) {
            return type.defaultLightValue;
        } else {
            IPatchedChunk chunk = (IPatchedChunk) this.getChunk(x >> 4, z >> 4);
            return chunk.getLightFor(type, x, y, z);
        }
    }

    @Override
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid(int x, int y, int z) {
        return !this.isOutsideBuildHeight(y) && x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000;
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return y < 0 || y >= 256;
    }

    @Override
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isBlockLoaded(int x, int z) {
        return this.isBlockLoaded(x, z, true);
    }

    @Override
    public boolean isBlockLoaded(int x, int z, boolean allowEmpty) {
        return this.isChunkLoaded(x >> 4, z >> 4, allowEmpty);
    }

    @Override
    public @NotNull IBlockState getBlockState(int x, int y, int z) {
        if (this.isOutsideBuildHeight(y)) {
            return Blocks.AIR.getDefaultState();
        } else {
            Chunk chunk = this.getChunk(x >> 4, z >> 4);
            return chunk.getBlockState(x, y, z);
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
