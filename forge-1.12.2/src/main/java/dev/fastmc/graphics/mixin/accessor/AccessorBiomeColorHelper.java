package dev.fastmc.graphics.mixin.accessor;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BiomeColorHelper.class)
public interface AccessorBiomeColorHelper {
    @Invoker
    static int invokeGetColorAtPos(
        IBlockAccess blockAccess,
        BlockPos pos,
        BiomeColorHelper.ColorResolver colorResolver
    ) {throw new UnsupportedOperationException();}
}