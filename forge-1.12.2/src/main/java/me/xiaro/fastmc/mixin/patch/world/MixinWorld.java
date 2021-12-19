package me.xiaro.fastmc.mixin.patch.world;

import me.xiaro.fastmc.mixin.IPatchedChunk;
import me.xiaro.fastmc.util.RaytraceKt;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(World.class)
public abstract class MixinWorld {
    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    @Shadow protected abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);

    @Shadow public abstract Chunk getChunk(int chunkX, int chunkZ);

    @Shadow @Final public WorldProvider provider;

    /**
     * @author Xiaro
     * @reason Raytrace optimization
     */
    @Overwrite
    @Nullable
    public RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        return RaytraceKt.rayTrace((World) (Object) this, vec31, vec32, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock);
    }

    /**
     * @author Xiaro
     * @reason Faster air check
     */
    @Overwrite
    public boolean isAirBlock(BlockPos pos) {
        return this.getBlockState(pos).getMaterial() == Material.AIR;
    }

    /**
     * @author Xiaro
     * @reason Memory allocation optimization
     */
    @Overwrite
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        return getLightFor(type, pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * @author Xiaro
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
