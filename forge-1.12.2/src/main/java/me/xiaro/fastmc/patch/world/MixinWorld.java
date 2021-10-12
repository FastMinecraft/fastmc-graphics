package me.xiaro.fastmc.patch.world;

import me.xiaro.fastmc.utils.RaytraceKt;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(World.class)
public class MixinWorld {
    /**
     * @author Xiaro
     * @reason Raytrace optimization
     */
    @Overwrite
    @Nullable
    public RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        return RaytraceKt.rayTrace((World) (Object) this, vec31, vec32, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock);
    }
}
