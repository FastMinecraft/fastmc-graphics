package me.luna.fastmc.mixin.accessor;

import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeAccessType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeAccess.class)
public interface AccessorBiomeAccess {
    @Accessor
    BiomeAccessType getType();

    @Accessor
    long getSeed();
}
